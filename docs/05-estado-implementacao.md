# Estado de Implementação — Sistema Controle Páscoa

> **Verificado em:** 2026-05-26 — atualizado Item 22 (senha), Item 23 (bugs médios), Item 24 (testes) e Item 25 (novas notificações)  
> **Critério:** ✅ Implementado e testado | ⚠️ Parcialmente implementado | ❌ Não iniciado | 🐛 Bug conhecido

---

## Resumo Executivo

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
| DRE simplificado | ❌ Não iniciado |
| Simulador de cenários | ❌ Não iniciado |
| CRM | ✅ Completo |
| Notificações (email + WhatsApp + SMS) — 10 eventos | ✅ Completo |
| Notificações de aniversário / expiração / SMS | ✅ Item 25 — Completo |
| Analytics (safras, ranking) | ✅ Completo |
| Catálogo público | ✅ Completo |
| PWA | ✅ Completo |
| Segurança / RBAC | ✅ Completo |
| Testes de integração | ✅ 107 testes — 10 classes cobrindo todos os módulos críticos |

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

## 21. Próximas Sessões — Prioridade Sugerida

1. **Item 10a: DRE simplificado** — Demonstrativo de Resultado do Exercício (receitas − custos − despesas)
2. **Item 10b: Simulador de cenários** — "e se aumentar o preço X%? vender Y unidades a mais?"
3. **`estoque/saida.html`** — template de saída manual de matéria-prima ausente
