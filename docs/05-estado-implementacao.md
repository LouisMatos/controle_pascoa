# Estado de Implementação — Sistema Controle Páscoa

> **Verificado em:** 2026-05-29 — atualizado migração v5 (microsserviços, arquitetura hexagonal, Docker Compose)  
> **Critério:** ✅ Implementado e testado | ⚠️ Parcialmente implementado | ❌ Não iniciado | 🐛 Bug conhecido

---

## Resumo Executivo

### Monólito (pascoa-monolith)
| Área | Status |
|------|--------|
| Cadastros base | ✅ Completo |
| Pedidos + ciclo de vida | ✅ Completo |
| Orçamentos + aprovação pública | ✅ Completo |
| Produção (Kanban + fila) | ✅ Completo |
| Qualidade (inspeção + checklist) | ✅ Completo |
| Estoque | ⚠️ Template de saída ausente |
| Ficha Técnica | ✅ Completo |
| Financeiro (dashboard, fluxo, breakeven, aging) | ✅ Completo |
| Gastos integrados ao financeiro | ✅ Completo |
| DRE simplificado | ❌ Não iniciado (implementado no financial-service) |
| Simulador de cenários | ❌ Não iniciado |
| CRM | ✅ Completo |
| Notificações (email + WhatsApp + SMS) — 10 eventos | ✅ Completo |
| Notificações de aniversário / expiração / SMS | ✅ Item 25 — Completo |
| Analytics (safras, ranking) | ✅ Completo |
| Catálogo público | ✅ Completo |
| PWA | ✅ Completo |
| Segurança / RBAC | ✅ Completo |
| Testes de integração | ✅ 107 testes — 10 classes cobrindo todos os módulos críticos |

### Microsserviços v5 — Migração Strangler Fig (design doc v5)
| Serviço | Status | Porta | Checklist 11.1 |
|---------|--------|-------|----------------|
| Infraestrutura (Docker Compose) | ✅ Completo | — | RabbitMQ, Redis, Zipkin, PostgreSQL x10 |
| pascoa-eureka | ✅ Completo | 8761 | Eureka Server |
| pascoa-config-server | ✅ Completo | 8888 | Spring Cloud Config + Basic Auth |
| pascoa-api-gateway | ✅ Completo | 8090 | Spring Cloud Gateway + Circuit Breaker |
| pascoa-commons | ✅ Completo | — | DTOs e eventos compartilhados |
| pascoa-auth-service | ✅ Completo | 8081 | JWT + TOTP + Redis blacklist + testes |
| pascoa-customer-service | ✅ Completo | 8082 | Hexagonal completo + testes |
| pascoa-inventory-service | ✅ Completo | 8083 | Hexagonal completo + testes |
| pascoa-product-service | ✅ Completo | 8084 | Hexagonal completo + testes |
| pascoa-order-service | ✅ Completo | 8085 | Hexagonal + OpenFeign + testes |
| pascoa-production-service | ✅ Completo | 8086 | Hexagonal + event-driven + testes |
| pascoa-financial-service | ✅ Completo | 8087 | DRE + lançamentos automáticos + testes |
| pascoa-notification-service | ✅ Completo | 8088 | Email/WhatsApp/SMS + fallback + testes |
| pascoa-analytics-service | ✅ Completo | 8089 | Safras + ranking + dashboard + testes |
| Multi-module Maven (root pom) | ✅ Completo | — | 14 módulos, `-parameters` em todos |

---

## 1. Cadastros Base (`cadastro/`)

### ✅ Implementado
- CRUD completo: **Cliente**, **Produto**, **Fornecedor**, **MateriaPrima**
- Soft-delete em `Cliente` e `Produto` (`@SQLDelete` + `@SQLRestriction`)
- Upload de foto do produto (`/uploads/` + `WebMvcConfig`)
- Estoque crítico: `MateriaPrimaService.findComEstoqueCritico()` ativo
- Dashboard principal com KPIs (clientes, produtos, pedidos abertos, faturamento, meta)
- `PreferenciaCanal`: WHATSAPP, EMAIL, **AMBOS**, **NENHUM** (4 opções — mais que EMAIL/WHATSAPP)

### Observações
- Não há template de **lista de fichas técnicas** — acesso somente via detalhe do produto.

---

## 2. Pedidos (`pedido/`)

### ✅ Implementado
- CRUD completo + wizard de criação rápida
- Máquina de estados completa: NOVO → CONFIRMADO → EM_PRODUCAO → PRONTO → ENTREGUE / CANCELADO
- Adição e remoção dinâmica de itens
- Registro de pagamentos (PIX, DINHEIRO, CARTAO_CREDITO, CARTAO_DEBITO, FIADO)
- Rastreamento público via token (`/acompanhamento/{token}`)
- Exportação para Excel (`ExportService` + Apache POI)
- `precoUnitario` fixado no momento da criação do `ItemPedido`

---

## 3. Orçamentos (`orcamento/`)

### ✅ Implementado
- CRUD completo com cálculo de total automático
- Geração de PDF com OpenPDF (`OrcamentoPdfService`)
- Aprovação/recusa pública via token sem autenticação (`/orcamento-publico/{token}`)
- Conversão de orçamento aprovado em Pedido (1 clique)
- Máquina de estados: PENDENTE → APROVADO / RECUSADO / EXPIRADO
- Eventos publicados: `ORCAMENTO_APROVADO`, `ORCAMENTO_RECUSADO`

---

## 4. Produção (`producao/`)

### ✅ Implementado
- `OrdemProducao` criada automaticamente ao confirmar Pedido
- Visualização Kanban (`/producao/kanban`) e fila (`/producao/fila`)
- Transições: PENDENTE → EM_ANDAMENTO → CONCLUIDA
- Detalhe da ordem com dados do Pedido pai

---

## 5. Qualidade (`qualidade/`)

### ✅ Implementado
- `InspecaoQualidade` com checklist armazenado em coluna **JSONB** (PostgreSQL)
- `ChecklistItem` como entidade separada para itens padrão de checklist
- Aprovação/reprovação de inspeção
- Publicação de `InspecaoReprovadaEvent` → gera `AlertaInterno` para confeiteiro

---

## 6. Estoque (`estoque/`)

### ⚠️ Parcialmente Implementado

**Implementado:**
- Entrada de estoque (`/estoque/entrada`) com atualização de custo médio ponderado
- Ajuste de estoque (`/estoque/ajuste`)
- Histórico de movimentações (`/estoque/movimentacoes`)
- `EstoqueInsuficienteException` ao tentar saída sem saldo

**Gap identificado:**
- **Template `estoque/saida.html` ausente** — o arquivo não existe em `src/main/resources/templates/estoque/`. O `EstoqueController` pode ter o endpoint mapeado, mas a tela de saída manual não está acessível via UI. Saídas automáticas por produção podem funcionar via código, mas a tela para operador registrar saída manual está faltando.

---

## 7. Ficha Técnica (`fichaTecnica/`)

### ✅ Implementado
- CRUD de fichas técnicas com itens (ingredientes + quantidades)
- Relação 1:1 com Produto
- Cálculo de custo unitário via `CustoRealService` consumindo ficha técnica
- Usado por `FinanceiroService` para calcular margens por produto
- Template `fichas/detalhe.html` com exibição dos itens

---

## 8. Financeiro (`financeiro/`)

### ✅ Implementado — todos os 4 services principais

**`FinanceiroService`** — Dashboard financeiro:
- Faturamento (pedidos ENTREGUE), total recebido, pipeline em aberto
- Gastos variáveis do mês (`GastoVariavelRepository`)
- Despesas fixas mensais (`DespesaFixaRepository`)
- Top 5 produtos por volume/receita
- Margens por produto via Ficha Técnica

**`FluxoCaixaService`** — Fluxo de caixa:
- Recebido real: pagamentos no período
- Previsto entrada: contas a receber com vencimento no período
- Saída MP: entradas de estoque com custo
- Saída despesas fixas: proporcionais ao período (normalizado por 30.44 dias)
- **Saída gastos variáveis**: `GastoVariavelRepository.sumTotalByPeriodo()` ✅ integrado
- Saída prevista: contas a pagar com vencimento no período
- Campos: `saldoRealizado` + `saldoProjetado`

**`BreakevenService`** — Break-even + Projeção + Aging:
- Break-even: DespesasFixas + **GastosVariáveis do mês** ✅ integrado
- Custo médio variável via `custoRealCalculado` nos pedidos (fallback: 60% do preço)
- Projeção de safra com `aliquotaSimples` (`ConfiguracaoFinanceira`) para calcular impostos
- Aging: buckets corrente, 1-30, 31-60, 61-90, 90+ dias

**`CustoRealService`** — Custo por pedido:
- Calcula custo real via Ficha Técnica × quantidade do pedido

### ❌ Não Iniciado (item 10 do roadmap)
- **DRE simplificado** — Demonstrativo de Resultado do Exercício (receitas - custos - despesas = lucro líquido em formato formal)
- **Simulador de cenários** — "e se eu aumentar o preço X%? e se vender Y unidades a mais?"

---

## 9. Gastos (`gastos/`)

### ✅ Implementado
- CRUD de `GastoVariavel` por categoria e período
- `OrcamentoGasto`: meta por categoria e mês
- Dashboard "orçado vs realizado" por categoria
- Importação via CSV/Excel (`GastoVariavelService` com Apache POI)
- **Integração com financeiro confirmada**: `FluxoCaixaService` e `BreakevenService` consomem `GastoVariavelRepository`

---

## 10. CRM (`crm/`)

### ✅ Implementado
- Perfil do cliente com LTV, ticket médio, histórico de pedidos
- Notas do atendente (`NotaCliente`)
- Pontos de fidelidade (`PontoFidelidade`: GANHO/RESGATADO)
  - **F9 (Item 23):** `saldoPorCliente()` agora exclui CREDITOs com `data_expiracao` passada
- Segmentação (`SegmentoCliente`)
  - **F8 (Item 23):** campo `segmento` persistido na entidade `Cliente`; job `@Scheduled("0 0 2 * * *")` + ShedLock atualiza diariamente
- Dashboard de segmentação (`/crm/dashboard`)

---

## 11. Notificações (`notificacao/`)

### ✅ Implementado — 8 eventos ativos

**`EventoNotificacao` (enum) — eventos existentes:**
```
PEDIDO_CONFIRMADO, PRODUCAO_INICIADA, PEDIDO_PRONTO, PEDIDO_ENTREGUE,
PAGAMENTO_RECEBIDO, PEDIDO_CANCELADO, ORCAMENTO_APROVADO, ORCAMENTO_RECUSADO
```

**Infraestrutura:**
- `NotificacaoService`: multi-canal, verifica opt-in, interpola `{nome}`, `{numeroPedido}`, `{dataEntrega}`, `{link}`, `{valor}`
- `EmailService`: SMTP via `JavaMailSender` com suporte a testMode
- `WhatsAppService`: Evolution API com testMode, verificação de conexão, formatação de número (+55)
- Templates configuráveis por evento + canal no banco
- `NotificacaoEnviada`: registro de status (ENVIADA/FALHA) com `mensagemErro`
- `AlertaInterno`: gerado por `AlertaInternoListener` para eventos internos
- Configuração de canais: ativar/desativar EMAIL e WHATSAPP pelo admin

### ✅ Item 25 — Novas Notificações
- **ANIVERSARIO_CLIENTE** — job `@Scheduled("0 0 8 * * *")` + ShedLock; filtra aniversariantes via `findAniversariantesHoje()` (SQL nativo EXTRACT); idempotência por ano por canal
- **ORCAMENTO_EXPIRANDO** — job `@Scheduled("0 0 9 * * *")` + ShedLock; alerta 2 dias antes do vencimento de orçamentos PENDENTE; idempotência por índice único `uq_notif_orcamento_expirando`
- **Canal SMS** — `CanalNotificacao.SMS` + `SmsService` (webhook HTTP genérico, testMode por padrão); fallback automático quando WhatsApp falha
- Templates padrão inseridos pela migration V14 para ANIVERSARIO (email + whatsapp) e ORCAMENTO_EXPIRANDO (email + whatsapp)
- `NotificacaoEnviada` atualizada: `pedido_id` nullable + FKs `cliente_id` e `orcamento_id`
- `historico.html` atualizado: mostra evento, contexto (pedido/orçamento/aniversário), badge SMS

---

## 12. Analytics (`analytics/`)

### ✅ Implementado
- Comparativo entre as 2 últimas safras (anos com pedidos)
- `SafraDto`: faturamento total + quantidade + dados mensais (12 meses)
- `RankingProdutoDto`: top 15 produtos por quantidade/receita
- Anos disponíveis dinâmicos (`pedidoRepository.anosComPedidos()`)

---

## 13. Segurança (`seguranca/`)

### ✅ Implementado
- 6 roles: ADMIN, FINANCEIRO, ATENDENTE, CONFEITEIRO, GESTOR_QUALIDADE, ANALISTA
- `UsuarioService` implementa `UserDetailsService`
- CRUD de usuários (ADMIN only)
- BCrypt para senhas
- Usuário inativo bloqueado no login
- **Recuperação de senha** (Item 22): `AuthController` + `PasswordResetService` + token UUID 30min + email HTML
- Campo `email` opcional no `Usuario` para recuperação

---

## 14. Migrations Flyway

### ✅ Arquivos confirmados em `db/migration/`

| Arquivo | Status | O que faz |
|---------|--------|-----------|
| `V1__baseline.sql` | ✅ | Schema base |
| `V2__novas_tabelas_v3.sql` | ✅ | Tabelas v3 |
| `V3__crm_notas.sql` | ✅ | Notas CRM |
| `V4__alertas_internos.sql` | ✅ | Alertas internos |
| `V5__totp_admin.sql` | ✅ | 2FA TOTP |
| `V6__custo_snapshot_item_pedido.sql` | ✅ | Snapshot de custo |
| `V7__shedlock.sql` | ✅ | Tabela ShedLock |
| `V8__audit_log.sql` | ✅ | Auditoria |
| `V9__lgpd_campos.sql` | ✅ | LGPD |
| `V10__configuracao_sistema.sql` | ✅ | Config do sistema |
| `V11__password_reset_token.sql` | ✅ | Item 22: reset de senha + email em usuários |
| `V12__bugs_medios_item23.sql` | ✅ | Item 23: `evento` em notificacoes_enviadas, `desconsiderar_no_custo` + `pedido_id` em gastos_variaveis |
| `V13__cliente_segmento_campo.sql` | ✅ | Item 23 F8: campo `segmento` em clientes |

---

## 15. Testes

### ✅ 10 classes de teste — 107 testes (0 falhas)

| Classe | Testes | Cobre |
|--------|--------|-------|
| `PascoaApplicationTests` | 1 | Context load básico |
| `CustoRealServiceIntegrationTest` | ? | Custo real via Ficha Técnica |
| `FluxoCaixaGastosIntegrationTest` | ? | Fluxo de caixa com gastos variáveis |
| `NotificacaoEventListenerTest` | ? | Listener de eventos + envio |
| `AlertaInternoIntegrationTest` | ? | Criação e leitura de alertas |
| `OrcamentoServiceIntegrationTest` | 9 | CRUD orçamento + conversão em pedido (`@WithMockUser`) |
| `RolePermissionsTest` | 22 | RBAC (autorização por role) |
| `PedidoStateMachineTest` | 13 | Máquina de estados + F6 (cancelar desconsidere gastos) + B9 |
| `CrmSegmentoTest` | 8 | F8 (segmentação agendada) + F9 (saldo com expiração) |
| `NotificacaoIdempotenciaTest` | 5 | B7 — idempotência de notificações |
| `PasswordResetServiceTest` | 14 | Ciclo completo de recuperação de senha |

**Profile de teste:** H2 in-memory, `ddl-auto=create-drop`, Flyway desabilitado.  
**Infraestrutura:** `TestShedLockConfig` (no-op `LockProvider`) + `spring.main.allow-bean-definition-overriding=true`.  
**Padrão:** `em.flush(); em.clear()` após `criarComItens()` para evitar cache L1 do Hibernate com coleção vazia.

**Sem testes para:** EstoqueService, AnalyticsService.

---

## 16. Bugs e Problemas Conhecidos

### ✅ B13 — Após ativar 2FA, volta para `/login` em vez de `/dashboard` — RESOLVIDO 2026-05-30

**Problema:** Login OK → 2FA setup OK → digita TOTP → cai em `/login` em vez de `/dashboard`.

**Causa raiz:** `TwoFactorController.completarAutenticacao()` usava o padrão antigo do Spring Security 5 (`session.setAttribute(SPRING_SECURITY_CONTEXT_KEY, ctx)`). No Spring Security 6 o `SecurityContextPersistenceFilter` foi removido — a persistência precisa passar por `SecurityContextRepository.saveContext()`. Sem isso, o `RequestAttributeSecurityContextRepository` (que tem precedência no `DelegatingSecurityContextRepository`) retornava context vazio no próximo request e o usuário caía em `/login` como anônimo.

**Correção:**
- `SecurityConfig`: bean `SecurityContextRepository` explícito + amarrado ao `SecurityFilterChain` via `.securityContext(...)`.
- `TwoFactorController`: injeta o repositório e chama `securityContextRepository.saveContext(context, request, response)` em `completarAutenticacao()`.

**Detalhes completos:** [docs/10-bugfix-login-loop-gateway.md](10-bugfix-login-loop-gateway.md) seção 10.

### ✅ B12 — `./start-all.sh` travado em "Iniciando pascoa-config-server (porta 8888)..." — RESOLVIDO 2026-05-30

**Problema:** Script ficava 180s aguardando porta 8888 e abortava sem mensagem clara.

**Causa raiz:** `ConfigServerSmokeTest` importava `SecurityMockMvcRequestPostProcessors` mas o pom não declarava `spring-security-test`. O `spring-boot:run` (invocado sem `-Dmaven.test.skip=true`) disparava `test-compile`, que falhava → JAR nunca iniciava.

**Correção:**
- Adicionada dependência `spring-security-test` (test scope) em `pascoa-config-server/pom.xml`.
- `start-all.sh` agora invoca `spring-boot:run` com `-Dmaven.test.skip=true` e despeja `tail -20` do log no console em caso de timeout.

**Detalhes completos:** [docs/10-bugfix-login-loop-gateway.md](10-bugfix-login-loop-gateway.md) seção 9.

### ✅ B11 — Loop de login após migração v5 (gateway) — RESOLVIDO 2026-05-30

**Problema:** Acessando via `pascoa-api-gateway` (`localhost:8090`), o POST `/login` redirecionava para `localhost:8080/2fa/setup`, o navegador saía do gateway, perdia o `JSESSIONID` e voltava para `/login` em loop.

**Causa raiz:** `server.forward-headers-strategy` não configurado no monólito — Tomcat ignorava `X-Forwarded-Host/Port` enviados pelo Spring Cloud Gateway e usava `localhost:8080` em `sendRedirect`.

**Correção:**
- `pascoa-monolith/application.properties`: adicionado `server.forward-headers-strategy=framework` + `server.servlet.session.cookie.same-site=lax`
- `pascoa-config-server/configs/pascoa-monolith.yml`: mesmas configs replicadas
- `pascoa-api-gateway/application.yml`: `spring.cloud.gateway.x-forwarded.*` habilitado explicitamente

**Detalhes completos:** [docs/10-bugfix-login-loop-gateway.md](10-bugfix-login-loop-gateway.md)

### ⚠️ Gap: Template `estoque/saida.html` ausente

**Problema:** O arquivo `src/main/resources/templates/estoque/saida.html` não existe no projeto. A operação de saída manual de matéria-prima pode não ter tela acessível pela UI.

**Ação:** Criar o template seguindo o padrão de `estoque/entrada.html`.

---

## 17. Item 22 — Recuperação de Senha ✅

Implementado nesta sessão:
- Migration `V11__password_reset_token.sql`: tabela `password_reset_token` + coluna `email` em `usuarios`
- `PasswordResetToken` entity + `PasswordResetTokenRepository`
- `PasswordResetService`: gera token UUID (30 min), invalida token antigo, envia email HTML
- `AuthController` (`/auth/forgot-password`, `/auth/reset-password/{token}`): anti-enumeração
- Templates `auth/forgot-password.html` e `auth/reset-password.html` (standalone, sem layout)
- `reset-password.html` com barra de força de senha + show/hide + validação de confirm
- `Usuario.email` + `UsuarioForm.email` + `@InitBinder(StringTrimmerEditor)` para `@Email` opcional
- Link "Esqueceu sua senha?" na `login.html`
- Campo email no form de usuário

---

## 18. Item 23 — Bugs Médios e Fluxos ✅

Implementado nesta sessão:

| Bug | Arquivo(s) | Correção |
|-----|-----------|----------|
| **S9** — sw.js cacheava páginas autenticadas | `static/sw.js` | `networkFirst()`: removido `cache.put()` — agora nunca cacheia respostas HTML dinâmicas |
| **B7** — notificações duplicadas | `NotificacaoEnviada`, `NotificacaoEnviadaRepository`, `NotificacaoService`, `V12` | Campo `evento` adicionado; índice único parcial `(pedido_id, evento, canal) WHERE status='ENVIADA'`; check de idempotência em `processarCanal()` |
| **B8** — ficha técnica vazia retorna custo zero silenciosamente | `FichaTecnicaService` | `log.warn` quando `getItens().isEmpty()`; método `fichaTemItens()` para callers verificarem |
| **B9** — gastos de pedidos cancelados poluíam cálculos | `GastoVariavel`, `GastoVariavelRepository`, `V12` | Campo `desconsiderarNoCusto` + `pedidoId` FK; queries `sumTotal`, `sumPorCategoria`, `sumTotalByPeriodo` filtram `desconsiderarNoCusto = false` |
| **B10** — Periodicidade sem TRIMESTRAL/SEMESTRAL | `Periodicidade`, `DespesaFixaRepository` | Enum ampliado; CASE atualizado: TRIMESTRAL÷3, SEMESTRAL÷6 |
| **F6** — cancelar pedido não desmarcava gastos | `PedidoService`, `GastoVariavelRepository` | `cancelar()` chama `desconsiderarPorPedido(id)` |
| **F8** — segmento calculado apenas on-the-fly | `CrmService`, `Cliente`, `ClienteRepository`, `V13` | Campo `segmento` persistido em `clientes`; `@Scheduled("0 0 2 * * *")` + ShedLock `crm_recalcularSegmentos` |
| **F9** — pontos expirados contados no saldo | `PontoFidelidadeRepository` | `saldoPorCliente()`: CREDITO só conta se `data_expiracao IS NULL OR data_expiracao >= CURRENT_DATE` |

**B6** — `@DecimalMin("0.0001")` já presente em `EntradaEstoqueForm` — sem mudanças necessárias.  
**F7** — FluxoCaixa caixa vs competência: complexidade de UI elevada, adiado.

---

## 19. Roadmap — Itens Pendentes (design doc v4)

| # | Item | Status | Observação |
|---|------|--------|------------|
| 22 | Recuperação de Senha | ✅ Completo | Token UUID 30min, email HTML, anti-enumeração |
| 23 | Bugs Médios e Fluxos | ✅ Completo | S9, B7-B10, F6, F8, F9 (exceto F7) |
| 24 | Testes completos | ✅ Completo | 109 testes, 10 classes, BUILD SUCCESS |
| 25 | Novas Notificações | ✅ Completo | ANIVERSARIO_CLIENTE, ORCAMENTO_EXPIRANDO, canal SMS fallback |
| 10a | DRE simplificado | ❌ | Nova tela + `FinanceiroService` |
| 10b | Simulador de cenários financeiros | ❌ | Cálculos hipotéticos em `BreakevenService` |
| — | F7: FluxoCaixa caixa vs competência | ❌ | Adiado — requer toggle de UI complexo |
| — | `estoque/saida.html` | ❌ | Template ausente |

---

## 20. Item 24 — Testes Completos ✅

Implementado nesta sessão:

| Classe | Destaque |
|--------|----------|
| `PedidoStateMachineTest` | 13 testes: todas as transições válidas (NOVO→CONFIRMADO→PRONTO→ENTREGUE, NOVO→CANCELADO, CONFIRMADO→CANCELADO), transições inválidas lançam `IllegalStateException`, F6 (cancelar desconsidere gastos vinculados), B9 (sumTotal exclui `desconsiderarNoCusto=true`) |
| `CrmSegmentoTest` | 8 testes: F9 (créditos expirados ignorados, débitos deduzidos, expires-today conta, zero sem pontos), F8 (recalcularSegmentos persiste NOVO sem pedidos, dois clientes independentes) |
| `PasswordResetServiceTest` | 14 testes: solicitar por login/email/inexistente/inativo/sem-email; segunda solicitação apaga token anterior; expiração futura; validar válido/inexistente/expirado/usado; resetar atualiza senha e marca usado; duplo uso falha |
| `NotificacaoIdempotenciaTest` | 5 testes: campo `evento` persistido; existsBy detecta duplicata; evento diferente retorna false; FALHA não bloqueia retry; dois eventos/canais independentes |

**Fixes colaterais:**
- `OrcamentoServiceIntegrationTest`: adicionado `@WithMockUser(roles = "ADMIN")` em `converter_aprovado_*` e `converter_naoAprovado_*` — eliminada falha pré-existente com `@PreAuthorize`
- `application-test.properties`: `spring.flyway.enabled=false` + `spring.main.allow-bean-definition-overriding=true`
- `TestShedLockConfig`: no-op `LockProvider` com `@Primary` para evitar acesso à tabela `shedlock` ausente no H2

---

## 21. Migração v5 — Microsserviços (Strangler Fig) ✅

**Implementado em 2026-05-29** — todos os 12 itens do design doc v5 concluídos.

### Estrutura de módulos criada

```
controle_pascoa/
├── pom.xml                    ← root parent (packaging=pom, 14 módulos)
├── pascoa-monolith/           ← monólito original (295 arquivos)
├── pascoa-commons/            ← DomainEvent base compartilhado
├── pascoa-eureka/             ← @EnableEurekaServer, porta 8761
├── pascoa-config-server/      ← @EnableConfigServer + Basic Auth, porta 8888
│   └── configs/               ← 12 arquivos .yml (1 por serviço + application.yml)
├── pascoa-api-gateway/        ← Spring Cloud Gateway, porta 8090
│   ├── RequestTracingFilter   ← injeta X-Request-ID em todas as requisições
│   ├── ResponseTimeFilter     ← loga método + path + status + ms
│   └── FallbackController     ← 503 quando monólito cai
├── pascoa-auth-service/       ← porta 8081
│   ├── domain/                ← Usuario, Token, Role, JwtDomainService (sem Spring)
│   ├── application/           ← AuthUseCase + ports
│   ├── adapter/out/redis/     ← TokenBlacklistAdapter (Redis)
│   └── db/migration/V1__     ← tabelas usuarios + usuario_roles
├── pascoa-customer-service/   ← porta 8082, banco pascoa_customers
├── pascoa-inventory-service/  ← porta 8083, banco pascoa_inventory
├── pascoa-product-service/    ← porta 8084, banco pascoa_products
├── pascoa-order-service/      ← porta 8085, banco pascoa_orders
│   └── adapter/out/client/    ← ClienteFeignClient + ProdutoFeignClient
├── pascoa-production-service/ ← porta 8086, banco pascoa_production
├── pascoa-financial-service/  ← porta 8087, banco pascoa_financial
│   └── domain/                ← DreAnual, ResumoFinanceiro (cálculos puros)
├── pascoa-notification-service/ ← porta 8088, banco pascoa_notifications
│   └── domain/service/        ← TemplateEngine (substituição {variavel})
├── pascoa-analytics-service/  ← porta 8089, banco pascoa_analytics
│   └── domain/                ← MetricaSafra, RankingProduto, ComparativoSafra
└── infra/
    ├── postgres/init-databases.sql ← cria os 10 bancos de dados
    └── rabbitmq/definitions.json   ← 5 exchanges + filas + DLQs pré-configuradas
```

### Fluxo de eventos implementado

| Evento | Exchange | Publisher | Consumers |
|--------|----------|-----------|-----------|
| `order.confirmed` | `pascoa.orders` | order-service | production-service, inventory-service, notification-service |
| `order.delivered` | `pascoa.orders` | order-service | financial-service, analytics-service, notification-service |
| `order.cancelled` | `pascoa.orders` | order-service | notification-service |
| `production.completed` | `pascoa.production` | production-service | order-service (→PRONTO), financial-service |
| `inventory.stock.critical` | `pascoa.production` | inventory-service | (futuro: notification-service) |
| `auth.login.success/failed` | `pascoa.customers` | auth-service | (futuro: auditoria) |

### Fix crítico aplicado: `-parameters` flag

- **Causa:** Migração para `pascoa-parent` perdeu o flag `-parameters` do `spring-boot-starter-parent`
- **Sintoma:** `IllegalArgumentException: Name for argument of type [String] not specified` no login
- **Correção:** `maven-compiler-plugin <parameters>true</parameters>` no root pom + em cada módulo + `.idea/compiler.xml`
- **Impacto:** Afeta todos os 28 controllers com `@RequestParam`/`@PathVariable` sem `value` explícito

---

## 22. Próximas Sessões — Prioridade Sugerida

1. **Simulador de cenários financeiros** — "e se aumentar o preço X%? vender Y unidades a mais?" (monólito)
2. **`estoque/saida.html`** — template de saída manual de matéria-prima ausente (monólito)
3. **Integração Eureka** — habilitar `EUREKA_ENABLED=true` e testar service discovery entre microsserviços
4. **Dockerizar microsserviços** — criar Dockerfiles + adicionar serviços no docker-compose.yml
5. **customer-service com dados reais** — migrar dados de clientes do monólito para pascoa_customers
