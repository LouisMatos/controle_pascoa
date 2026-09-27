# Referência de Rotas e Endpoints — Sistema Controle Páscoa

> **Uso:** Referência rápida de todos os endpoints HTTP do sistema.  
> **Cobre:** Monólito (~126 endpoints) + 9 Microsserviços REST (v5)  
> **Atualizado em:** 2026-05-29

---

## Legenda

| Símbolo | Significado |
|---------|-------------|
| 🔓 | Rota pública (sem autenticação) |
| 🔐 | Requer autenticação |
| 👑 | Apenas ADMIN |
| 💰 | ADMIN ou FINANCEIRO |
| 👤 | ADMIN ou ATENDENTE |
| 🎂 | ADMIN ou CONFEITEIRO |
| ⭐ | ADMIN ou GESTOR_QUALIDADE |
| 📊 | ADMIN, FINANCEIRO ou ANALISTA |

---

## 1. Autenticação

| Método | URL | Auth | Descrição |
|--------|-----|------|-----------|
| GET | `/login` | 🔓 | Página de login |
| POST | `/login` | 🔓 | Processar autenticação |
| GET | `/logout` | 🔐 | Encerrar sessão |

---

## 2. Dashboard

| Método | URL | Auth | Descrição |
|--------|-----|------|-----------|
| GET | `/` | 🔐 | Dashboard principal com KPIs |
| GET | `/dashboard` | 🔐 | Alias do dashboard principal |

---

## 3. Clientes

| Método | URL | Auth | Descrição |
|--------|-----|------|-----------|
| GET | `/clientes` | 🔐 | Listar clientes |
| GET | `/clientes/novo` | 🔐 | Formulário novo cliente |
| POST | `/clientes/novo` | 🔐 | Salvar novo cliente |
| GET | `/clientes/{id}/editar` | 🔐 | Formulário editar cliente |
| POST | `/clientes/{id}/editar` | 🔐 | Salvar edição de cliente |
| POST | `/clientes/{id}/excluir` | 🔐 | Soft-delete de cliente |

---

## 4. Produtos

| Método | URL | Auth | Descrição |
|--------|-----|------|-----------|
| GET | `/produtos` | 🔐 | Listar produtos |
| GET | `/produtos/novo` | 🔐 | Formulário novo produto |
| POST | `/produtos/novo` | 🔐 | Salvar novo produto (aceita imagem) |
| GET | `/produtos/{id}/editar` | 🔐 | Formulário editar produto |
| POST | `/produtos/{id}/editar` | 🔐 | Salvar edição (aceita imagem) |
| POST | `/produtos/{id}/excluir` | 🔐 | Soft-delete de produto |

---

## 5. Fornecedores

| Método | URL | Auth | Descrição |
|--------|-----|------|-----------|
| GET | `/fornecedores` | 🔐 | Listar fornecedores |
| GET | `/fornecedores/novo` | 🔐 | Formulário novo fornecedor |
| POST | `/fornecedores/novo` | 🔐 | Salvar novo fornecedor |
| GET | `/fornecedores/{id}/editar` | 🔐 | Formulário editar fornecedor |
| POST | `/fornecedores/{id}/editar` | 🔐 | Salvar edição |
| POST | `/fornecedores/{id}/excluir` | 🔐 | Excluir fornecedor |

---

## 6. Matérias-Primas

| Método | URL | Auth | Descrição |
|--------|-----|------|-----------|
| GET | `/materias-primas` | 🔐 | Listar matérias-primas |
| GET | `/materias-primas/novo` | 🔐 | Formulário nova matéria-prima |
| POST | `/materias-primas/novo` | 🔐 | Salvar |
| GET | `/materias-primas/{id}/editar` | 🔐 | Formulário editar |
| POST | `/materias-primas/{id}/editar` | 🔐 | Salvar edição |
| POST | `/materias-primas/{id}/excluir` | 🔐 | Excluir |

---

## 7. Pedidos

| Método | URL | Auth | Descrição |
|--------|-----|------|-----------|
| GET | `/pedidos` | 🔐 | Listar pedidos (filtro por status) |
| GET | `/pedidos/novo` | 🔐 | Formulário novo pedido |
| POST | `/pedidos/novo` | 🔐 | Criar pedido |
| GET | `/pedidos/wizard` | 🔐 | Wizard de criação rápida |
| POST | `/pedidos/wizard` | 🔐 | Processar wizard |
| GET | `/pedidos/{id}` | 🔐 | Detalhe do pedido |
| GET | `/pedidos/{id}/editar` | 🔐 | Formulário editar pedido |
| POST | `/pedidos/{id}/editar` | 🔐 | Salvar edição |
| POST | `/pedidos/{id}/confirmar` | 🔐 | Transição: NOVO → CONFIRMADO |
| POST | `/pedidos/{id}/iniciar-producao` | 🔐 | Transição: CONFIRMADO → EM_PRODUCAO |
| POST | `/pedidos/{id}/marcar-pronto` | 🔐 | Transição: EM_PRODUCAO → PRONTO |
| POST | `/pedidos/{id}/registrar-entrega` | 🔐 | Transição: PRONTO → ENTREGUE |
| POST | `/pedidos/{id}/cancelar` | 🔐 | Transição para CANCELADO |
| POST | `/pedidos/{id}/itens` | 🔐 | Adicionar item ao pedido |
| DELETE | `/pedidos/{id}/itens/{itemId}` | 🔐 | Remover item do pedido |
| POST | `/pedidos/{id}/pagamentos` | 🔐 | Registrar pagamento |
| GET | `/pedidos/exportar` | 🔐 | Exportar lista para Excel |

---

## 8. Rastreamento Público de Pedido

| Método | URL | Auth | Descrição |
|--------|-----|------|-----------|
| GET | `/acompanhamento/{token}` | 🔓 | Página pública de rastreamento |

---

## 9. Orçamentos

| Método | URL | Auth | Descrição |
|--------|-----|------|-----------|
| GET | `/orcamentos` | 🔐 | Listar orçamentos |
| GET | `/orcamentos/novo` | 🔐 | Formulário novo orçamento |
| POST | `/orcamentos/novo` | 🔐 | Criar orçamento |
| GET | `/orcamentos/{id}` | 🔐 | Detalhe do orçamento |
| GET | `/orcamentos/{id}/editar` | 🔐 | Formulário editar |
| POST | `/orcamentos/{id}/editar` | 🔐 | Salvar edição |
| DELETE | `/orcamentos/{id}` | 🔐 | Excluir orçamento |
| GET | `/orcamentos/{id}/pdf` | 🔐 | Gerar e baixar PDF |
| POST | `/orcamentos/{id}/converter` | 🔐 | Converter em Pedido |

**Aprovação Pública:**

| Método | URL | Auth | Descrição |
|--------|-----|------|-----------|
| GET | `/orcamento-publico/{token}` | 🔓 | Página pública de aprovação |
| POST | `/orcamento-publico/{token}/aprovar` | 🔓 | Cliente aprova orçamento |
| POST | `/orcamento-publico/{token}/recusar` | 🔓 | Cliente recusa orçamento |

---

## 10. Fichas Técnicas

| Método | URL | Auth | Descrição |
|--------|-----|------|-----------|
| GET | `/fichas` | 🔐 | Listar fichas técnicas |
| GET | `/fichas/{produtoId}/novo` | 🔐 | Criar ficha para produto |
| POST | `/fichas/{produtoId}/novo` | 🔐 | Salvar ficha |
| GET | `/fichas/{id}` | 🔐 | Detalhe da ficha |
| GET | `/fichas/{id}/editar` | 🔐 | Formulário editar |
| POST | `/fichas/{id}/editar` | 🔐 | Salvar edição |

---

## 11. Estoque

| Método | URL | Auth | Descrição |
|--------|-----|------|-----------|
| GET | `/estoque/movimentacoes` | 🔐 | Histórico de movimentações |
| GET | `/estoque/entrada` | 🔐 | Formulário entrada de estoque |
| POST | `/estoque/entrada` | 🔐 | Registrar entrada |
| GET | `/estoque/saida` | 🔐 | Formulário saída de estoque |
| POST | `/estoque/saida` | 🔐 | Registrar saída |
| GET | `/estoque/ajuste` | 🔐 | Formulário ajuste de estoque |
| POST | `/estoque/ajuste` | 🔐 | Registrar ajuste |

---

## 12. Produção

| Método | URL | Auth | Descrição |
|--------|-----|------|-----------|
| GET | `/producao/kanban` | 🎂 | Visualização Kanban das ordens |
| GET | `/producao/fila` | 🎂 | Fila de produção |
| GET | `/producao/{id}` | 🎂 | Detalhe da ordem de produção |
| POST | `/producao/{id}/iniciar` | 🎂 | Iniciar ordem (PENDENTE → EM_ANDAMENTO) |
| POST | `/producao/{id}/concluir` | 🎂 | Concluir ordem (EM_ANDAMENTO → CONCLUIDA) |

---

## 13. Qualidade

| Método | URL | Auth | Descrição |
|--------|-----|------|-----------|
| GET | `/qualidade/lista` | ⭐ | Listar inspeções |
| GET | `/qualidade/nova/{ordemId}` | ⭐ | Formulário nova inspeção |
| POST | `/qualidade/nova/{ordemId}` | ⭐ | Registrar inspeção |
| GET | `/qualidade/{id}` | ⭐ | Detalhe da inspeção |

---

## 14. Financeiro

| Método | URL | Auth | Descrição |
|--------|-----|------|-----------|
| GET | `/financeiro/dashboard` | 💰 | Dashboard financeiro consolidado |
| GET | `/financeiro/custo-real` | 💰 | Análise de custo real por pedido |
| GET | `/financeiro/fluxo-caixa` | 💰 | Fluxo de caixa com filtro de período |
| GET | `/financeiro/breakeven` | 💰 | Análise de break-even |
| GET | `/financeiro/projecao-safra` | 💰 | Projeção de safra |
| GET | `/financeiro/aging` | 💰 | Análise de vencimento (aging) |
| GET | `/financeiro/contas-pagar` | 💰 | Contas a pagar |
| POST | `/financeiro/contas-pagar/{id}/pagar` | 💰 | Registrar pagamento de conta |
| GET | `/financeiro/despesas-fixas` | 💰 | Listar despesas fixas |
| POST | `/financeiro/despesas-fixas` | 💰 | Criar despesa fixa |

---

## 15. Gastos Variáveis

| Método | URL | Auth | Descrição |
|--------|-----|------|-----------|
| GET | `/gastos` | 💰 | Listar gastos variáveis |
| GET | `/gastos/dashboard` | 💰 | Dashboard orçado vs realizado |
| GET | `/gastos/novo` | 💰 | Formulário novo gasto |
| POST | `/gastos/novo` | 💰 | Salvar gasto |
| GET | `/gastos/{id}/editar` | 💰 | Formulário editar |
| POST | `/gastos/{id}/editar` | 💰 | Salvar edição |
| POST | `/gastos/{id}/excluir` | 💰 | Excluir gasto |
| GET | `/gastos/importar` | 💰 | Tela de importação |
| POST | `/gastos/importar` | 💰 | Importar CSV/Excel |

---

## 16. CRM

| Método | URL | Auth | Descrição |
|--------|-----|------|-----------|
| GET | `/crm/dashboard` | 👤 | Dashboard de segmentação |
| GET | `/crm/clientes/{id}` | 👤 | Perfil CRM do cliente |
| POST | `/crm/clientes/{id}/notas` | 👤 | Adicionar nota ao cliente |
| POST | `/crm/clientes/{id}/pontos` | 👤 | Lançar pontos de fidelidade |

---

## 17. Analytics

| Método | URL | Auth | Descrição |
|--------|-----|------|-----------|
| GET | `/analytics/dashboard` | 📊 | Dashboard de analytics |
| GET | `/analytics/safras` | 📊 | Comparativo entre safras |
| GET | `/analytics/ranking` | 📊 | Ranking de produtos |

---

## 18. Notificações

| Método | URL | Auth | Descrição |
|--------|-----|------|-----------|
| GET | `/notificacoes/configuracao` | 👑 | Configuração de canais |
| POST | `/notificacoes/configuracao` | 👑 | Salvar configuração |
| GET | `/notificacoes/templates` | 👑 | Listar templates |
| GET | `/notificacoes/templates/{id}/editar` | 👑 | Editar template |
| POST | `/notificacoes/templates/{id}/editar` | 👑 | Salvar template |
| GET | `/notificacoes/historico` | 👑 | Histórico de envios |
| GET | `/notificacoes/alertas` | 🔐 | Alertas internos do sistema |
| POST | `/notificacoes/alertas/{id}/lido` | 🔐 | Marcar alerta como lido |

---

## 19. Usuários

| Método | URL | Auth | Descrição |
|--------|-----|------|-----------|
| GET | `/usuarios` | 👑 | Listar usuários |
| GET | `/usuarios/novo` | 👑 | Formulário novo usuário |
| POST | `/usuarios/novo` | 👑 | Criar usuário |
| GET | `/usuarios/{id}/editar` | 👑 | Formulário editar |
| POST | `/usuarios/{id}/editar` | 👑 | Salvar edição |
| POST | `/usuarios/{id}/ativar` | 👑 | Ativar usuário |
| POST | `/usuarios/{id}/desativar` | 👑 | Desativar usuário |

---

## 20. Catálogo Público

| Método | URL | Auth | Descrição |
|--------|-----|------|-----------|
| GET | `/catalogo` | 🔓 | Catálogo de produtos |
| GET | `/catalogo/{id}` | 🔓 | Detalhe do produto |

---

## 21. PWA e Estáticos

| Método | URL | Auth | Descrição |
|--------|-----|------|-----------|
| GET | `/manifest.json` | 🔓 | PWA manifest |
| GET | `/sw.js` | 🔓 | Service Worker |
| GET | `/icons/**` | 🔓 | Ícones da aplicação |
| GET | `/uploads/**` | 🔓 | Imagens de produtos |

---

## 22. Resumo por Módulo

| Módulo | Total de Rotas | Rotas Públicas |
|--------|---------------|----------------|
| Autenticação | 3 | 2 |
| Dashboard | 2 | 0 |
| Clientes | 6 | 0 |
| Produtos | 6 | 0 |
| Fornecedores | 6 | 0 |
| Matérias-Primas | 6 | 0 |
| Pedidos | 16 | 1 (rastreamento) |
| Orçamentos | 12 | 2 (aprovação pública) |
| Fichas Técnicas | 6 | 0 |
| Estoque | 7 | 0 |
| Produção | 5 | 0 |
| Qualidade | 4 | 0 |
| Financeiro | 10 | 0 |
| Gastos | 9 | 0 |
| CRM | 4 | 0 |
| Analytics | 3 | 0 |
| Notificações | 8 | 0 |
| Usuários | 7 | 0 |
| Catálogo | 2 | 2 |
| PWA/Estáticos | 4 | 4 |
| **Total Monólito** | **~126** | **11** |

---

## Microsserviços v5 — APIs REST

> Base URL de cada serviço: `http://localhost:{porta}` ou via Gateway `http://localhost:8090`  
> Autenticação: `Authorization: Bearer {jwt}` (exceto rotas públicas)  
> Roles via `@PreAuthorize("hasAnyRole(...)")`

---

### auth-service (porta 8081)

| Método | URL | Auth | Descrição |
|--------|-----|------|-----------|
| POST | `/api/auth/login` | 🔓 | Login com usuário/senha (+ TOTP opcional) → retorna access + refresh token |
| POST | `/api/auth/refresh` | 🔓 | Renovar access token via refresh token |
| POST | `/api/auth/logout` | 🔐 | Revogar access token (adiciona ao blacklist Redis) |
| GET | `/api/auth/validate` | 🔐 | Validar token e retornar claims (userId, login, roles) |
| GET | `/actuator/health` | 🔓 | Health check |

---

### customer-service (porta 8082)

| Método | URL | Auth | Descrição |
|--------|-----|------|-----------|
| GET | `/api/clientes` | 🔐 👑 | Listar todos os clientes ativos |
| GET | `/api/clientes/{id}` | 🔐 | Buscar cliente por ID |
| POST | `/api/clientes` | 🔐 👑 | Criar novo cliente |
| PUT | `/api/clientes/{id}` | 🔐 👑 | Atualizar cliente |
| DELETE | `/api/clientes/{id}` | 🔐 👑 | Inativar cliente (soft-delete) |
| GET | `/actuator/health` | 🔓 | Health check |

---

### inventory-service (porta 8083)

| Método | URL | Auth | Descrição |
|--------|-----|------|-----------|
| GET | `/api/materias-primas` | 🔐 🎂📊 | Listar matérias-primas ativas |
| GET | `/api/materias-primas/criticos` | 🔐 🎂📊 | Listar com estoque abaixo do mínimo |
| GET | `/api/materias-primas/{id}` | 🔐 🎂📊 | Buscar por ID |
| POST | `/api/materias-primas` | 🔐 👑🎂 | Criar matéria-prima |
| POST | `/api/materias-primas/{id}/entrada` | 🔐 👑🎂 | Registrar entrada de estoque |
| POST | `/api/materias-primas/{id}/saida` | 🔐 👑🎂 | Registrar saída de estoque |
| GET | `/api/materias-primas/{id}/movimentacoes` | 🔐 👑🎂📊 | Histórico de movimentações |
| GET | `/api/materias-primas/{id}/disponibilidade?quantidade=` | 🔐 👑🎂 | Verificar disponibilidade |
| GET | `/actuator/health` | 🔓 | Health check |

---

### product-service (porta 8084)

| Método | URL | Auth | Descrição |
|--------|-----|------|-----------|
| GET | `/api/produtos` | 🔐 | Listar produtos ativos |
| GET | `/api/produtos/disponiveis` | 🔓 | Listar disponíveis (catálogo público) |
| GET | `/api/produtos/categoria/{cat}` | 🔓 | Filtrar por categoria |
| GET | `/api/produtos/{id}` | 🔐 | Buscar por ID |
| POST | `/api/produtos` | 🔐 👑 | Criar produto |
| PUT | `/api/produtos/{id}` | 🔐 👑 | Atualizar produto |
| PATCH | `/api/produtos/{id}/foto` | 🔐 👑 | Atualizar foto do produto |
| DELETE | `/api/produtos/{id}` | 🔐 👑 | Inativar produto |
| GET | `/actuator/health` | 🔓 | Health check |

---

### order-service (porta 8085)

| Método | URL | Auth | Descrição |
|--------|-----|------|-----------|
| GET | `/api/pedidos` | 🔐 👤📊💰 | Listar todos os pedidos |
| GET | `/api/pedidos/{id}` | 🔐 👤📊💰 | Buscar pedido por ID |
| POST | `/api/pedidos` | 🔐 👤 | Criar novo pedido |
| POST | `/api/pedidos/{id}/itens` | 🔐 👤 | Adicionar item ao pedido |
| DELETE | `/api/pedidos/{id}/itens/{itemId}` | 🔐 👤 | Remover item do pedido |
| POST | `/api/pedidos/{id}/confirmar` | 🔐 👤 | Confirmar pedido (→CONFIRMADO) |
| POST | `/api/pedidos/{id}/cancelar` | 🔐 👤 | Cancelar pedido |
| POST | `/api/pedidos/{id}/pronto` | 🔐 🎂 | Marcar como pronto (→PRONTO) |
| POST | `/api/pedidos/{id}/entregar` | 🔐 👤 | Registrar entrega (→ENTREGUE) |
| POST | `/api/pedidos/{id}/pagamento` | 🔐 👤 | Registrar forma de pagamento |
| GET | `/api/acompanhamento/{token}` | 🔓 | Rastreamento público por token |
| GET | `/actuator/health` | 🔓 | Health check |

---

### production-service (porta 8086)

| Método | URL | Auth | Descrição |
|--------|-----|------|-----------|
| GET | `/api/producao` | 🔐 🎂📊 | Listar todas as ordens de produção |
| GET | `/api/producao/kanban` | 🔐 🎂 | Visão Kanban (PENDENTE/EM_ANDAMENTO/CONCLUIDA) |
| GET | `/api/producao/{id}` | 🔐 🎂📊 | Buscar ordem por ID |
| GET | `/api/producao/pedido/{pedidoId}` | 🔐 🎂📊 | Buscar ordem pelo ID do pedido |
| POST | `/api/producao/{id}/iniciar` | 🔐 🎂 | Iniciar produção (→EM_ANDAMENTO) |
| POST | `/api/producao/{id}/concluir` | 🔐 🎂 | Concluir produção (→CONCLUIDA) + publica evento |
| POST | `/api/producao/{id}/cancelar` | 🔐 👑 | Cancelar ordem (→CANCELADA) |
| GET | `/actuator/health` | 🔓 | Health check |

---

### financial-service (porta 8087)

| Método | URL | Auth | Descrição |
|--------|-----|------|-----------|
| GET | `/api/financeiro/lancamentos?mes=&ano=` | 🔐 💰📊 | Listar lançamentos do mês/ano |
| GET | `/api/financeiro/lancamentos/{id}` | 🔐 💰📊 | Buscar lançamento por ID |
| POST | `/api/financeiro/lancamentos` | 🔐 💰 | Registrar lançamento manual |
| GET | `/api/financeiro/resumo?mes=&ano=` | 🔐 💰📊 | Resumo mensal (receitas, despesas, margem) |
| GET | `/api/financeiro/dre?ano=` | 🔐 💰📊 | DRE anual (receita bruta, custos, lucro líquido) |
| GET | `/api/financeiro/fluxo-caixa?ano=` | 🔐 💰📊 | Fluxo de caixa mensal (12 meses) |
| GET | `/actuator/health` | 🔓 | Health check |

---

### notification-service (porta 8088)

| Método | URL | Auth | Descrição |
|--------|-----|------|-----------|
| GET | `/api/notificacoes?limite=` | 🔐 👑 | Listar notificações recentes |
| GET | `/api/notificacoes/{id}` | 🔐 👑 | Buscar notificação por ID |
| GET | `/api/notificacoes/referencia/{referenciaId}` | 🔐 👤 | Histórico por pedido/cliente |
| POST | `/api/notificacoes/{id}/reenviar` | 🔐 👑 | Reenviar notificação com falha |
| GET | `/actuator/health` | 🔓 | Health check |

---

### analytics-service (porta 8089)

| Método | URL | Auth | Descrição |
|--------|-----|------|-----------|
| GET | `/api/analytics/dashboard` | 🔐 📊💰 | Dashboard completo (safra + comparativo + ranking + mensal) |
| GET | `/api/analytics/safras/comparativo?anoAtual=&anoAnterior=` | 🔐 📊💰 | Comparativo entre duas safras (variação %) |
| GET | `/api/analytics/safras/{ano}` | 🔐 📊💰 | Métricas de uma safra específica |
| GET | `/api/analytics/produtos/ranking?ano=&limite=` | 🔐 📊💰 | Ranking de produtos (quantidade + receita) |
| GET | `/api/analytics/metricas-mensais?ano=` | 🔐 📊💰 | Métricas mês a mês (12 registros) |
| GET | `/actuator/health` | 🔓 | Health check |

---

### api-gateway (porta 8090)

| Método | URL | Auth | Descrição |
|--------|-----|------|-----------|
| GET | `/fallback/monolith` | 🔓 | Resposta 503 quando monólito está fora |
| GET | `/actuator/health` | 🔓 | Health check do gateway |
| GET | `/actuator/gateway/routes` | 🔓 | Listar rotas configuradas |

> Todas as demais rotas são **proxy transparente** para `http://localhost:8080` (monólito).

---

### Resumo Geral de Endpoints

| Serviço | Total | Públicos |
|---------|-------|---------|
| Monólito | ~126 | 11 |
| auth-service | 5 | 2 |
| customer-service | 6 | 1 |
| inventory-service | 9 | 1 |
| product-service | 9 | 3 |
| order-service | 12 | 2 |
| production-service | 7 | 1 |
| financial-service | 7 | 1 |
| notification-service | 5 | 1 |
| analytics-service | 6 | 1 |
| api-gateway | 3 | 3 |
| **Total Geral** | **~195** | **~27** |
