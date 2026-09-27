# Sandbox AWS — pascoa-monolith

Infra mínima para disponibilizar o monólito em acesso trial. Terraform + Docker + ECR + SSM.
Nada dos 13 microsserviços v5 sobe aqui: o monólito não tem dependência de reactor, RabbitMQ,
Redis ou Eureka, então roda sozinho.

## Topologia

```
Internet ──HTTPS──> API Gateway (HTTP API, $default)
                         │  HTTP_PROXY + header X-Gateway-Secret
                         ▼
                    EIP :8080 ──> EC2 t3.small (Amazon Linux 2023)
                                    └─ docker compose
                                         ├─ app        (imagem do ECR)
                                         └─ postgres:16-alpine (volume no EBS)
```

Sem ALB (economiza ~US$ 18/mês), sem RDS, sem NAT, sem VPC própria — usa a default VPC.
Acesso administrativo por SSM Session Manager: **nenhuma porta 22 aberta, nenhuma chave SSH**.

## Custo estimado (us-east-1, 24/7)

| Item | US$/mês |
|---|---|
| EC2 t3.small | ~15,20 |
| EBS gp3 30 GB | ~2,40 |
| IPv4 público (EIP) | ~3,60 |
| API Gateway | ~1,00 por milhão de requisições |
| ECR (3 imagens ~250 MB) | ~0,08 |
| **Total** | **~21–23** |

Desligar a EC2 fora do horário de demonstração corta a maior parte. `99-destroy.sh` zera tudo.

## Pré-requisitos

- AWS CLI v2 autenticada (`aws sts get-caller-identity`), com o plugin Session Manager
- Terraform >= 1.6
- Docker em execução (build da imagem)
- JDK 21 + Maven

## Execução em fases

### Fase 1 — configurar

```bash
cp aws/terraform/terraform.tfvars.example aws/terraform/terraform.tfvars
# preencher: db_password, admin_senha_inicial, gateway_shared_secret
openssl rand -base64 24   # senhas
openssl rand -hex 32      # gateway_shared_secret
```

`terraform.tfvars` está no `.gitignore` — segredos não entram no repositório.

### Fase 2 — provisionar a infra

```bash
./aws/scripts/01-infra.sh
```

Cria ECR, IAM role (SSM + pull do ECR), security group, EIP, EC2 e API Gateway.
O `user_data` instala Docker, cria 2 GB de swap e escreve `/opt/pascoa/{docker-compose.yml,app.env,postgres.env}`.
**Não sobe a aplicação** — a imagem ainda não existe. Leva ~3 min até a instância aparecer no SSM.

### Fase 3 — buildar e publicar a imagem

```bash
./aws/scripts/02-build-push.sh
```

`mvn package` do monólito, `docker build --platform linux/amd64` (obrigatório em Mac ARM)
e push para o ECR com duas tags: o SHA curto do commit e `latest`.

### Fase 4 — deploy

```bash
./aws/scripts/03-deploy.sh
```

Via `aws ssm send-command`: login no ECR, `docker compose pull`, `up -d`. Depois aguarda
`/actuator/health/readiness` responder `UP` através do API Gateway e imprime a URL pública.
O grupo `readiness` inclui apenas `db,ping`: o indicador de mail (sem SMTP no sandbox) e o
`whatsapp` (UNKNOWN sem canal no banco) derrubavam o `/actuator/health` agregado.
O Flyway roda as 15 migrations no primeiro boot; o `DataInitializer` cria o usuário `admin`
com a senha de `admin_senha_inicial`.

Atalho para as fases 2–4: `./aws/deploy.sh`.

### Fase 5 — redeploy após mudança de código

```bash
./aws/scripts/02-build-push.sh && ./aws/scripts/03-deploy.sh
```

O compose referencia a tag `latest`, então não é preciso rodar o Terraform de novo.

### Destruir

```bash
./aws/scripts/99-destroy.sh
```

## Operação

```bash
aws ssm start-session --target "$(terraform -chdir=aws/terraform output -raw instance_id)"
# na instância:
sudo docker compose -f /opt/pascoa/docker-compose.yml logs -f app
sudo docker compose -f /opt/pascoa/docker-compose.yml restart app
sudo cat /var/log/pascoa-bootstrap.log     # diagnóstico do user_data
```

Backup do banco:
```bash
sudo docker compose -f /opt/pascoa/docker-compose.yml exec -T postgres \
  pg_dump -U postgres pascoa_monolith | gzip > /tmp/pascoa-$(date +%F).sql.gz
```

## Alterações feitas no monólito para viabilizar o deploy

| Arquivo | Motivo |
|---|---|
| `pascoa-monolith/pom.xml` | `spring-boot-maven-plugin` sem execução `repackage` — o parent é `pascoa-parent`, não `spring-boot-starter-parent`, então o jar saía sem dependências (652 KB em vez de 77 MB) e não era executável |
| `application.properties` | `app.base-url` e `app.upload.dir` passaram a aceitar `APP_BASE_URL` / `APP_UPLOAD_DIR` |
| `application-prod.properties` (novo) | desliga `show-sql`, devtools e cache-off do Thymeleaf; `cookie.secure=true`; nível de log |
| `SecurityConfig.java` | `/actuator/health` liberado para probes (só status, `show-details=when-authorized` mantido) |
| `DataInitializer.java` | senha inicial do admin via `app.admin.senha-inicial`; deixou de ser logada |
| `GatewaySecretFilter.java` (novo) | exige `X-Gateway-Secret` para que a EC2 exposta só aceite tráfego do API Gateway. Escreve o 403 direto na resposta: `sendError()` dispara dispatch ERROR para `/error`, que exige autenticação, e o bloqueio virava 302 para `/login` em loop |

## Limitações aceitas neste sandbox

- **Banco no mesmo host.** Terminar a instância apaga os dados. Trocar por RDS quando o
  ambiente deixar de ser descartável.
- **Uploads em disco local** (`/opt/pascoa/uploads`, bind mount no EBS). Não sobrevive à
  recriação da instância nem escala para duas instâncias — o caminho é S3.
- **Sessão HTTP em memória** do Tomcat e rate limit do bucket4j em memória: uma instância só.
  Escala horizontal exige Spring Session (Redis/JDBC) e sticky sessions.
- **Jobs `@Scheduled` sem lock distribuído** (o ShedLock foi removido). Correto com uma
  instância, duplicaria com duas.
- **Security group aberto na 8080.** O API Gateway HTTP API não tem faixa de IP fixa para
  restringir. Quem protege é o segredo compartilhado no header, não a rede — por isso
  `gateway_shared_secret` não deve ficar vazio.
- **Sem TLS entre API Gateway e EC2.** O tráfego externo é HTTPS; o trecho interno é HTTP
  na internet pública. Para eliminar: NLB interno + VPC Link, ou certificado na EC2.
- **State do Terraform local.** Um operador só. Backend S3 + DynamoDB se virar equipe.
- **Sem WAF, sem CloudWatch Logs, sem alarme, sem backup automático.**
