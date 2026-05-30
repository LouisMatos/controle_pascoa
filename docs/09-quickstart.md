# Quickstart — Sistema Páscoa v5 em um comando

> **Objetivo:** subir todo o ambiente (infra Docker + 9 microsserviços + monólito + gateway + Eureka + Config Server) com um único comando, sem precisar abrir IntelliJ ou subir serviços manualmente.

---

## 1. Pré-requisitos

| Ferramenta | Versão mínima | Verificar |
|------------|---------------|-----------|
| Java       | 21 (LTS)      | `java -version` |
| Maven      | 3.9+          | `mvn -v` |
| Docker     | 24+ (com `compose` plugin) | `docker compose version` |
| Bash       | 4+            | `bash --version` |

> macOS: Docker Desktop com WSL/QEMU em modo Apple Silicon — os manifestos do `docker-compose.yml` já têm `platform: linux/arm64`.

---

## 2. Subir tudo em um comando

```bash
./start-all.sh
```

O script executa, em ordem:

1. **Verifica pré-requisitos** (Java 21, Maven, Docker).
2. **Sobe infra Docker** (`docker compose up -d`):
   - PostgreSQL 16 (porta `5432`) — cria 10 bancos via `infra/postgres/init-databases.sql`.
   - RabbitMQ 3.13 (`5672` + UI `15672`) — definitions pré-carregadas de `infra/rabbitmq/`.
   - Redis 7 (`6379`) — cache + blacklist de JWT.
   - Zipkin 3 (`9411`) — distributed tracing.
3. **Compila o projeto** (`mvn -DskipTests install`) — necessário para instalar `pascoa-commons` no `~/.m2`.
4. **Inicia os serviços Java em ordem topológica**, aguardando cada porta TCP responder antes do próximo passo:

   | Ordem | Serviço | Porta | Papel |
   |-------|---------|-------|-------|
   | 1 | `pascoa-config-server`     | 8888 | Config centralizado (basic auth: `config-admin`/`config123`) |
   | 2 | `pascoa-eureka`            | 8761 | Service Discovery |
   | 3 | `pascoa-auth-service`      | 8081 | JWT + TOTP |
   | 4 | `pascoa-customer-service`  | 8082 | Clientes |
   | 5 | `pascoa-inventory-service` | 8083 | Matérias-primas |
   | 6 | `pascoa-product-service`   | 8084 | Catálogo |
   | 7 | `pascoa-order-service`     | 8085 | Pedidos |
   | 8 | `pascoa-production-service`| 8086 | Ordens de produção (event-driven) |
   | 9 | `pascoa-financial-service` | 8087 | DRE + lançamentos |
   |10 | `pascoa-notification-service` | 8088 | Email/WhatsApp/SMS |
   |11 | `pascoa-analytics-service` | 8089 | Comparativo de safras |
   |12 | `pascoa-api-gateway`       | 8090 | Spring Cloud Gateway |
   |13 | `pascoa-monolith`          | 8080 | UI Thymeleaf + módulos legados |

Tempo estimado de boot completo (cache quente): **~3–5 min**.

---

## 3. Endpoints úteis

| Recurso | URL | Credenciais |
|---------|-----|-------------|
| Aplicação (UI) | http://localhost:8080 | `admin` / `admin123` |
| API Gateway    | http://localhost:8090 | — |
| Eureka         | http://localhost:8761 | — |
| Config Server  | http://localhost:8888 | `config-admin` / `config123` |
| RabbitMQ UI    | http://localhost:15672 | `pascoa` / `pascoa123` |
| Zipkin         | http://localhost:9411 | — |
| Postgres       | `localhost:5432`      | `postgres` / `sua_senha` |
| Redis          | `localhost:6379`      | senha: `pascoa123` |

---

## 4. Outros comandos

```bash
./start-all.sh status            # Mostra cada porta UP/DOWN + containers Docker
./start-all.sh logs pascoa-order-service   # tail -f no log do serviço
./start-all.sh stop              # Para serviços Java + docker compose down (volumes preservados)
```

Logs ficam em `logs/<modulo>.log`. PIDs ficam em `.pids/<modulo>.pid`.

---

## 5. Estrutura interna do script

- **Idempotência:** se um serviço já estiver rodando (PID válido), pula.
- **Healthcheck por porta TCP:** usa `/dev/tcp/host/port` do Bash — sem `curl` ou `nc`.
- **Timeout configurável:** 180s por serviço Spring Boot, 120s para containers de infra.
- **Volumes Docker preservados** no `stop` — para zerar dados:

  ```bash
  docker compose down -v
  ```

---

## 6. Troubleshooting

| Sintoma | Causa provável | Ação |
|---------|----------------|------|
| Porta já em uso ao subir um serviço | Processo antigo travado | `lsof -i :PORTA` → `kill -9 PID`; ou `./start-all.sh stop` |
| Serviço fica em "esperando porta..." e estoura timeout | Erro de boot | O script agora despeja `tail -20` do log automaticamente; também ver `logs/<modulo>.log` |
| `Iniciando pascoa-config-server (porta 8888)...` trava por 180s | Compilação de teste falha (ex.: `spring-security-test` ausente) | Bug B12 — `start-all.sh` agora invoca `spring-boot:run` com `-Dmaven.test.skip=true`; ver [docs/10-bugfix-login-loop-gateway.md](10-bugfix-login-loop-gateway.md#9-bug-b12) |
| Login via gateway (`:8090`) cai em loop para `/login` | `forward-headers-strategy` não configurado e/ou `SecurityContextRepository` não exposto como bean | Bugs B11/B13 — ver [docs/10-bugfix-login-loop-gateway.md](10-bugfix-login-loop-gateway.md) |
| Flyway falha por schema sujo | Banco com dados de outra versão | `docker compose down -v` (zera volumes) e rodar novamente |
| Eureka não registra serviços | Config Server caiu | `./start-all.sh logs pascoa-config-server` |
| Build Maven falha em `pascoa-commons` | Cache do `~/.m2` corrompido | `mvn clean install -DskipTests` na raiz |

---

## 7. Subir apenas o monólito (modo dev rápido)

Quando estiver desenvolvendo no monólito sem precisar dos microsserviços:

```bash
docker compose up -d postgres
cd pascoa-monolith && mvn spring-boot:run
```

UI continua em http://localhost:8080.

---

## 8. Referências cruzadas

- [docs/01-infraestrutura.md](01-infraestrutura.md) — detalhes de stack, bancos, portas.
- [docs/02-arquitetura-tecnica.md](02-arquitetura-tecnica.md) — desenho dos microsserviços e eventos.
- [docs/05-estado-implementacao.md](05-estado-implementacao.md) — o que está pronto x pendente.
