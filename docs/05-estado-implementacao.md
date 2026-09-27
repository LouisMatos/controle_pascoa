# Estado de Implementação — Sistema Controle Páscoa

> **Verificado em:** 2026-09-22 — testes unitários passando (78/78), fixes JaCoCo/Mockito/Java 21  
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
| `V14__novas_notificacoes_item25.sql` | ✅ | Item 25: `cliente_id` + `orcamento_id` em notificacoes_enviadas, índices de idempotência |
| `V15__indices_performance_fluxo_caixa.sql` | ✅ | Performance: `idx_pagamento_data_pagamento` + `idx_movimentacao_estoque_tipo_data` para agregações no DB |

> **Próxima versão de migration disponível:** V16.

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

## 22. Otimizações de Performance (2026-06-30) ✅

Varredura geral de hotspots no monólito (via `/java-performance-analysis`). 5 problemas corrigidos:

| # | Tipo | Local | Antes | Depois |
|---|------|-------|-------|--------|
| 1 | JPA | `FluxoCaixaService.calcular()` (recebido real) | `pagamentoRepository.findAll().stream().filter(período)` | `pagamentoRepository.sumValorByPeriodo(inicio, fim)` — `SUM()` no DB |
| 2 | JPA | `FluxoCaixaService.calcular()` (saída MP) | `findByTipoOrderByDataDesc(ENTRADA).stream().filter(período)` | `sumCustoByTipoEPeriodo(tipo, inicioDt, fimExclusivo)` — `SUM()` no DB com range sargable |
| 3 | N+1 | `PedidoService.snapshotCustos()` | `buscarPorProduto()` + `save()` por item (2N round-trips) | `FichaTecnicaRepository.findByProdutoIdsComItens(produtoIds)` + `saveAll()` em batch |
| 4 | N+1 | `PedidoService.criarComItens()` | `produtoRepository.findById()` + `itemRepository.save()` por item | `findAllById()` + `saveAll()` em batch |
| 5 | JPA | `OrcamentoController` (4 ocorrências) | `clienteRepo.findAll()` (entidade completa) | `clienteRepo.findAllComboBox()` projetando `ClienteComboDto(id, nome)` já ordenado |

**Mudanças de suporte:**
- `V15__indices_performance_fluxo_caixa.sql`: `idx_pagamento_data_pagamento` + `idx_movimentacao_estoque_tipo_data` (composto).
- `application.properties`: `hibernate.jdbc.batch_size=30` + `order_inserts=true` + `order_updates=true` + `batch_versioned_data=true`.
- Novo DTO `cadastro/dto/ClienteComboDto` (record `id`, `nome`).

**Validação:**
- `mvn -pl pascoa-monolith clean compile` → `BUILD SUCCESS` (200 arquivos).
- Para validar ganho real: ativar `hibernate.generate_statistics=true` em dev e medir `/financeiro/fluxo-caixa` antes/depois.

---

## 21. Fixes Java 21 + Refactor Navbar ✅

**Data:** 2026-09-22 — Teste suite foi 78/78 (100% passing)

### Problemas Resolvidos

| Problema | Raiz | Solução |
|----------|------|---------|
| Testes falhavam com JaCoCo `Unsupported class file major version 70` | JaCoCo 0.8.12 não suporta Java 21 | Upgrade `pom.xml`: JaCoCo 0.8.12 → 0.8.14 |
| Mockito inline mocks falhavam com Byte Buddy/Java 21 | Mock-maker padrão incompatível | Maven Surefire: `<mockito.mock-maker>subclass</mockito.mock-maker>` |
| Byte Buddy rejeita Java 21 | Versão antiga Byte Buddy | Adicionar flag: `<net.bytebuddy.experimental>true</net.bytebuddy.experimental>` |
| Templates MockMvc falhavam ao resolver `#httpServletRequest` | Spring test context não fornecia `HttpServletRequest` no modelo | Refactor: criar `@ControllerAdvice` `LayoutAdvice` para injetar `activeGroup` no modelo |
| Navbar ativo usava expressão complexa em Thymeleaf | Acesso direto a `#httpServletRequest` via SpringEL | Simplificar: `${activeGroup == 'cadastros'}` em lugar de ternário com 4 condições |

### Arquivos Modificados

- `pom.xml`: JaCoCo 0.8.12 → 0.8.14
- `pascoa-monolith/pom.xml`: Surefire plugin com mock-maker + Byte Buddy flags
- `pascoa-monolith/src/main/java/br/com/seuprojeto/pascoa/config/LayoutAdvice.java` (novo): injeta `activeGroup` baseado em `request.getRequestURI()`
- `pascoa-monolith/src/main/resources/templates/fragments/layout.html`: Navbar refatorado com `${activeGroup}` (6 grupos: cadastros, comercial, producao, estoque, financeiro, admin)

### Testes — Resultado Final

```
[INFO] Tests run: 78, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

**Cobertura:** Todos os 78 testes unitários passam, incluindo:
- 22 testes `RolePermissionsTest` (7 roles × navegação navbar)
- 14 testes `NotificacaoEventListenerTest` (eventos + exception handling)
- 13 testes `PedidoStateMachineTest` (transições de estado)
- 8 testes `CrmSegmentoTest` (segmentação)
- 14 testes `PasswordResetServiceTest` (recuperação de senha)
- 6 testes `NotificacaoIdempotenciaTest` (idempotência)
- Demais testes de autenticação, autorização, controllers, services

### Impacto

✅ **Nenhum bug novo introduzido** — refactor é estrutural, não muda lógica de negócio.  
✅ **Testes de integração confirmam** — RolePermissionsTest renderiza 15 templates com sucesso.  
✅ **Pronto para CI/CD** — Maven agora rodar testes com `mvn test -pl pascoa-monolith` sem falhas.

---

## 23. Sandbox AWS (2026-09-25)

Infra mínima para acesso trial do monólito, em `aws/`. Ver `aws/README.md` para as fases de
execução, custo estimado (~US$ 22/mês) e limitações aceitas.

**Topologia:** API Gateway (HTTP API, `$default`) → HTTP_PROXY → EIP:8080 → EC2 t3.small
(Amazon Linux 2023) rodando `docker compose` com app + `postgres:16-alpine`. Sem ALB, sem RDS,
sem VPC própria (usa a default). Acesso administrativo por SSM Session Manager, sem porta 22.

| Arquivo | Conteúdo |
|---|---|
| `aws/terraform/` | ECR, IAM (SSM + pull ECR), SG, EIP, EC2, API Gateway, templates de `user_data`/compose |
| `aws/Dockerfile` | `eclipse-temurin:21-jre-alpine`, uid 10001, healthcheck em `/actuator/health` |
| `aws/scripts/01..03`, `aws/deploy.sh` | provisionar → build+push ECR → deploy via `aws ssm send-command` |
| `aws/scripts/99-destroy.sh` | `terraform destroy` |

**Mudanças no monólito para viabilizar o deploy:**

- 🐛 `pascoa-monolith/pom.xml` — `spring-boot-maven-plugin` não tinha execução `repackage`
  (o parent é `pascoa-parent`, não `spring-boot-starter-parent`). O jar saía com 652 KB, sem
  dependências e não executável; agora 77 MB. Afetava também o artifact publicado pelo CI.
- `application-prod.properties` (novo) — perfil `prod`: `show-sql=false`, `thymeleaf.cache=true`,
  devtools desligado, `session.cookie.secure=true`, `timeout=30m`, níveis de log, e grupo de
  health `readiness` (`db,ping`) com `management.health.mail.enabled=false` — o indicador de
  mail sem SMTP e o `whatsapp` em UNKNOWN deixavam `/actuator/health` em DOWN/503.
- `application.properties` — `app.base-url` e `app.upload.dir` passaram a ler `APP_BASE_URL` /
  `APP_UPLOAD_DIR`.
- `SecurityConfig.java` — `/actuator/health` liberado a anônimo (só o status; `show-details`
  segue `when-authorized`). Antes `/actuator/**` exigia ROLE_ADMIN e nenhuma probe funcionava.
- `DataInitializer.java` — senha inicial do admin via `app.admin.senha-inicial`
  (`ADMIN_SENHA_INICIAL`), e deixou de ser escrita no log.
- `GatewaySecretFilter.java` (novo) + `GatewaySecretFilterTest` (4 testes) — exige o header
  `X-Gateway-Secret` injetado pelo API Gateway. Necessário porque o HTTP API não tem faixa de
  IP fixa, logo o SG fica aberto na 8080 e a rede sozinha não protege a instância. O 403 é
  escrito direto na resposta: com `sendError()` o dispatch ERROR cai em `/error`, que exige
  autenticação, e a recusa virava 302 para `/login` em loop.

**Validado em ambiente real (2026-09-25):** conta 896328389222, us-east-1. `readiness` UP,
login `admin` via API Gateway redirecionando para `/dashboard` (forward-headers correto),
`/pedidos`, `/orcamentos`, `/producao`, `/crm`, `/analytics`, `/usuarios`, `/auditoria`,
`/gastos`, `/qualidade`, `/estoque/movimentacoes`, `/financeiro/dashboard`,
`/financeiro/fluxo-caixa` em 200; `/catalogo`, `/manifest.json`, `/sw.js` públicos em 200;
acesso direto ao IP da EC2 em 403.

**Pendente / não coberto:** uploads em disco local (deveriam ir para S3), sessão e rate limit
em memória (uma instância só), jobs `@Scheduled` sem lock distribuído, sem TLS no trecho
API Gateway → EC2, state do Terraform local, sem WAF/CloudWatch/backup automático.

---

## 25. Evolução do Fluxo Operacional (2026-09-26)

Branch `feat/evolucao-fluxo-producao-pascoa`. Acompanhamento detalhado em `EVOLUCAO_FLUXO_PASCOA.md`.

| Mudança | Onde |
|---|---|
| Pagamento recusa lançamento duplicado, pedido quitado e valor acima do saldo | `PedidoService.registrarPagamento`, `PagamentoRepository.existsByPedidoIdAndValorAndTipoPagamentoAndDataPagamento` |
| Status do pedido derivado da produção: ordem EM_ANDAMENTO → pedido EM_PRODUCAO; sem ordem aberta e alguma concluída → PRONTO | `ProducaoAtualizadaEvent`, `ProducaoService`, `PedidoService.sincronizarComProducao` |
| "A receber" derivado de `Pedido` − `Pagamento` (aging e previsto de entrada do fluxo de caixa passaram a ter dados; `ContaReceber` deixou de ser lida) | `PedidoRepository.saldosEmAberto`, `sumSaldoEmAbertoPorVencimento`, `BreakevenService.aging`, `FluxoCaixaService` |
| Cancelamento de pedido com valor recebido gera alerta interno de devolução | `PedidoService.cancelar` |
| Custo real usa o snapshot `ItemPedido.custoUnitario` (ficha técnica só como fallback) | `CustoRealService` |
| Detalhe do pedido deixou de gravar no banco durante GET; `salvarSemRecalculo` removido | `PedidoController.detalhe`, `PedidoService` |
| Painel do dia no dashboard: atrasados, entregar hoje, produzir | `DashboardController`, `dashboard.html`, `PedidoRepository.findPorDataEntrega/findAtrasados`, `OrdemProducaoRepository.findAbertasPorPrazo` |
| Coluna `#Conta` do aging removida (duplicava o pedido) | `AgingDto`, `financeiro/aging.html` |

Sem migration Flyway: tudo derivado de colunas existentes (próxima migration livre continua V15).
`EM_PRODUCAO` deixou de ser estado morto. `ContaReceber`/`contas_receber` ficam sem uso — remoção
em migration futura.

Testes: 130 testes, 0 falhas (`mvn test` no monólito). Novos: `ProducaoStatusPedidoTest`,
`AgingDerivadoTest`, `PainelDoDiaTest`, mais 5 casos em `PedidoStateMachineTest`.

### Massa de testes e performance (2026-09-26)

`infra/seed/seed-massa-teste.sql` popula o banco local com todos os cenários em volume de estresse
(5.000 pedidos em 3 safras, 30k movimentações, 13.9k ordens, 1.5k orçamentos, todos os enums e os
casos limítrofes). Comando em `docs/09-quickstart.md` §8.

Gargalos corrigidos com essa massa: `/estoque/movimentacoes` e `/producao` passaram a paginar
(50 por página, padrão de `/auditoria`), o Kanban limita cada coluna no banco e o painel do dia
do dashboard mostra 10 linhas por card com total no badge.

| Tela | Antes | Depois |
|---|---|---|
| `/producao` | 2,5s · 18 MB · 2.669 queries | 0,09s · 104 KB · 44 queries |
| `/estoque/movimentacoes` | 5,5s · 34 MB · 30 queries | 0,09s · 100 KB · 6 queries |
| `/` | 0,4s · 884 KB · 386 queries | 0,25s · 61 KB · 57 queries |

Corrigido também: soft-delete de `Cliente`/`Produto` deixou de usar `@SQLRestriction` (era ele que
derrubava as telas com `FetchNotFoundException` ao navegar da FK para registro excluído). O filtro
de excluídos passou a ser explícito nas listagens/combos e em `findVigenteById`/`findVigentesByIds`
para cadastro novo — histórico mostra o nome real e nada quebra. E `crm/dashboard.html`/`perfil.html`
passaram a acessar `segmento().badgeColor` como propriedade (o enum expõe `getBadgeColor()`).

Paginação concluída em `/pedidos`, `/clientes`, `/orcamentos` e `/qualidade` (50 por página, padrão
de `/auditoria`, filtros preservados na navegação). E a associação inversa `Produto.fichaTecnica`
foi removida: marcada lazy, o Hibernate a carregava eager sem bytecode enhancement, custando um
`select` em `fichas_tecnicas` por produto — o fallback de custo do `CustoRealService` agora busca as
fichas em uma query via `FichaTecnicaService.buscarPorProdutoIds`.

| Tela | Antes | Depois |
|---|---|---|
| `/pedidos` | 5 MB · 4 queries | 90 KB · 5 queries |
| `/producao` | 18 MB · 2.669 queries | 104 KB · 5 queries |
| `/estoque/movimentacoes` | 34 MB · 30 queries | 100 KB · 6 queries |
| `/clientes`, `/orcamentos`, `/qualidade` | 1–2 MB cada | ~110 KB cada |

Bugs em aberto (detalhe em `EVOLUCAO_FLUXO_PASCOA.md`): N+1 em `/crm` (795 queries) e em
`/financeiro/custo-real/{id}` (508 queries, `contarUnidadesMes` em memória); `/financeiro/aging`
renderiza 1.101 linhas de uma vez.

### Publicado no sandbox AWS (2026-09-26)

Imagem `pascoa-sandbox:03113bb` no ECR da conta 896328389222, deploy por SSM na EC2
`i-058913654054bd84b` (`aws/scripts/02-build-push.sh` + `03-deploy.sh`), readiness UP. A mesma massa
de teste foi aplicada no Postgres do sandbox (seed enviado comprimido por SSM). Trial em
https://fhz145okvk.execute-api.us-east-1.amazonaws.com — `admin` com o valor de
`admin_senha_inicial` do `terraform.tfvars`; os usuários da massa (`financeiro`, `atendente`,
`confeiteiro`, `qualidade`, `analista`, `admin2`) compartilham essa senha.

Todas as telas responderam 200 na cloud, em 0,7–1,2s (aging 2,8s, breakeven 2,2s) num t3.small
atrás do API Gateway. Sem migration nesta evolução: o schema do sandbox não mudou.

---

## 26. Próximas Sessões — Prioridade Sugerida

1. **Simulador de cenários financeiros** — "e se aumentar o preço X%? vender Y unidades a mais?" (monólito)
2. **`estoque/saida.html`** — template de saída manual de matéria-prima ausente (monólito)
3. **Integração Eureka** — habilitar `EUREKA_ENABLED=true` e testar service discovery entre microsserviços
4. **Dockerizar microsserviços** — criar Dockerfiles + adicionar serviços no docker-compose.yml
5. **customer-service com dados reais** — migrar dados de clientes do monólito para pascoa_customers
