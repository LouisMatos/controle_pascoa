# Índice de Documentação — Sistema Controle Páscoa

> **Projeto:** Sistema de Gestão de Ovos de Páscoa Artesanal  
> **Stack:** Java 21 + Spring Boot 3.3.4 + Spring Cloud 2023.0.3 + PostgreSQL + Thymeleaf  
> **Arquitetura:** Monólito MVC + 9 Microsserviços (Strangler Fig) — design v5  
> **Repositório:** `/Users/luismatos/Documents/IA - CLAUDE/controle_pascoa`  
> **Atualizado em:** 2026-05-29

---

## Documentos Disponíveis

| # | Arquivo | Conteúdo | Quando usar |
|---|---------|----------|-------------|
| 00 | [00-indice.md](00-indice.md) | Este índice | Ponto de entrada |
| 01 | [01-infraestrutura.md](01-infraestrutura.md) | Stack, banco, Flyway, Docker Compose, RabbitMQ, Redis, Spring Cloud, deploy | Infraestrutura, deploy, configurações |
| 02 | [02-arquitetura-tecnica.md](02-arquitetura-tecnica.md) | Monólito MVC, arquitetura hexagonal dos microsserviços, padrões | Desenvolvimento, debugging, novos recursos |
| 03 | [03-fluxos-negocio.md](03-fluxos-negocio.md) | Fluxos de pedido, orçamento, produção, estoque, financeiro, notificações, CRM | Lógica de negócio, novas funcionalidades |
| 04 | [04-rotas-endpoints.md](04-rotas-endpoints.md) | Endpoints do monólito (~126) + APIs REST dos 9 microsserviços | Adicionar rotas, revisar permissões |
| 05 | [05-estado-implementacao.md](05-estado-implementacao.md) | O que está feito, parcial, pendente e bugs conhecidos | **Iniciar qualquer sessão de desenvolvimento** |
| 06 | [06-schema-banco.md](06-schema-banco.md) | 10 bancos de dados, 40+ tabelas, colunas, FKs e índices | Escrever queries, criar migrations, debugar JPA |
| 07 | [07-convencoes-desenvolvimento.md](07-convencoes-desenvolvimento.md) | Padrões hexagonal, naming, checklist de PR, estrutura de módulos | Qualquer tarefa de desenvolvimento novo |
| 08 | [08-manutencao-docs.md](08-manutencao-docs.md) | Protocolo de fim de sessão — quais docs atualizar após cada mudança | **Executar ao final de toda sessão com código alterado** |
| 09 | [09-quickstart.md](09-quickstart.md) | Subir todo o ambiente (infra + microsserviços + monólito) com `./start-all.sh` | Onboarding, primeira execução, demo |
| 10 | [10-bugfix-login-loop-gateway.md](10-bugfix-login-loop-gateway.md) | Bugfix B11 — loop de login após migração v5; configuração de `forward-headers-strategy` no monólito atrás do gateway | Referência ao corrigir/diagnosticar problemas de redirect via gateway |

---

## Mapa Rápido do Sistema

### Monólito (`pascoa-monolith` — porta 8080)
```
Módulos de Negócio (Spring MVC + Thymeleaf):
  cadastro/     → Cliente, Produto, Fornecedor, MateriaPrima
  pedido/       → Pedido, ItemPedido, Pagamento (máquina de estados)
  orcamento/    → Orcamento, OrcamentoItem, aprovação pública via token
  producao/     → OrdemProducao (Kanban / fila)
  qualidade/    → InspecaoQualidade (checklist JSONB)
  estoque/      → MovimentacaoEstoque (entrada/saída/ajuste)
  fichaTecnica/ → FichaTecnica + FichaTecnicaItem (composição do produto)
  financeiro/   → ContaReceber, ContaPagar, DespesaFixa, FluxoCaixa, Breakeven
  crm/          → NotaCliente, PontoFidelidade, segmentação
  notificacao/  → Templates por evento/canal + email + WhatsApp + SMS
  gastos/       → GastoVariavel, OrcamentoGasto
  analytics/    → Comparativo de safras, ranking de produtos
  catalogo/     → Catálogo público (sem login)
  pwa/          → Service Worker, manifest.json
  seguranca/    → Usuario, Role (RBAC)
```

### Microsserviços v5 (Arquitetura Hexagonal — Strangler Fig)
```
pascoa-api-gateway          (porta 8090) → Proxy + Circuit Breaker + Retry
pascoa-auth-service         (porta 8081) → JWT + TOTP + Redis blacklist
pascoa-config-server        (porta 8888) → Spring Cloud Config (Basic Auth)
pascoa-eureka               (porta 8761) → Service Discovery (Eureka Server)
pascoa-customer-service     (porta 8082) → Bounded context de clientes
pascoa-inventory-service    (porta 8083) → Estoque de matérias-primas + alertas críticos
pascoa-product-service      (porta 8084) → Catálogo de produtos
pascoa-order-service        (porta 8085) → Pedidos + OpenFeign + máquina de estados
pascoa-production-service   (porta 8086) → Ordens de produção (criadas via evento)
pascoa-financial-service    (porta 8087) → DRE, fluxo de caixa, lançamentos automáticos
pascoa-notification-service (porta 8088) → Email/WhatsApp/SMS com fallback automático
pascoa-analytics-service    (porta 8089) → Comparativo de safras, ranking, dashboard
```

### Infraestrutura (Docker Compose)
```
PostgreSQL   (porta 5432)  → 10 bancos (monolith + 9 microsserviços)
RabbitMQ     (porta 5672)  → 5 exchanges topic + DLX + DLQs
Redis        (porta 6379)  → JWT blacklist + rate limiting
Zipkin       (porta 9411)  → Distributed tracing
```

---

## Fluxo de Eventos RabbitMQ

```
order.confirmed ──► production-service  (cria OrdemProducao automaticamente)
                ──► inventory-service   (reserva insumos)
                ──► notification-service (notifica cliente)

order.delivered ──► financial-service   (lança RECEITA automaticamente)
                ──► analytics-service   (registra venda)
                ──► notification-service (notifica cliente)

production.completed ──► order-service  (avança pedido para PRONTO)
                     ──► financial-service (lança DESPESA de produção)

OpenFeign (síncrono):
  order-service ──► customer-service (valida cliente ao criar pedido)
  order-service ──► product-service  (valida produto + snapshot de preço)
```

---

## Enums Críticos

| Enum | Módulo | Valores |
|------|--------|---------|
| `StatusPedido` | monolith + order-service | NOVO → CONFIRMADO → EM_PRODUCAO → PRONTO → ENTREGUE / CANCELADO |
| `StatusOrdem` | monolith + production-service | PENDENTE → EM_ANDAMENTO → CONCLUIDA / CANCELADA |
| `StatusOrcamento` | monolith | PENDENTE → APROVADO / RECUSADO / EXPIRADO |
| `TipoMovimentacao` | inventory-service | ENTRADA, SAIDA |
| `TipoLancamento` | financial-service | RECEITA, DESPESA |
| `CategoriaLancamento` | financial-service | VENDA, INSUMO, PRODUCAO, OPERACIONAL, OUTRO |
| `Canal` | notification-service | EMAIL, WHATSAPP, SMS |
| `StatusNotificacao` | notification-service | PENDENTE, ENVIADA, FALHOU |
| `Role` | auth-service + monolith | ADMIN, FINANCEIRO, ATENDENTE, CONFEITEIRO, GESTOR_QUALIDADE, ANALISTA |

---

## Rotas Públicas (Sem Autenticação)

| Serviço | URL | Funcionalidade |
|---------|-----|---------------|
| Monolith | `/login`, `/logout` | Autenticação form-based |
| Monolith | `/acompanhamento/{token}` | Rastreamento de pedido |
| Monolith | `/orcamento-publico/{token}` | Aprovação de orçamento |
| Monolith | `/catalogo/**`, `/uploads/**` | Catálogo público e imagens |
| Monolith | `/manifest.json`, `/sw.js`, `/icons/**` | PWA |
| auth-service | `POST /api/auth/login` | Login JWT |
| auth-service | `POST /api/auth/refresh` | Renovar access token |
| order-service | `GET /api/acompanhamento/{token}` | Rastreamento via API REST |
| Todos | `GET /actuator/health` | Health check (sem auth) |

---

## Credenciais Padrão de Desenvolvimento

```
Monólito:      http://localhost:8080  —  admin / admin123
Gateway:       http://localhost:8090
Eureka UI:     http://localhost:8761
Config Server: http://localhost:8888  —  config-admin / config123
RabbitMQ UI:   http://localhost:15672 —  pascoa / pascoa123
Zipkin:        http://localhost:9411
Redis:         localhost:6379         —  senha: pascoa123
PostgreSQL:    localhost:5432         —  postgres / sua_senha
```

---

## Como Usar Esta Documentação

**Ao iniciar uma sessão de desenvolvimento**, leia nesta ordem:
1. **`00-indice.md`** — orientação geral (este arquivo)
2. **`05-estado-implementacao.md`** — o que está feito, pendente e bugs — **leia sempre**
3. **`02-arquitetura-tecnica.md`** — se a tarefa envolver código Java ou arquitetura
4. **`03-fluxos-negocio.md`** — se a tarefa envolver lógica de negócio
5. **`01-infraestrutura.md`** — se a tarefa envolver configuração/deploy/Docker
6. **`04-rotas-endpoints.md`** — se a tarefa envolver endpoints/permissões

**Ao encerrar uma sessão em que código foi alterado:**
7. **`08-manutencao-docs.md`** — executar o protocolo de atualização das docs
