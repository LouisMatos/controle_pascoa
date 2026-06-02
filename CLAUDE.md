# CLAUDE.md — Sistema Controle Páscoa

Sistema de gestão de ovos de Páscoa artesanal. Monolito Spring Boot MVC + Thymeleaf.

---

## Stack

- **Java 21** + **Spring Boot 3.3.4** + Maven
- **PostgreSQL** (`pascoa_db` em localhost:5432)
- **Flyway** para migrations (ddl-auto=`validate` — Hibernate nunca gera schema)
- **Thymeleaf 3** + Bootstrap 5.3.2 + Bootstrap Icons 1.11.3
- **Spring Security 6** com RBAC por roles
- **Lombok** (`@RequiredArgsConstructor` + `@Data` em todas as entidades/serviços)

Pacote base: `br.com.seuprojeto.pascoa`  
Entry point: `PascoaApplication.java`

---

## Documentação do Projeto

Leia antes de começar qualquer tarefa — evita re-exploração do código:

| Arquivo | Quando ler |
|---------|-----------|
| `docs/00-indice.md` | Sempre — mapa geral e orientação |
| `docs/05-estado-implementacao.md` | **Sempre ao iniciar desenvolvimento** — o que está feito, pendente e bugs |
| `docs/02-arquitetura-tecnica.md` | Tarefas de código Java (entidades, services, módulos) |
| `docs/03-fluxos-negocio.md` | Lógica de negócio, novos fluxos |
| `docs/01-infraestrutura.md` | Configurações, deploy, infraestrutura |
| `docs/04-rotas-endpoints.md` | Endpoints, permissões, novas rotas |
| `docs/06-schema-banco.md` | 29 tabelas com colunas, tipos, FKs e índices — migrations V1–V14; próxima: V15 |
| `docs/07-convencoes-desenvolvimento.md` | Padrões de entity/service/controller/template + naming + checklist de PR |
| `docs/08-manutencao-docs.md` | **Protocolo de fim de sessão** — quais docs atualizar após cada mudança de código |
| `docs/11-migracao-tenant-pascoa-original.md` | Etapa 16 v6 — estratégia "Tenant Zero": operação atual representada como tenant ENTERPRISE sem migração de dados |
| `docs/12-deploy-canary-trial.md` | Etapa 18 v6 — playbook de rollout dos 5 tenants canary, métricas de acompanhamento e rollback |
| `docs/13-acessibilidade-v7.md` | FASE 10 v7 — checklist WCAG 2.1 AA aplicado no Design System + guia de validação manual (axe, NVDA, Lighthouse, E2E) |

---

## ⚠️ Protocolo Obrigatório de Fim de Sessão

**Ao final de toda sessão em que código foi criado ou alterado**, executar:

1. `git diff --name-only HEAD` — listar arquivos modificados
2. Consultar `docs/08-manutencao-docs.md` — identificar quais docs precisam ser atualizadas
3. Atualizar `docs/05-estado-implementacao.md` — sempre (roadmap, bugs, gaps)
4. Atualizar os demais docs impactados conforme a tabela em `08-manutencao-docs.md`
5. Atualizar este `CLAUDE.md` se o roadmap mudou
6. **Atualizar `start-all.sh`** sempre que: (a) novo microsserviço Spring Boot for criado, (b) novo container Docker for adicionado, (c) porta de algum serviço mudar. Acrescentar à lista `V6_SERVICES` ou `MICROSERVICES` + `wait_port` da infra correspondente + linha no banner de URLs

> Se a sessão não produziu mudança de código (apenas consultas ou leituras), pular este protocolo.

---

## ⚠️ Política de Recursos (ambiente dev: MacBook Air M1, 8 GB RAM)

Hardware do dev é limitado — **todo novo serviço (container ou microsserviço Spring Boot) DEVE rodar com recursos mínimos** para que IDE + JVMs + docker caibam em 8 GB.

**Containers Docker (em `docker-compose.yml`):**
- Sempre definir `mem_limit`, `mem_reservation` e `cpus`.
- Preferir imagens `-alpine` / `-slim` e `platform: linux/arm64` (nativo M1).
- Tunar a aplicação para caber no limite (PG `shared_buffers`, Redis `maxmemory`, RabbitMQ `vm_memory_high_watermark`, etc.).
- Healthcheck com `interval` ≥ 15s.
- Orçamento total da stack docker ≈ 1.5 GB (ver tabela no fim do `docker-compose.yml`).

**Microsserviços Spring Boot (rodando via Maven/IntelliJ):**
- VM options padrão: `-Xms128m -Xmx256m -XX:MaxRAMPercentage=75 -XX:+UseSerialGC`.
- Em `application.properties`: `spring.datasource.hikari.maximum-pool-size=5`, `server.tomcat.threads.max=20`.
- Desabilitar devtools/livereload quando não usados.
- Em dev rotineiro, subir apenas os serviços necessários para a tarefa — não a stack toda.

**Stack mínima em dev:** `docker compose up postgres redis` (≈512 MB). Demais containers só quando trabalhando em microsserviços que dependem deles.

---

## Estrutura de Módulos

Cada módulo em `src/main/java/br/com/seuprojeto/pascoa/` segue:
```
{modulo}/controller/{Entidade}Controller.java
{modulo}/service/{Entidade}Service.java
{modulo}/repository/{Entidade}Repository.java
{modulo}/entity/{Entidade}.java
{modulo}/dto/{Entidade}Form.java  (input)
{modulo}/dto/{Entidade}Dto.java   (output)
```

Módulos existentes: `cadastro`, `pedido`, `orcamento`, `producao`, `qualidade`, `estoque`, `fichaTecnica`, `financeiro`, `crm`, `notificacao`, `gastos`, `analytics`, `catalogo`, `pwa`, `seguranca`

Templates em: `src/main/resources/templates/{modulo}/{arquivo}.html`

---

## Convenções Obrigatórias

**Código Java:**
- Injetar dependências via `@RequiredArgsConstructor` (Lombok) — nunca `@Autowired`
- Services são `@Transactional` por padrão
- Todas as entidades herdam `BaseEntity` (auditoria automática: criadoEm, atualizadoEm, criadoPor, atualizadoPor)
- Soft-delete via `@SQLDelete` + `@SQLRestriction` (nunca deletar fisicamente clientes/produtos)

**Banco de Dados:**
- **NUNCA** alterar `ddl-auto` — sempre criar migration Flyway em `src/main/resources/db/migration/`
- Nomenclatura: `V{N}__{descricao_snake_case}.sql`
- Próximo número de migration: verificar arquivos existentes em `db/migration/`
- Colunas NOT NULL novas precisam de DEFAULT na migration (lição aprendida)

**Templates Thymeleaf:**
- Sempre usar o fragment de layout: `th:replace="~{fragments/layout :: layout(~{::title}, ~{::main})}"`
- Permissões nos templates via `sec:authorize="hasRole('ROLE_ADMIN')"` etc.
- Formulários POST usam `th:action` + `th:object` + `th:field`

**Segurança:**
- Novas rotas precisam ser adicionadas em `SecurityConfig.java`
- Roles disponíveis: `ADMIN`, `FINANCEIRO`, `ATENDENTE`, `CONFEITEIRO`, `GESTOR_QUALIDADE`, `ANALISTA`
- **Spring Security 6 — persistência de `SecurityContext` em fluxos customizados:** NUNCA usar `session.setAttribute(SPRING_SECURITY_CONTEXT_KEY, ctx)` sozinho — o `SecurityContextPersistenceFilter` foi removido. Injetar o bean `SecurityContextRepository` (exposto em `SecurityConfig`) e chamar `securityContextRepository.saveContext(context, request, response)`. Detalhes em `docs/07-convencoes-desenvolvimento.md` §9.1 e `docs/10-bugfix-login-loop-gateway.md` §10.
- **Monólito atrás do `pascoa-api-gateway` (8090):** `server.forward-headers-strategy=framework` é obrigatório em `application.properties` para que redirects não escapem do gateway. Ver `docs/10-bugfix-login-loop-gateway.md`.

---

## Fluxos Críticos (resumo)

**Pedido:** NOVO → CONFIRMADO → EM_PRODUCAO → PRONTO → ENTREGUE (ou CANCELADO)  
Cada transição publica evento → `NotificacaoEventListener` → email/WhatsApp/SMS (fallback)

**Orçamento:** PENDENTE → APROVADO (via link público com token) → converte em Pedido

**Produção:** OrdemProducao criada automaticamente ao confirmar Pedido  
**Qualidade:** InspecaoQualidade com checklist JSONB — reprovado gera AlertaInterno

**Notificações:** Templates configuráveis por evento+canal no banco; `NotificacaoService` substitui `{variaveis}`.  
Jobs proativos: aniversário (08h), orçamento expirando (09h). SMS como fallback quando WhatsApp falha.

---

## Rotas Públicas (sem autenticação)

```
/login, /logout
/acompanhamento/{token}      → rastreamento de pedido pelo cliente
/orcamento-publico/{token}   → aprovação de orçamento pelo cliente
/catalogo/**                 → catálogo de produtos
/uploads/**                  → imagens de produtos
/manifest.json, /sw.js, /icons/**  → PWA
```

---

## Credenciais Dev

```
URL:   http://localhost:8080
Login: admin / Senha: admin123
BD:    localhost:5432/pascoa_db (usuário: postgres)
```

---

## Ordem de Implementação — v3/v4 (monólito)

1. ✅ Flyway baseline + migrations
2. ✅ Módulo Gastos Variáveis
3. ✅ Módulo Orçamentos
4. ✅ Catálogo Digital (upload de foto, QR Code)
5. ✅ CRM (segmentação, LTV, notas, pontos de fidelidade)
6. ✅ Controle de Qualidade (checklist, inspeção)
7. ✅ Analytics (comparativo de safras, ranking)
8. ✅ PWA (manifest.json, Service Worker)
9. ✅ Novas notificações: aniversário, orçamento expirando, SMS fallback
10. ⏳ DRE simplificado no monólito (implementado no financial-service v5)
11. ✅ Roles: GESTOR_QUALIDADE, ANALISTA
12. ✅ Testes de integração

## Ordem de Implementação — v5 (microsserviços, design doc v5) ✅ COMPLETO

1. ✅ Infraestrutura base — Docker Compose: RabbitMQ, Eureka, Config Server, Zipkin, Redis, PostgreSQL x10
2. ✅ api-gateway — Spring Cloud Gateway, proxy para monólito, porta 8090
3. ✅ auth-service — JWT + TOTP + Redis blacklist, porta 8081
4. ✅ config-server — Spring Cloud Config + Basic Auth, porta 8888
5. ✅ customer-service — bounded context de clientes, porta 8082
6. ✅ inventory-service — estoque de matérias-primas, porta 8083
7. ✅ product-service — catálogo de produtos, porta 8084
8. ✅ order-service — pedidos + OpenFeign, porta 8085
9. ✅ production-service — ordens de produção event-driven, porta 8086
10. ✅ financial-service — DRE + lançamentos automáticos, porta 8087
11. ✅ notification-service — email/WhatsApp/SMS com fallback, porta 8088
12. ✅ analytics-service — comparativo de safras, ranking, dashboard, porta 8089

## Ordem de Implementação — v6 (FoodFlow SaaS multi-tenant, design doc v6)

1. ✅ TenantContext + TenantAwareDataSource em `pascoa-commons` (auto-config opcional via `foodflow.tenant.enabled=true`); 9 serviços v5 já dependem de commons
2. ✅ postgres-platform + schema `platform` (DB `foodflow_platform`, porta 5441)
3. ✅ tenant-service (registro, provisionamento, status — porta 8094)
4. ✅ Liquibase multi-schema (changelog per-tenant aplicado em CREATE SCHEMA)
5. ✅ config-engine-service (6 templates: CONFEITARIA/MARMITARIA/RESTAURANTE/SALGADERIA/DOCES/CUSTOM — porta 8091)
6. ✅ feature flags + integração com TenantContext (REST API no tenant-service + `FeatureFlags.isAtivo(chave)` em commons)
7. ✅ pricing-engine-service (POR_UNIDADE/POR_PESO/POR_PORCAO/POR_COMBO/FAIXA_VOLUME/FORMULA_CUSTOM SpEL/ASSINATURA — porta 8092)
8. ✅ Evoluir catalog-service (atributos dinâmicos + variantes + ficha nutricional + tenant_id + sazonalidade — V2 migration)
9. ✅ Evoluir production-service (fases configuráveis + histórico de transições + tenant_id — V2 migration)
10. ✅ Evoluir notification-service (templates white-label com fallback global → tenant + tenant_id em notificações — V2 migration)
11. ✅ subscription-service + Stripe (porta 8093, modo MOCK por default, planos+limites+metering+webhook+job trial)
12. ✅ API Gateway com TenantResolutionFilter (subdomínio → X-Tenant-Id) + rate limiting Redis por tenant + rotas v6
13. ✅ White-label (endpoints públicos `manifest.json`/`theme.css`/`white-label` no tenant-service + abstração `WhiteLabel.atual()` em commons para PDF/UI)
14. ✅ Onboarding wizard (`OnboardingService` orquestra tenant→template→trial→white-label; UI estática em `/onboarding.html`)
15. ✅ API pública + OpenAPI 3.0 (springdoc + Swagger UI + ApiKey SHA-256 hashed + tabela `platform.api_key` — gate via feature flag `API_PUBLICA`)
16. ✅ Bootstrap tenant `pascoa-original` no startup (CommandLineRunner idempotente, ENTERPRISE perpétuo, sem migração física — operação atual segue em `pascoa_monolith`)
17. ✅ Testes E2E multi-tenant (`MultiTenantE2ETest` com Testcontainers PG — 7 cenários: registro/isolamento, feature flags, white-label, api-key lifecycle, onboarding best-effort)
18. ✅ Deploy canary TRIAL — script `infra/canary/register-canary-tenants.sh` (5 tenants, 5 templates) + playbook `docs/12-deploy-canary-trial.md` (execução real fica para janela combinada)

## Ordem de Implementação — v7 (FoodFlow Interface, design doc v7) ✅ COMPLETO

Front-end completo do FoodFlow com Design System white-label. **Zero alteração em endpoints Java pré-existentes** — todas as 51 telas que usavam `~{fragments/layout :: layout}` continuam funcionando.

1. ✅ Fundação Design System — `tokens.css` (3 camadas: primitivos/semânticos/componentes), `reset.css`, `typography.css` (Inter via Google Fonts), agregador `foodflow.css` com `@import`
2. ✅ Layout base + sidebar/topbar/breadcrumb — grid `.ff-layout` 2-col com 3 breakpoints (drawer mobile / ícones tablet / completa desktop); preserva 7 grupos de menu via `sec:authorize`
3. ✅ Biblioteca de componentes — 5 CSS (buttons/forms/badges/cards/tables) + 4 fragments (alert/modal-confirm/empty-state/pagination); função JS `ffToast()` e `ffConfirm()`
4. ✅ Telas alta prioridade — Login, 2FA setup/verificar (fullscreen com `ff-auth`), Dashboard (KPIs), Lista e Detalhe Pedido
5. ✅ Wizard de Pedidos + Kanban — stepper horizontal CSS + Sortable.js drag-drop entre colunas
6. ✅ Telas de gestão — Produtos lista/form, Matérias-Primas, Financeiro dashboard, CRM, Analytics (Chart.js brand-aware via `getComputedStyle()`)
7. ✅ Painel Admin Tenant (`/admin/tenant/*`) — 5 abas (Identidade com Alpine.js preview live / Fases Sortable / Atributos tabela editável / Templates Notif com vars clicáveis / Plano stripe-ready) + `TenantColorValidator` (utilitário Java WCAG 2.1) + endpoint AJAX `/admin/tenant/identidade/contraste`
8. ✅ Onboarding Wizard (`/onboarding/etapa1-7` + conclusão, permitAll) — `layout-fullscreen.html` com stepper de progresso; 7 etapas (empresa, tipo de negócio com 6 templates visuais, catálogo pré-carregado, fases drag-drop, identidade live preview, notificações WhatsApp/email, equipe convites)
9. ✅ PWA + Catálogo Público — `layout-public.html` white-label, catálogo com filtros pills + CTA WhatsApp, rastreio `/acompanhamento/{token}` com stepper FF, mobile produção (`/producao/mobile` touch ≥48px), `manifest.json` rosa + 3 shortcuts, `sw.js` v7 com Stale-While-Revalidate para `/css/foodflow*`
10. ✅ Acessibilidade WCAG 2.1 AA — `a11y.css` (focus-visible universal, skip link, sr-only, touch targets 44px em `pointer: coarse`, suporte forced-colors / reduced-motion / print) + correção da cor default da sidebar para atender contraste AA (`--brand-primary-dark` #c2185b com texto branco = 5.16:1) + `aria-current="step"` nos steppers + documento `docs/13-acessibilidade-v7.md` com checklist completo e guia de validação manual (axe, NVDA, Lighthouse, E2E)

**Stack adotada v7:** CSS Custom Properties (sem SCSS) · Inter font · Alpine.js 3.14 (lazy) · Sortable.js 1.15 · Chart.js 4.4 · Bootstrap 5.3 (mantido p/ dropdowns/modais) · Bootstrap Icons 1.11.

**Artefatos novos:** 15 CSS modulares · 12 fragments · 4 controllers Java (`TenantAdminController`, `TenantColorValidator`, `OnboardingController`, `ProducaoMobileController`) · 15 templates novos (admin/tenant + onboarding + mobile) · 15 templates migrados (login, 2fa, dashboard, pedidos, produtos, materias-primas, financeiro, crm, analytics, catalogo, acompanhamento, wizard, kanban).

**Wire-up futuro:** os POST handlers do `TenantAdminController` e `OnboardingController` hoje stub (flash success sem persistir) — pronto para integrar via Feign com `tenant-service` / `config-engine-service` / `subscription-service` quando multi-tenant do monólito for ativado.

## Correção Emergencial — v9 (telas em branco, design doc v9 §8) ✅ COMPLETO (exceto 7.5 E2E opcional)

Auditoria página a página das 7 etapas do design v9 "Emergência Primeiro", **adaptada ao monólito** (que usa `th:replace="~{fragments/layout :: layout}"`, não `layout:decorate`, e é single-tenant com acesso direto ao banco). **Resultado: 0 alterações de código** — as causas de tela em branco do doc ou já estavam resolvidas (v7/v8) ou não existem na arquitetura monolítica (sem Feign/timeout, sem TenantContext null, sem JWT decode, sem Layout Dialect, sem AJAX JSON quebrado).

1. ✅ ETAPA 1 — Desbloqueio (SecurityConfig static permitAll, `GlobalModelAdvice`, `TenantInfo.defaultTenant`, `AppErrorController`, `error/{500,404,403}.html`) — artefatos novos a commitar
2. ✅ ETAPA 2 — Pedidos/wizard (2.1/2.6 prontos; 2.2–2.5 N/A monólito)
3. ✅ ETAPA 3 — Produção/Catálogo (3.2/3.4 prontos; 3.1/3.3/3.5 N/A)
4. ✅ ETAPA 4 — Financeiro/CRM (4.1/4.2/4.4 prontos; 4.3 server-side equivalente)
5. ✅ ETAPA 5 — Admin/Onboarding (5.4 v8; 5.1/5.3 stubs seguros; 5.2/5.5 N/A)
6. ✅ ETAPA 6 — Catálogo Público/Rastreio (6.2/6.4 prontos; 6.1/6.3 single-tenant)
7. ✅ ETAPA 7 — Legados/Polimento (7.1 N/A; 7.2/7.3/7.4 verificados; **7.5 E2E Playwright pendente** — opcional, não-emergencial)

Detalhes página a página em `docs/05-estado-implementacao.md` → seção "Correção Emergencial v9".

## Multi-module Maven

O projeto é agora um **Maven multi-module** com 18 módulos:
- Root: `pom.xml` (packaging=pom, parent de todos)
- Monólito: `pascoa-monolith/`
- Microsserviços: `pascoa-{servico}/` (9 v5 + commons + eureka + config-server + api-gateway + v6: tenant-service + config-engine-service + pricing-engine-service + subscription-service)

**Atenção:** Ao rodar o monólito no IntelliJ, selecionar `pascoa-monolith` como módulo Maven.
