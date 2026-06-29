# Migração `pascoa-original` para Modelo Tenant — v6 Etapa 16

## Contexto

O **Controle Páscoa v5** é a operação atual em produção: monolito Spring Boot
single-tenant rodando em `pascoa_monolith` (PostgreSQL principal, porta 5432).

A v6 introduz o modelo SaaS multi-tenant. A Etapa 16 alinha a operação atual
ao novo modelo **sem migração física de dados** — o monolito continua usando
seu banco, mas passa a ser representado como um **tenant ENTERPRISE perpétuo**
no painel da plataforma.

## Estratégia "Tenant Zero"

```
┌─────────────────────────────────┐         ┌────────────────────────────────┐
│  foodflow_platform (porta 5441) │         │  pascoa_monolith (porta 5432)  │
│  ┌───────────────────────────┐  │         │  ┌──────────────────────────┐  │
│  │ platform.tenant           │  │         │  │ todas as tabelas do v5    │  │
│  │   id = 'pascoa-original'  │──┼─repr.───┼─→│ (pedidos, produtos, etc) │  │
│  │   plano = ENTERPRISE      │  │ aponta  │  │                          │  │
│  │   status = ATIVO          │  │ para    │  └──────────────────────────┘  │
│  └───────────────────────────┘  │         └────────────────────────────────┘
└─────────────────────────────────┘
```

O tenant **não tem schema dedicado** em `foodflow_platform` — o schema PostgreSQL
do monólito continua sendo o banco `pascoa_monolith` inteiro. A entrada na tabela
`platform.tenant` serve para:

1. Aparecer no painel admin com plano ENTERPRISE
2. Receber configuração de white-label (cores, nome, rodapé)
3. Ter feature flags configuráveis (overrides locais)
4. Reportar uso em `subscription.uso_metrico` para metering global

## Bootstrap Automático

`LegacyTenantBootstrap` (CommandLineRunner em `pascoa-tenant-service`) registra
o tenant no startup se ele ainda não existir:

```yaml
foodflow:
  bootstrap-legacy: true   # default; setar false para opt-out
```

**Idempotente:** se `pascoa-original` já existe, nada é feito. Reinícios do
serviço não duplicam nem sobrescrevem dados.

**Configuração registrada:**

| Campo | Valor |
|-------|-------|
| `id` | `pascoa-original` |
| `razao_social` | Controle Páscoa Original |
| `email` | admin@pascoa-original.local |
| `tipo_negocio` | DOCES (herança Páscoa) |
| `plano` | ENTERPRISE |
| `status` | ATIVO |
| `data_expiracao` | 9999-12-31 (perpétuo) |
| `nome_app` | Controle Páscoa |
| `cor_primaria` | `#e91e63` (rosa Páscoa) |
| `cor_secundaria` | `#fce4ec` |
| `rodape` | Sistema legacy — pascoa_monolith |

## Ativação Multi-Tenant no Monólito (Futuro)

Quando o monolito for ativado em modo multi-tenant (`foodflow.tenant.enabled=true`
em `application.properties`), requests sem subdomínio reconhecível pelo gateway
(localhost, IP, domínio custom não mapeado) devem cair no fallback
`pascoa-original` via header explícito:

```bash
curl -H 'X-Tenant-Id: pascoa-original' http://localhost:8080/pedidos
```

Isso é coerente com a regra atual do `TenantResolutionFilter`: localhost não
injeta header → modo legacy v5 → monólito segue como hoje.

## O Que NÃO É Feito Nesta Etapa

- **Não há migração de dados** entre `pascoa_monolith` e `foodflow_platform`
- **Não há criação de schema** `pascoa-original` em `foodflow_platform`
- **Não há mudança em `application.properties` do monólito** — permanece single-tenant
- **Não há reescrita de queries** existentes

A migração efetiva (esquema dedicado em `foodflow_platform` com dados copiados)
fica para uma etapa futura quando houver decisão de negócio para isso.

## Validação

```bash
# Subir tenant-service
docker compose up -d postgres-platform
# (rodar tenant-service no IntelliJ)

# Verificar tenant registrado
curl http://localhost:8094/tenants/pascoa-original | jq

# Esperado:
# {
#   "id": "pascoa-original",
#   "razaoSocial": "Controle Páscoa Original",
#   "plano": "ENTERPRISE",
#   "status": "ATIVO",
#   "tipoNegocio": "DOCES",
#   "dataExpiracao": "9999-12-31"
# }

# White-label
curl http://localhost:8094/tenants/pascoa-original/white-label | jq
# Esperado: cor_primaria #e91e63, nome_app "Controle Páscoa"

# Manifest PWA (Etapa 13)
curl http://localhost:8094/tenants/pascoa-original/manifest.json | jq
# Esperado: name "Controle Páscoa", theme_color #e91e63
```

## Reversão (se necessário)

Para remover o tenant legacy:

```sql
-- Conectar ao foodflow_platform
DELETE FROM platform.tenant WHERE id = 'pascoa-original';
-- CASCADE limpa white_label_config, feature_flag_tenant, api_key
```

E desabilitar o bootstrap:

```yaml
foodflow:
  bootstrap-legacy: false
```
