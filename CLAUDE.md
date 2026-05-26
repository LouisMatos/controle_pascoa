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
| `docs/06-schema-banco.md` | 25 tabelas com colunas, tipos, FKs e índices — migrations e queries |
| `docs/07-convencoes-desenvolvimento.md` | Padrões de entity/service/controller/template + naming + checklist de PR |

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

---

## Fluxos Críticos (resumo)

**Pedido:** NOVO → CONFIRMADO → EM_PRODUCAO → PRONTO → ENTREGUE (ou CANCELADO)  
Cada transição publica evento → `NotificacaoEventListener` → email/WhatsApp

**Orçamento:** PENDENTE → APROVADO (via link público com token) → converte em Pedido

**Produção:** OrdemProducao criada automaticamente ao confirmar Pedido  
**Qualidade:** InspecaoQualidade com checklist JSONB — reprovado gera AlertaInterno

**Notificações:** Templates configuráveis por evento+canal no banco; `NotificacaoService` substitui `{{variaveis}}`

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

## Ordem de Implementação Pendente (item 11 do design doc v3)

1. ✅ Flyway baseline + migrations
2. ✅ Módulo Gastos Variáveis
3. ✅ Módulo Orçamentos
4. ✅ Catálogo Digital (upload de foto, QR Code)
5. ✅ CRM (segmentação, LTV, notas, pontos de fidelidade)
6. ✅ Controle de Qualidade (checklist, inspeção)
7. ✅ Analytics (comparativo de safras, ranking)
8. ✅ PWA (manifest.json, Service Worker)
9. ⏳ Novas notificações: aniversário, orçamento expirando, SMS fallback
10. ⏳ Integrar gastos ao financeiro: DRE simplificado, simulador de cenários
11. ✅ Roles: GESTOR_QUALIDADE, ANALISTA
12. ✅ Testes de integração
