# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Sistema Controle Páscoa

Sistema de gestão de ovos de Páscoa artesanal. Monolito Spring Boot MVC + Thymeleaf, evoluindo para arquitetura de microsserviços (v5).

---

## Comandos

Projeto é Maven multi-module (root `pom.xml`, packaging=pom). Rodar comandos a partir da raiz ou dentro do módulo específico.

```bash
# build completo (todos os módulos), sem testes
mvn -DskipTests install

# build de um módulo só (ex.: monólito)
cd pascoa-monolith && mvn install

# rodar monólito isolado (dev rápido — só precisa do Postgres)
docker compose up -d postgres
cd pascoa-monolith && mvn spring-boot:run

# subir ambiente completo (infra Docker + 13 serviços Java em ordem topológica)
./start-all.sh
./start-all.sh status                        # porta UP/DOWN de cada serviço
./start-all.sh logs pascoa-order-service     # tail -f do log
./start-all.sh stop                          # para tudo (volumes preservados)

# testes — todos os módulos, exclui *IT.java e *IntegrationTest.java
mvn test -Dsurefire.excludes="**/*IT.java,**/*IntegrationTest.java,**/*IT.class,**/*IntegrationTest.class"

# um teste único
mvn test -Dtest=PedidoStateMachineTest -pl pascoa-monolith
mvn test -Dtest=PedidoStateMachineTest#cancelar_deveLiberarEstoque -pl pascoa-monolith

# lint / análise estática — só ativos com o profile "ci"
mvn checkstyle:check -Pci -Dcheckstyle.failsOnError=false -Dcheckstyle.logViolationsToConsole=true
mvn spotbugs:check -Pci -DfailOnError=false
```

- Config do Checkstyle: `.github/checkstyle.xml` (severidade `error` bloqueia só com o profile `ci` ativo).
- Config do SpotBugs: `.github/spotbugs-exclude.xml`.
- Cobertura via JaCoCo roda automaticamente em `mvn test` (todos os módulos).
- Credenciais/portas dos serviços de infra (RabbitMQ, Redis, Eureka, Config Server) e troubleshooting: `docs/09-quickstart.md`.

**GitFlow:** branches `feat/*`, `fix/*`, `refactor/*`, `chore/*` a partir de `develop`; `release/*` a partir de `develop` (auto-PR para `main`); `hotfix/*` a partir de `main` (auto-PR para `main` e `develop`). CI varia por tipo de branch — ver `docs/09-gitflow.md`.

---

## Stack

- **Java 21** + **Spring Boot 3.3.4** + Maven
- **PostgreSQL** (`pascoa_monolith` em localhost:5432)
- **Flyway** para migrations (ddl-auto=`validate` — Hibernate nunca gera schema)
- **Thymeleaf 3** + Bootstrap 5.3.2 + Bootstrap Icons 1.11.3
- **Spring Security 6** com RBAC por roles
- **Lombok** (`@RequiredArgsConstructor` + `@Data` em todas as entidades/serviços)

Pacote base: `br.com.seuprojeto.pascoa`  
Entry point: `PascoaApplication.java`

---

## Arquitetura — Strangler Fig (2 camadas coexistindo)

**Monólito (`pascoa-monolith`)** — MVC em camadas: `Controller → Service → Repository → PostgreSQL`. Eventos internos via `ApplicationEventPublisher` do Spring.

**Microsserviços v5 (os outros 13 módulos)** — Arquitetura Hexagonal (Ports & Adapters):
```
adapter/in (REST, RabbitMQ Consumer) → application/usecase → domain/model → adapter/out (JPA, RabbitMQ)
```
- **Regra de ouro:** `domain/` nunca importa Spring/JPA/framework nenhum; `application/usecase` só conhece `port/in` e `port/out`, nunca HTTP/JPA/RabbitMQ diretamente.
- JPA Entity é **separada** do domain model (mapeada via MapStruct).
- Comunicação entre serviços: RabbitMQ (topic exchanges), assíncrona, com idempotência por `eventId`.
- Cada serviço é stateless e valida o próprio JWT (sem sessão compartilhada).
- Detalhes completos e checklist de novo serviço: `docs/02-arquitetura-tecnica.md` §1 e §14, `docs/07-convencoes-desenvolvimento.md` (seção "Microsserviços v5").

Migração é gradual: features novas tendem a nascer no microsserviço correspondente; o monólito ainda concentra a maior parte do domínio de negócio.

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

---

## ⚠️ Protocolo Obrigatório de Fim de Sessão

**Ao final de toda sessão em que código foi criado ou alterado**, executar:

1. `git diff --name-only HEAD` — listar arquivos modificados
2. Consultar `docs/08-manutencao-docs.md` — identificar quais docs precisam ser atualizadas
3. Atualizar `docs/05-estado-implementacao.md` — sempre (roadmap, bugs, gaps)
4. Atualizar os demais docs impactados conforme a tabela em `08-manutencao-docs.md`
5. Atualizar este `CLAUDE.md` se o roadmap mudou

> Se a sessão não produziu mudança de código (apenas consultas ou leituras), pular este protocolo.

---

## Estrutura de Módulos (monólito)

Cada módulo em `pascoa-monolith/src/main/java/br/com/seuprojeto/pascoa/` segue:
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
BD:    localhost:5432/pascoa_monolith (usuário: postgres)
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

## Multi-module Maven

O projeto é agora um **Maven multi-module** com 14 módulos:
- Root: `pom.xml` (packaging=pom, parent de todos)
- Monólito: `pascoa-monolith/`
- Microsserviços: `pascoa-{servico}/` (9 serviços + commons + eureka + config-server + api-gateway)

**Atenção:** Ao rodar o monólito no IntelliJ, selecionar `pascoa-monolith` como módulo Maven.
