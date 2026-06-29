# Deploy Canary TRIAL — FoodFlow v6 Etapa 18

Playbook de rollout controlado dos 4 microsserviços v6 (`tenant-service`,
`config-engine-service`, `pricing-engine-service`, `subscription-service`) com
**5 tenants TRIAL externos** — um por tipo de negócio (5 dos 6 templates do
design v6 §4).

## Objetivo

Validar em produção, com tráfego real e limitado:
- Onboarding self-service funciona end-to-end
- Os 6 templates de negócio cobrem casos reais sem ajustes
- Isolamento multi-tenant é mantido em carga
- Stripe MOCK comporta-se como o real (transição para PROD após canary OK)

## Pré-requisitos

| Item | Como verificar |
|------|----------------|
| Suite E2E verde | `mvn -pl pascoa-tenant-service test -Dtest=MultiTenantE2ETest` |
| 4 serviços v6 buildados | `mvn -DskipTests install` no root |
| Stack docker rodando | `docker compose ps` mostra postgres, postgres-platform, rabbitmq, redis, zipkin |
| Bootstrap legacy concluído | `curl :8094/tenants/pascoa-original` retorna 200 |
| Gateway com X-Tenant-Id ativo | `curl -H "Host: x.foodflow.com.br" :8090/actuator/health` |

## Os 5 Tenants Canary

| Slug | Template | Email | Cor primária |
|------|----------|-------|--------------|
| `confeitaria-canary-1` | CONFEITARIA | canary1@confeitaria.test | `#e91e63` (rosa) |
| `marmitaria-canary-1` | MARMITARIA | canary1@marmitaria.test | `#4caf50` (verde) |
| `restaurante-canary-1` | RESTAURANTE | canary1@restaurante.test | `#ff9800` (laranja) |
| `salgaderia-canary-1` | SALGADERIA | canary1@salgaderia.test | `#795548` (marrom) |
| `doces-canary-1` | DOCES | canary1@doces.test | `#9c27b0` (roxo) |

O template `CUSTOM` é provisionado sob demanda quando um tenant real solicitar.

## Rollout

### 1. Smoke test em staging

```bash
TENANT_SERVICE_URL=https://staging.foodflow.com.br \
  ./infra/canary/register-canary-tenants.sh --dry-run
# Confirma os 5 payloads sem efeitos colaterais
```

### 2. Provisionar canary

```bash
TENANT_SERVICE_URL=https://api.foodflow.com.br \
  ./infra/canary/register-canary-tenants.sh
```

Resultado esperado por tenant:
- HTTP `201` → todas as etapas OK
- HTTP `207` → tenant criado mas template/trial falharam (rastrear no log de cada serviço)

### 3. Validar pós-deploy

```bash
# Cada tenant deve estar TRIAL com expira_em em D+14
for s in confeitaria marmitaria restaurante salgaderia doces; do
  curl -s https://api.foodflow.com.br/tenants/$s-canary-1 | jq '.status, .dataExpiracao'
done

# Manifest PWA por tenant
curl https://api.foodflow.com.br/tenants/confeitaria-canary-1/manifest.json | jq

# Config efetiva alinhada ao template aplicado
curl https://api.foodflow.com.br/config/marmitaria-canary-1/efetivo | jq '.categorias, .modeloPreco'
# Esperado: 7 categorias marmitaria, modeloPreco=POR_UNIDADE

# Assinatura TRIAL no Stripe (mock ou real)
curl https://api.foodflow.com.br/subscriptions/restaurante-canary-1 | jq '.plano, .status, .stripeCustomerId'
```

### 4. Métricas para acompanhar (primeiras 72h)

| Métrica | Onde | Alvo |
|---------|------|------|
| Latência p95 `/onboarding/iniciar` | Actuator + Zipkin | < 2s |
| Taxa de erro 5xx no gateway | Logs/Prometheus | < 0.5% |
| Conflito de schema em provisionamento | `tenant-service.log` grep `CREATE SCHEMA` | zero erros |
| Falhas best-effort no onboarding | resposta HTTP 207 | < 5% dos requests |
| Uso de CPU dos 4 serviços v6 | `docker stats` ou Prometheus | cada um < 75% |
| Conexões Hikari (Pool 5) | `/actuator/metrics/hikaricp.connections.active` | < 4 simultâneas |

### 5. Critérios de promoção (canary → general availability)

✅ Promover quando, por 7 dias seguidos:
- Zero erros 5xx nos 4 serviços v6
- 5/5 tenants ativos sem suporte humano necessário
- Uso de cota dentro dos limites de plano (sem alertas de `MeteringService.verificarCota`)
- Custos de infraestrutura sob estimativa (Postgres < 500MB total, Redis < 80MB)

## Rollback

### Rollback parcial (suspende tenants)

```bash
./infra/canary/register-canary-tenants.sh --rollback
# POST /tenants/{id}/suspender em cada um — schema preservado, login bloqueado
```

### Rollback total (remove infraestrutura v6)

1. Suspender os 5 tenants (acima)
2. Reverter rotas v6 no `api-gateway`:
   ```yaml
   # Comentar bloco "v6 FoodFlow — rotas explícitas" em application.yml
   ```
3. Manter `pascoa-monolith` ativo — operação `pascoa-original` segue como sempre
4. Os bancos `foodflow_platform` permanecem (dados intactos para retomada)

### Anonimização LGPD (após decisão de descomissionar)

```bash
for s in confeitaria marmitaria restaurante salgaderia doces; do
  curl -X DELETE https://api.foodflow.com.br/tenants/$s-canary-1
done
# DROP SCHEMA CASCADE remove fisicamente os dados do tenant
```

## Comunicação com tenants canary

Email enviado aos 5 antes do D-Day:

> Boas-vindas ao programa canary do FoodFlow! Sua conta `<slug>` está pronta com
> trial de 14 dias. Acesse `https://<slug>.foodflow.com.br/onboarding.html` para
> personalizar.
>
> Esperamos seu feedback até [data] — reportar bugs em
> bugs@foodflow.com.br ou WhatsApp [número].

## Histórico de Execução

Registrar cada rollout neste arquivo:

| Data | Ambiente | Operador | Resultado | Notas |
|------|----------|----------|-----------|-------|
| _Aguardando primeiro deploy_ | — | — | — | — |
