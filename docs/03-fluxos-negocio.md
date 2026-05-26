# Documentação de Fluxos de Negócio — Sistema Controle Páscoa

> **Projeto:** Sistema de Gestão de Ovos de Páscoa Artesanal  
> **Versão:** Spring Boot 3.3.4 / Java 21  
> **Atualizado em:** 2026-05-26

---

## 1. Contexto de Negócio

O sistema gerencia a operação completa de uma **confeitaria artesanal de ovos de Páscoa**, cobrindo desde o cadastro de produtos e matérias-primas até a entrega ao cliente, controle financeiro e análise de resultados por safra (temporada de Páscoa).

**Atores do sistema:**

| Ator | Role | Responsabilidades |
|------|------|------------------|
| Administrador | ADMIN | Configurações, usuários, acesso total |
| Financeiro | FINANCEIRO | Contas, despesas, relatórios financeiros |
| Atendente | ATENDENTE | Clientes, pedidos, orçamentos, CRM |
| Confeiteiro | CONFEITEIRO | Produção, fila de ordens |
| Gestor de Qualidade | GESTOR_QUALIDADE | Inspeção e aprovação da produção |
| Analista | ANALISTA | Dashboards, analytics, relatórios |
| **Cliente** | — | Rastreamento público, aprovação de orçamento |

---

## 2. Fluxo Principal: Ciclo de Vida de um Pedido

Este é o fluxo central do sistema, que conecta quase todos os módulos.

```
CLIENTE solicita produto
        │
        ▼
[1. ATENDIMENTO]
  Atendente verifica cliente (CRM)
  Atendente cria Pedido (ou converte Orçamento)
        │
        ▼
[2. CONFIRMAÇÃO] → status: NOVO → CONFIRMADO
  Atendente confirma pedido
  Sistema envia notificação ao cliente (email/WhatsApp)
  Sistema cria OrdemProducao automaticamente
        │
        ▼
[3. PRODUÇÃO] → status: CONFIRMADO → EM_PRODUCAO
  Confeiteiro inicia produção (Kanban)
  Sistema notifica cliente: "Seu pedido está sendo produzido"
        │
        ▼
[4. QUALIDADE]
  Gestor de Qualidade realiza inspeção com checklist
  Se REPROVADO → alerta interno para confeiteiro reprocessar
  Se APROVADO → produção concluída
        │
        ▼
[5. PRONTO] → status: EM_PRODUCAO → PRONTO
  Confeiteiro marca ordem como concluída
  Sistema notifica cliente: "Pedido pronto para retirada/entrega"
        │
        ▼
[6. ENTREGA] → status: PRONTO → ENTREGUE
  Atendente registra entrega
  Sistema registra pagamento
  Sistema notifica cliente: confirmação de entrega
  CRM atualiza histórico e pontos de fidelidade
```

### 2.1 Máquina de Estados do Pedido

```
NOVO ──[confirmar]──► CONFIRMADO ──[iniciar]──► EM_PRODUCAO ──[marcar pronto]──► PRONTO ──[entregar]──► ENTREGUE
  │                      │                          │                               │
  └──[cancelar]──►  CANCELADO    ◄──[cancelar]───────┘               ◄──[cancelar]──┘
```

**Endpoints de transição:**

| Ação | Endpoint |
|------|---------|
| Confirmar pedido | `POST /pedidos/{id}/confirmar` |
| Iniciar produção | `POST /pedidos/{id}/iniciar-producao` |
| Marcar como pronto | `POST /pedidos/{id}/marcar-pronto` |
| Registrar entrega | `POST /pedidos/{id}/registrar-entrega` |
| Cancelar | `POST /pedidos/{id}/cancelar` |

### 2.2 Notificações Automáticas por Status

| Transição | Evento Notificação | Canal |
|-----------|-------------------|-------|
| NOVO → CONFIRMADO | `PEDIDO_CONFIRMADO` | Email + WhatsApp |
| CONFIRMADO → EM_PRODUCAO | `PRODUCAO_INICIADA` | Email + WhatsApp |
| EM_PRODUCAO → PRONTO | `PEDIDO_PRONTO` | Email + WhatsApp |
| PRONTO → ENTREGUE | `PEDIDO_ENTREGUE` | Email + WhatsApp |
| → CANCELADO | `PEDIDO_CANCELADO` | Email + WhatsApp |

---

## 3. Fluxo de Orçamento (Cotação)

Fluxo alternativo para clientes que querem uma cotação antes de confirmar o pedido.

```
[1. CRIAÇÃO DO ORÇAMENTO]
  Atendente cria orçamento com produtos e quantidades
  Sistema gera token único de aprovação
  Status: PENDENTE
        │
        ▼
[2. ENVIO AO CLIENTE]
  Atendente gera PDF do orçamento (opcional)
  Atendente compartilha link público: /orcamento-publico/{token}
        │
        ▼
[3. DECISÃO DO CLIENTE] (via link público, sem login)
  ├── APROVADO → status: APROVADO
  │     Sistema notifica atendente (EventoNotificacao: ORCAMENTO_APROVADO)
  │     Atendente converte orçamento em pedido (1 clique)
  │
  └── RECUSADO → status: RECUSADO
        Sistema notifica atendente (EventoNotificacao: ORCAMENTO_RECUSADO)
        CRM registra rejeição
        │
        ▼
[4. EXPIRAÇÃO AUTOMÁTICA]
  Se data de validade < hoje → status: EXPIRADO
```

### 3.1 Endpoints Principais

| Ação | Endpoint |
|------|---------|
| Listar orçamentos | `GET /orcamentos` |
| Criar orçamento | `GET/POST /orcamentos/novo` |
| Detalhe | `GET /orcamentos/{id}` |
| Gerar PDF | `GET /orcamentos/{id}/pdf` |
| Converter em pedido | `POST /orcamentos/{id}/converter` |
| Aprovação pública | `GET /orcamento-publico/{token}` |
| Aprovar (cliente) | `POST /orcamento-publico/{token}/aprovar` |
| Recusar (cliente) | `POST /orcamento-publico/{token}/recusar` |

---

## 4. Fluxo de Produção e Qualidade

```
[Criação automática via Pedido]
  Quando pedido vai para CONFIRMADO, sistema cria OrdemProducao
  Status inicial: PENDENTE
        │
        ▼
[1. FILA DE PRODUÇÃO]
  Confeiteiro acessa /producao/fila ou /producao/kanban
  Visualiza ordens por status (PENDENTE / EM_ANDAMENTO / CONCLUIDA)
        │
        ▼
[2. INICIAR PRODUÇÃO]
  Confeiteiro clica "Iniciar" na ordem
  Status: PENDENTE → EM_ANDAMENTO
  Data de início registrada
  Sistema sincroniza status do Pedido pai → EM_PRODUCAO
        │
        ▼
[3. INSPEÇÃO DE QUALIDADE]
  Gestor de Qualidade acessa /qualidade/lista
  Cria nova inspeção para a ordem
  Preenche checklist (armazenado como JSONB):
    ☑ Aparência visual
    ☑ Peso correto
    ☑ Recheio conforme
    ☑ Embalagem intacta
    ☑ Data de validade
  
  ├── APROVADO
  │     OrdemProducao → CONCLUIDA
  │     Data de conclusão registrada
  │     Pedido pode avançar para PRONTO
  │
  └── REPROVADO
        Evento InspecaoReprovadaEvent publicado
        Alerta interno gerado para confeiteiro
        Confeiteiro reprocessa ou descarta item
        Nova inspeção pode ser criada
```

---

## 5. Fluxo de Estoque e Ficha Técnica

### 5.1 Cadastro de Ficha Técnica

```
Produto precisa ter ficha técnica para:
  - calcular custo de produção
  - baixar automaticamente estoque de matérias-primas

[1. CRIAR FICHA TÉCNICA]
  /fichas/{produtoId}/novo ou /fichas/{produtoId}/editar
  Informar:
    - Rendimento (ex: 10 unidades por receita)
    - Unidade de rendimento
    - Ingredientes (matérias-primas) + quantidade

[2. CÁLCULO AUTOMÁTICO DE CUSTO]
  CustoRealService.calcularCustoReal(pedido):
    Para cada item do pedido:
      → buscar FichaTecnica do produto
      → multiplicar ingredientes × quantidade do pedido
      → calcular custo total via custo médio ponderado de estoque
    → retornar CustoRealDto com custoVariavel, custoFixo, margem
```

### 5.2 Movimentação de Estoque

```
[ENTRADA]  /estoque/entrada
  Informar: matéria-prima, quantidade, custo unitário
  Sistema calcula novo custo médio ponderado
  Registra MovimentacaoEstoque tipo ENTRADA

[SAÍDA]    /estoque/saida
  Informar: matéria-prima, quantidade
  Sistema verifica se há saldo suficiente
  Se insuficiente → EstoqueInsuficienteException
  Registra MovimentacaoEstoque tipo SAIDA

[AJUSTE]   /estoque/ajuste
  Informar: matéria-prima, nova quantidade, motivo
  Sistema calcula diferença e registra AJUSTE
  Atualiza quantidadeAtual na MateriaPrima

[ALERTA DE ESTOQUE CRÍTICO]
  MateriaPrimaService verifica: quantidadeAtual < quantidadeMinima
  Dashboard exibe badge "Estoque Crítico" no KPI
```

---

## 6. Fluxo Financeiro

### 6.1 Contas a Receber

```
[Geração automática]
  Quando pedido vai para CONFIRMADO:
    Sistema cria ContaReceber com valor = total do pedido
    Status: ABERTA
    Vencimento: data de entrega do pedido

[Registro de pagamento]
  Atendente registra Pagamento no detalhe do pedido
  Sistema atualiza valorPago em ContaReceber
  Se valorPago >= valorOriginal → status: PAGA

[Inadimplência]
  ContaReceber vencimento < hoje && status != PAGA → VENCIDA
  Aging report mostra buckets: 0-30, 30-60, 60-90, 90+ dias em atraso
```

### 6.2 Contas a Pagar

```
[Lançamento manual]
  Financeiro acessa /financeiro/contas-pagar
  Informa: fornecedor, valor, vencimento, categoria
  Status inicial: ABERTA

[Controle de vencimento]
  Status automático: ABERTA → VENCIDA (quando vencimento < hoje)
  Financeiro registra pagamento → PAGA

[Despesas Fixas]
  Configuradas uma vez, repetidas mensalmente
  Exemplos: aluguel, contador, internet
  BreakevenService usa despesas fixas para calcular ponto de equilíbrio
```

### 6.3 Análise de Break-even

```
[Dados de entrada]
  - Total de despesas fixas mensais (DespesasFixas ativas)
  - Margem de contribuição média dos produtos

[Cálculo]
  Ponto de Equilíbrio (unidades) = Despesas Fixas / Margem de Contribuição Unitária
  Ponto de Equilíbrio (R$)      = Despesas Fixas / (Margem% / 100)

[Projeção de Safra]
  BreakevenService.calcularProjecaoSafra(ano):
    - Dias restantes até fim da safra
    - Vendas realizadas até agora
    - Taxa de crescimento necessária para atingir meta
```

### 6.4 Fluxo de Caixa

```
FluxoCaixaService.calcularFluxo(dataInicio, dataFim):
  [Entradas]
    - Pagamentos de pedidos recebidos no período
    - ContasReceber pagas no período

  [Saídas]
    - ContasPagar pagas no período
    - DespesasFixas (rateio mensal)
    - GastosVariaveis no período

  [Resultado]
    - Saldo inicial
    - Entradas por período (semanal/mensal)
    - Saídas por período
    - Saldo final
    - Projeção de saldo futuro
```

### 6.5 Controle de Gastos vs Orçamento

```
[Lançamento de Gastos Variáveis]
  Financeiro lança gasto via /gastos/novo
  Ou importa CSV/Excel via /gastos/importar

[Orçamento de Gastos]
  Financeiro define meta por categoria e mês

[Dashboard de Gastos]
  Mostra: orçado vs realizado por categoria
  Destaca categorias acima do orçado (vermelho)
```

---

## 7. Fluxo de Notificações

```
[1. Configuração] (uma vez, pelo ADMIN)
  /notificacoes/configuracao
    → Ativar/desativar canal EMAIL
    → Ativar/desativar canal WHATSAPP
    → Configurar credenciais (SMTP, Evolution API)
    → Ativar testMode (envia para email/número de teste)

[2. Templates]
  /notificacoes/templates
    → Editar corpo do template por EventoNotificacao + CanalNotificacao
    → Usar placeholders: {{nomeCliente}}, {{numeroPedido}}, {{dataEntrega}}, etc.

[3. Fluxo de Envio] (automático por evento)
  Evento publicado (ex: PedidoStatusEvent com PEDIDO_CONFIRMADO)
        │
        ▼
  NotificacaoEventListener.onPedidoStatus(event)
        │
        ▼
  NotificacaoService.enviarNotificacao(cliente, evento, variaveis)
    1. Verifica se canal está ativo (ConfiguracaoCanal)
    2. Verifica opt-in do cliente (cliente.optIn)
    3. Busca template por (evento, canal)
    4. Substitui variáveis {{}} no template
    5. Delega a EmailService ou WhatsAppService
    6. Registra NotificacaoEnviada (status: ENVIADO ou FALHA)

[4. Alertas Internos]
  AlertaInternoListener cria AlertaInterno para usuários do sistema
  Exibidos no sino (🔔) do navbar
  Marcados como lidos ao visualizar
```

---

## 8. Fluxo de CRM

```
[Perfil do Cliente]  /crm/clientes/{id}
  Exibe:
    - Histórico completo de pedidos
    - LTV (Lifetime Value = soma de todos os pedidos)
    - Ticket médio
    - Data do último pedido
    - Segmentação automática
    - Notas do atendente
    - Pontos de fidelidade

[Segmentação Automática]
  CrmService.segmentar(cliente):
    VIP       → LTV > limite_vip
    FREQUENTE → pedidos últimos 30 dias > N
    NOVO      → menos de X pedidos no total
    INATIVO   → último pedido > 90 dias atrás

[Notas do Atendente]
  Atendente adiciona nota no perfil do cliente
  Nota fica vinculada ao cliente com data e autor
  Histórico permanente (não deletável)

[Pontos de Fidelidade]
  Ganhos: automaticamente ao confirmar pedido (baseado no valor)
  Resgatados: manualmente pelo atendente
  Saldo: soma de GANHO - RESGATADO
```

---

## 9. Fluxo de Rastreamento Público de Pedido

```
[Geração do Token]
  Quando pedido é criado, sistema gera tokenAcompanhamento (UUID aleatório)
  Token é exibido no detalhe do pedido para o atendente compartilhar

[Acesso Público]
  URL: /acompanhamento/{token}
  Sem login necessário
  Exibe:
    - Nome do produto(s)
    - Status atual do pedido com timeline visual
    - Data prevista de entrega
    - Mensagem amigável por status

[Timeline de Status]
  NOVO       → "Pedido recebido"
  CONFIRMADO → "Pedido confirmado, aguardando produção"
  EM_PRODUCAO→ "Sendo produzido com carinho 🍫"
  PRONTO     → "Pronto! Aguardando retirada/entrega"
  ENTREGUE   → "Entregue! Bom proveito! 🐣"
  CANCELADO  → "Pedido cancelado"
```

---

## 10. Fluxo de Analytics por Safra

```
[Contexto]
  "Safra" = temporada de vendas de Páscoa (geralmente Jan-Abril)
  Sistema permite comparar safras de diferentes anos

[Dados analisados]
  AnalyticsService.comparativoSafras(ano1, ano2):
    - Total de pedidos por safra
    - Faturamento por safra
    - Ticket médio por safra
    - Top produtos por volume e receita
    - Crescimento percentual entre safras

[Ranking de Produtos]
  AnalyticsService.rankingProdutos(mes, ano):
    - Ordenado por volume (quantidade) ou receita
    - Margem de contribuição por produto
    - Comparativo com período anterior

[Dashboard Principal]  GET /
  KPIs em tempo real:
    - Total de clientes ativos
    - Total de produtos ativos
    - Pedidos em aberto
    - Matérias-primas com estoque crítico
    - Faturamento do mês atual
    - Meta de faturamento vs realizado (%)
```

---

## 11. Fluxo de Catálogo Público

```
[Acesso]
  URL: /catalogo
  Sem login necessário

[Exibição]
  Produtos ativos, agrupados por categoria
  Foto, nome, descrição
  Disponibilidade (se dentro da safra)
  NÃO exibe preços (apenas para pedidos/orçamentos internos)

[Detalhe]
  /catalogo/{id}
  Informações detalhadas do produto
  QR Code de acesso ao catálogo (gerado via ZXing)
```

---

## 12. Fluxo de Gestão de Usuários

```
[Acesso]  /usuarios  (apenas ADMIN)

[Criação]
  ADMIN informa: nome, login, role
  Sistema gera senha temporária ou ADMIN define
  Usuário recebe credenciais por fora do sistema

[Gerenciamento]
  ADMIN pode: editar role, ativar/desativar usuário
  Usuário desativado não consegue logar (blocked no UserDetailsService)
  ADMIN não pode se auto-deletar (validação de negócio)
```

---

## 13. Regras de Negócio Críticas

| Regra | Implementação |
|-------|--------------|
| Preço do item é fixado no momento do pedido | `ItemPedido.precoUnitario` copiado de `Produto.precoVenda` |
| Cliente com opt-in=false não recebe notificações | `NotificacaoService` verifica `cliente.optIn` antes de enviar |
| Soft-delete em clientes e produtos | Registros nunca são fisicamente deletados |
| Estoque não pode ficar negativo | `EstoqueInsuficienteException` em `EstoqueService` |
| Orçamento aprovado não pode ser editado | Service bloqueia edição de status != PENDENTE |
| Pedido entregue não pode voltar a status anterior | Máquina de estados não tem transição reversa para ENTREGUE |
| Custo real calculado só com FichaTécnica | Se produto sem ficha técnica → custo não calculado |
| Meta de faturamento configurável | `ConfiguracaoFinanceira.metaFaturamentoMensal` |
| Inspeção reprovada gera alerta interno | `InspecaoReprovadaEvent` → `AlertaInternoService` |
| Usuário inativo não acessa o sistema | `UsuarioService.loadUserByUsername` verifica `usuario.ativo` |

---

## 14. Integrações Externas

| Sistema | Finalidade | Configuração |
|---------|-----------|-------------|
| Gmail SMTP | Envio de e-mails ao cliente | `application.properties` + tela de config |
| Evolution API (WhatsApp) | Envio de mensagens WhatsApp | URL + token via tela de admin |

Ambas as integrações podem ser desabilitadas pelo ADMIN sem alterar código (`ConfiguracaoCanal.ativo=false`).

---

## 15. Relatórios Disponíveis

| Relatório | URL | Formato | Dados |
|-----------|-----|---------|-------|
| Dashboard principal | `/` | HTML | KPIs em tempo real |
| Dashboard financeiro | `/financeiro/dashboard` | HTML | Resumo financeiro |
| Custo real por pedido | `/financeiro/custo-real` | HTML | Custo via ficha técnica |
| Fluxo de caixa | `/financeiro/fluxo-caixa` | HTML + Gráfico | Entradas/saídas |
| Break-even | `/financeiro/breakeven` | HTML | Ponto de equilíbrio |
| Projeção de safra | `/financeiro/projecao-safra` | HTML | Meta vs realizado |
| Aging | `/financeiro/aging` | HTML | Contas vencidas por bucket |
| Dashboard analytics | `/analytics/dashboard` | HTML + Gráficos | Comparativo safras |
| Exportação pedidos | `/pedidos/exportar` | Excel (.xlsx) | Lista completa |
| PDF orçamento | `/orcamentos/{id}/pdf` | PDF | Documento para cliente |
| Dashboard gastos | `/gastos/dashboard` | HTML | Orçado vs realizado |
| Dashboard CRM | `/crm/dashboard` | HTML | Segmentação de clientes |
