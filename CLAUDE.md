# CLAUDE.md

## Modo obrigatório de sessão

Ao iniciar sessão nova ou tarefa nova, antes de qualquer outra ação, ativar:

```
/caveman ultra
/ponytail ultra
```

Ambos ficam ativos em toda resposta até o fim da sessão.

Gestão de ovos de Páscoa artesanal. Maven multi-module: monólito Spring Boot MVC + Thymeleaf (`pascoa-monolith`) + 13 módulos v5 (microsserviços, Strangler Fig). Java 21, Spring Boot 3.3.4, PostgreSQL, Flyway, Spring Security 6, Lombok. Pacote base: `br.com.seuprojeto.pascoa`.

## Saída e estilo de trabalho

- Não imprimir código no terminal. Editar arquivo e citar `caminho:linha`.
- Código sem comentários, exceto regra de negócio não óbvia.
- Sem resumo longo, sem tabela decorativa, sem repetir o que o diff já mostra. Resposta final: o que mudou + arquivos + próximo passo pendente.
- Ler só os arquivos necessários à tarefa. Antes de explorar código, consultar `docs/` (tabela abaixo).
- Não criar abstração, interface, config ou teste não pedido.

## Comandos

```bash
mvn -DskipTests install                         # build todos módulos
cd pascoa-monolith && mvn spring-boot:run       # dev (docker compose up -d postgres antes)
./start-all.sh [status|logs <svc>|stop]         # ambiente completo
mvn test -Dsurefire.excludes="**/*IT.java,**/*IntegrationTest.java,**/*IT.class,**/*IntegrationTest.class"
mvn test -Dtest=PedidoStateMachineTest#cancelar_deveLiberarEstoque -pl pascoa-monolith
mvn checkstyle:check -Pci -Dcheckstyle.failsOnError=false
mvn spotbugs:check -Pci -DfailOnError=false
```

Checkstyle `.github/checkstyle.xml`, SpotBugs `.github/spotbugs-exclude.xml` (só no profile `ci`). JaCoCo roda em `mvn test`. Infra/portas/troubleshooting: `docs/09-quickstart.md`.

GitFlow: `feat|fix|refactor|chore/*` de `develop`; `release/*` de `develop`; `hotfix/*` de `main`. Ver `docs/09-gitflow.md`.

Dev: http://localhost:8080 · admin/admin123 · localhost:5432/pascoa_monolith (postgres). Gateway: 8090.

## Arquitetura

**Monólito**: Controller → Service → Repository → PostgreSQL. Eventos via `ApplicationEventPublisher`. Módulos em `pascoa-monolith/src/main/java/br/com/seuprojeto/pascoa/{modulo}/{controller,service,repository,entity,dto}`, templates em `resources/templates/{modulo}/`. Módulos: cadastro, pedido, orcamento, producao, qualidade, estoque, fichaTecnica, financeiro, crm, notificacao, gastos, analytics, catalogo, pwa, seguranca.

**Microsserviços v5** (hexagonal): `adapter/in → application/usecase → domain/model → adapter/out`. `domain/` nunca importa framework; usecase só conhece `port/in`/`port/out`. JPA entity separada do domain model (MapStruct). Comunicação RabbitMQ topic, assíncrona, idempotente por `eventId`. Cada serviço valida o próprio JWT. Serviços 8081–8090. Checklist de novo serviço: `docs/02-arquitetura-tecnica.md` §14.

Features novas nascem no microsserviço correspondente; monólito ainda concentra o domínio.

## Convenções

- `@RequiredArgsConstructor`, nunca `@Autowired`. Services `@Transactional`. Entidades herdam `BaseEntity`. Soft-delete `@SQLDelete` + `@SQLRestriction`.
- Banco: nunca mexer em `ddl-auto`. Migration Flyway `V{N}__{descricao_snake_case}.sql` em `src/main/resources/db/migration/` (próxima: V17). Coluna NOT NULL nova exige DEFAULT.
- Thymeleaf: `th:replace="~{fragments/layout :: layout(~{::title}, ~{::main})}"`; permissão via `sec:authorize`; forms com `th:action`/`th:object`/`th:field`.
- Multi-tenant: entidade nova herda `TenantEntity` (ou `BaseEntity`) e a tabela ganha `loja_id BIGINT NOT NULL DEFAULT 1 REFERENCES lojas(id)`. `nativeQuery` exige `AND loja_id = " + TenantContext.LOJA_ATUAL_SPEL`. Job `@Scheduled` usa `TenantJobRunner.porLoja`. Thread própria: `TenantContext.executar(lojaId, ...)`. `TenantAwareRepository` (base repo) corrige `findById` que o Hibernate 6.5 não filtra por `@TenantId`.
- Segurança: rota nova entra em `SecurityConfig.java`. Roles: ADMIN, FINANCEIRO, ATENDENTE, CONFEITEIRO, GESTOR_QUALIDADE, ANALISTA.
- Spring Security 6: nunca `session.setAttribute(SPRING_SECURITY_CONTEXT_KEY, ctx)` — injetar `SecurityContextRepository` e chamar `saveContext(context, request, response)`. Ver `docs/10-bugfix-login-loop-gateway.md`.
- `server.forward-headers-strategy=framework` obrigatório no monólito atrás do gateway.
- Públicas (sem auth): `/login`, `/logout`, `/acompanhamento/{token}`, `/orcamento-publico/{token}`, `/catalogo/**`, `/uploads/**`, `/manifest.json`, `/sw.js`, `/icons/**`.

## Fluxos

Pedido: NOVO → CONFIRMADO → EM_PRODUCAO → PRONTO → ENTREGUE | CANCELADO — cada transição publica evento → `NotificacaoEventListener` (email/WhatsApp/SMS fallback). Confirmar pedido cria OrdemProducao.
Orçamento: PENDENTE → APROVADO (link público com token) → vira Pedido.
Qualidade: checklist JSONB; reprovado gera AlertaInterno.
Jobs: aniversário 08h, orçamento expirando 09h.

## Docs

| Arquivo | Quando |
|---|---|
| `docs/05-estado-implementacao.md` | sempre ao iniciar dev — feito/pendente/bugs |
| `docs/02-arquitetura-tecnica.md` | código Java |
| `docs/03-fluxos-negocio.md` | regra de negócio |
| `docs/04-rotas-endpoints.md` | rotas/permissões |
| `docs/06-schema-banco.md` | 29 tabelas, FKs, migrations V1–V14 |
| `docs/07-convencoes-desenvolvimento.md` | padrões + checklist de PR |
| `docs/01-infraestrutura.md` | deploy/infra |
| `docs/08-manutencao-docs.md` | protocolo de fim de sessão |

## Fim de sessão (só se houve mudança de código)

`git diff --name-only HEAD` → atualizar `docs/05-estado-implementacao.md` + docs impactadas conforme `docs/08-manutencao-docs.md`.

Roadmap v3/v4 e v5: completos, exceto DRE no monólito (existe no financial-service). Histórico em `docs/05-estado-implementacao.md`.
