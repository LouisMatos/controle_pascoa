# Índice de Documentação — Sistema Controle Páscoa

> **Projeto:** Sistema de Gestão de Ovos de Páscoa Artesanal  
> **Stack:** Java 21 + Spring Boot 3.3.4 + PostgreSQL + Thymeleaf  
> **Repositório:** `/Users/luismatos/Documents/IA - CLAUDE/controle_pascoa`  
> **Atualizado em:** 2026-05-26

---

## Documentos Disponíveis

| # | Arquivo | Conteúdo | Quando usar |
|---|---------|----------|-------------|
| 00 | [00-indice.md](00-indice.md) | Este índice | Ponto de entrada |
| 01 | [01-infraestrutura.md](01-infraestrutura.md) | Stack, banco, Flyway, segurança, uploads, e-mail, WhatsApp, PWA, testes | Infraestrutura, deploy, configurações |
| 02 | [02-arquitetura-tecnica.md](02-arquitetura-tecnica.md) | Módulos, entidades, services, repositories, DTOs, dependências entre módulos | Desenvolvimento, debugging, novos recursos |
| 03 | [03-fluxos-negocio.md](03-fluxos-negocio.md) | Fluxos de pedido, orçamento, produção, estoque, financeiro, notificações, CRM | Lógica de negócio, novas funcionalidades |
| 04 | [04-rotas-endpoints.md](04-rotas-endpoints.md) | Todos os ~126 endpoints com método HTTP e permissão | Adicionar rotas, revisar permissões |
| 05 | [05-estado-implementacao.md](05-estado-implementacao.md) | O que está feito, parcial, pendente e bugs conhecidos | Iniciar qualquer sessão de desenvolvimento |
| 06 | [06-schema-banco.md](06-schema-banco.md) | 25 tabelas com colunas, tipos, FKs, índices e guia de migrations | Escrever queries, criar migrations, debugar JPA |
| 07 | [07-convencoes-desenvolvimento.md](07-convencoes-desenvolvimento.md) | Templates de entity/service/controller/template + naming + checklist | Qualquer tarefa de desenvolvimento novo |
| 08 | [08-manutencao-docs.md](08-manutencao-docs.md) | Protocolo de fim de sessão — quais docs atualizar após cada mudança | **Executar ao final de toda sessão com código alterado** |

---

## Mapa Rápido do Sistema

```
Módulos de Negócio:
  cadastro/     → Cliente, Produto, Fornecedor, MateriaPrima
  pedido/       → Pedido, ItemPedido, Pagamento (máquina de estados)
  orcamento/    → Orcamento, OrcamentoItem, aprovação pública via token
  producao/     → OrdemProducao (Kanban / fila)
  qualidade/    → InspecaoQualidade (checklist JSONB)
  estoque/      → MovimentacaoEstoque (entrada/saída/ajuste)
  fichaTecnica/ → FichaTecnica + FichaTecnicaItem (composição do produto)
  financeiro/   → ContaReceber, ContaPagar, DespesaFixa, FluxoCaixa, Breakeven
  crm/          → NotaCliente, PontoFidelidade, segmentação
  notificacao/  → Templates por evento/canal + email + WhatsApp
  gastos/       → GastoVariavel, OrcamentoGasto
  analytics/    → Comparativo de safras, ranking de produtos
  catalogo/     → Catálogo público (sem login)
  pwa/          → Service Worker, manifest.json

Infraestrutura:
  config/       → Security, WebMvc, Auditing, DataInitializer
  common/       → BaseEntity (auditoria automática)
  shared/       → GlobalExceptionHandler, exceções customizadas
  seguranca/    → Usuario, Role (RBAC)
```

---

## Dependências Chave entre Módulos

```
cadastro → pedido → producao → qualidade
       ↘           ↘
        orcamento   notificacao (eventos)
                  ↘
                   crm
fichaTecnica ──────► financeiro (custo real)
estoque ───────────► financeiro (custo médio)
gastos ─────────────► financeiro (fluxo de caixa)
pedido + financeiro → analytics
```

---

## Entidades Principais e suas Relações

```
Cliente 1──N Pedido 1──N ItemPedido N──1 Produto
                │                         │
                │                    FichaTecnica 1──N FichaTecnicaItem N──1 MateriaPrima
                │
                └── OrdemProducao ──► InspecaoQualidade
                └── ContaReceber
                └── Pagamento

Cliente 1──N Orcamento 1──N OrcamentoItem N──1 Produto
Orcamento ──► Pedido (conversão)

MateriaPrima ──N── MovimentacaoEstoque
```

---

## Enums Críticos (Estado / Fluxo)

| Enum | Valores |
|------|---------|
| `StatusPedido` | NOVO → CONFIRMADO → EM_PRODUCAO → PRONTO → ENTREGUE / CANCELADO |
| `StatusOrcamento` | PENDENTE → APROVADO / RECUSADO / EXPIRADO |
| `StatusOrdem` | PENDENTE → EM_ANDAMENTO → CONCLUIDA |
| `TipoMovimentacao` | ENTRADA, SAIDA, AJUSTE |
| `StatusConta` | ABERTA, PAGA, VENCIDA |
| `EventoNotificacao` | PEDIDO_CONFIRMADO, PRODUCAO_INICIADA, PEDIDO_PRONTO, PEDIDO_ENTREGUE, PAGAMENTO_RECEBIDO, PEDIDO_CANCELADO, ORCAMENTO_APROVADO, ORCAMENTO_RECUSADO |
| `CanalNotificacao` | EMAIL, WHATSAPP |
| `Role` | ADMIN, FINANCEIRO, ATENDENTE, CONFEITEIRO, GESTOR_QUALIDADE, ANALISTA |

---

## Rotas Públicas (Sem Login)

| URL | Funcionalidade |
|-----|---------------|
| `/login` | Autenticação |
| `/acompanhamento/{token}` | Rastreamento de pedido pelo cliente |
| `/orcamento-publico/{token}` | Aprovação de orçamento pelo cliente |
| `/catalogo/**` | Catálogo de produtos |
| `/uploads/**` | Imagens de produtos |
| `/manifest.json`, `/sw.js`, `/icons/**` | PWA |

---

## Credenciais Padrão de Desenvolvimento

```
URL:   http://localhost:8080
Login: admin
Senha: admin123
```

> **ATENÇÃO:** Alterar imediatamente em produção.

---

## Configurações Sensíveis

| Configuração | Onde alterar |
|-------------|-------------|
| Senha do banco | `application.properties` |
| Credenciais SMTP (Gmail) | `application.properties` |
| URL + Token WhatsApp (Evolution API) | Admin em `/notificacoes/configuracao` |
| Meta de faturamento | Admin em `/financeiro/configuracao` |
| Ativar/desativar canais de notificação | Admin em `/notificacoes/configuracao` |

---

## Como Usar Esta Documentação em Novas Sessões

**Ao iniciar uma sessão de desenvolvimento**, leia nesta ordem:

1. **`00-indice.md`** — orientação geral (este arquivo)
2. **`05-estado-implementacao.md`** — o que está feito, pendente e bugs — **leia sempre**
3. **`02-arquitetura-tecnica.md`** — se a tarefa envolver código Java
4. **`03-fluxos-negocio.md`** — se a tarefa envolver lógica de negócio
5. **`01-infraestrutura.md`** — se a tarefa envolver configuração/deploy
6. **`04-rotas-endpoints.md`** — se a tarefa envolver endpoints/permissões

**Ao encerrar uma sessão em que código foi alterado:**

7. **`08-manutencao-docs.md`** — executar o protocolo de atualização das docs
