# Referência de Rotas e Endpoints — Sistema Controle Páscoa

> **Uso:** Referência rápida de todos os endpoints HTTP do sistema.  
> **Atualizado em:** 2026-05-26

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
| **Total** | **~126** | **11** |
