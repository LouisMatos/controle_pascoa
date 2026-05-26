# Estado de Implementação — Sistema Controle Páscoa

> **Verificado em:** 2026-05-26 (leitura direta dos arquivos `.java` e `.html`)  
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
| Notificações (email + WhatsApp) — 8 eventos | ✅ Completo |
| Notificações de aniversário / expiração / SMS | ❌ Não iniciado |
| Analytics (safras, ranking) | ✅ Completo |
| Catálogo público | ✅ Completo |
| PWA | ✅ Completo |
| Segurança / RBAC | ✅ Completo |
| Testes de integração | ✅ 6 classes cobrindo módulos críticos |

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
- Segmentação (`SegmentoCliente`)
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

### ❌ Não Iniciado (item 9 do roadmap)
- Notificação de **aniversário do cliente** — sem evento `ANIVERSARIO_CLIENTE` no enum
- Notificação de **orçamento expirando** — sem evento `ORCAMENTO_EXPIRANDO` no enum
- **SMS fallback** — sem canal SMS em `CanalNotificacao` (apenas EMAIL e WHATSAPP)

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

---

## 14. Migrations Flyway

### ✅ Arquivos confirmados em `db/migration/`

| Arquivo | Status |
|---------|--------|
| `V1__baseline.sql` | ✅ Presente |
| `V2__novas_tabelas_v3.sql` | ✅ Presente |
| `V3__crm_notas.sql` | ✅ Presente |
| `V4__alertas_internos.sql` | ✅ Presente |

> **Nota:** Arquivos adicionais (`v2_item1_campos_novos.sql`, `v2_item2_entidades_financeiras.sql`, `v2_item4_notificacoes.sql`) **não existem como arquivos separados**. Seu conteúdo está provavelmente embutido no V2 ou foi aplicado por outro meio.

---

## 15. Testes

### ✅ 6 classes de teste de integração

| Classe | Cobre |
|--------|-------|
| `PascoaApplicationTests` | Context load básico |
| `CustoRealServiceIntegrationTest` | Custo real via Ficha Técnica |
| `FluxoCaixaGastosIntegrationTest` | Fluxo de caixa com gastos variáveis |
| `NotificacaoEventListenerTest` | Listener de eventos + envio |
| `AlertaInternoIntegrationTest` | Criação e leitura de alertas |
| `OrcamentoServiceIntegrationTest` | CRUD orçamento + conversão em pedido |
| `RolePermissionsTest` | RBAC (autorização por role) |

**Profile de teste:** H2 in-memory, `ddl-auto=create-drop`, sem Flyway.

**Sem testes para:** PedidoService (máquina de estados), EstoqueService, CrmService, AnalyticsService.

---

## 16. Bugs e Problemas Conhecidos

### 🐛 Bug: URL hardcoded em `NotificacaoService`

**Arquivo:** `notificacao/service/NotificacaoService.java:132`

```java
String link = "http://localhost:8080/acompanhamento/" + pedido.getTokenAcompanhamento();
```

**Problema:** A URL base está hardcoded para `localhost:8080`. Em produção, o link enviado ao cliente no e-mail/WhatsApp apontará para localhost, tornando o rastreamento inacessível.

**Correção necessária:** Externalizar para `application.properties`:
```properties
app.base-url=https://seudominio.com.br
```
E injetar via `@Value("${app.base-url}")` no service.

---

### ⚠️ Gap: Template `estoque/saida.html` ausente

**Problema:** O arquivo `src/main/resources/templates/estoque/saida.html` não existe no projeto. A operação de saída manual de matéria-prima pode não ter tela acessível pela UI.

**Ação:** Criar o template seguindo o padrão de `estoque/entrada.html`.

---

## 17. Roadmap — Itens Pendentes (design doc v3, item 11)

| # | Item | Status | Observação |
|---|------|--------|------------|
| 9 | Notificações: aniversário, orçamento expirando, SMS | ❌ Não iniciado | Requer novos valores no enum `EventoNotificacao` + canal SMS em `CanalNotificacao` |
| 10a | DRE simplificado | ❌ Não iniciado | Nova tela + FinanceiroService novo método + template |
| 10b | Simulador de cenários financeiros | ❌ Não iniciado | Nova tela + cálculos hipotéticos em BreakevenService |

**Itens já concluídos (revisão do CLAUDE.md):**

| # | Item | Confirmação |
|---|------|------------|
| 1 | Flyway baseline | ✅ V1-V4 presentes |
| 2 | Módulo Gastos | ✅ CRUD + CSV + dashboard + integração financeiro |
| 3 | Orçamentos | ✅ PDF + aprovação pública + conversão |
| 4 | Catálogo + QR Code | ✅ Controller público + ZXing |
| 5 | CRM | ✅ Segmentação + LTV + notas + pontos |
| 6 | Controle de Qualidade | ✅ Checklist JSONB + inspeção |
| 7 | Analytics | ✅ Comparativo safras + ranking |
| 8 | PWA | ✅ manifest.json + Service Worker |
| 11 | Roles GESTOR_QUALIDADE e ANALISTA | ✅ No enum Role |
| 12 | Testes de integração | ✅ 6 classes |

---

## 18. Próximas Sessões — Prioridade Sugerida

1. **Corrigir bug `app.base-url` hardcoded** — baixo esforço, alto impacto em produção
2. **Criar template `estoque/saida.html`** — completar gap do módulo de estoque
3. **Item 9: Notificações pendentes** — adicionar eventos ao enum + implementar agendamento (aniversário requer `@Scheduled`)
4. **Item 10: DRE + simulador** — novas telas no módulo financeiro
