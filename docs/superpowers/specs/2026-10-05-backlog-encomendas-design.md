# Backlog — Controle de Encomendas para Pequenas Lojas (doces e salgados)

Data: 2026-10-05 · Branch: `feat/backlog-fase1-encomendas`

## Contexto e decisões

- **Público:** pequenas lojas que fabricam doces e salgados sob encomenda. Hoje o sistema serve a uma única confeitaria de ovos de Páscoa.
- **Modelo:** SaaS único, várias lojas, dados isolados por loja.
- **Usuários:** mistura. O dono usa o celular; a equipe usa PC ou tablet.
- **Dores priorizadas:** anotar encomenda rápido, saber quanto cobrar e lucrar, organizar produção e compras.
- **Abordagem:** fundação primeiro (Fase 0), depois as 3 dores (Fase 1), depois polimento (Fase 2). Multi-tenant fica antes das features porque o retrofit de `loja_id` encarece a cada tabela nova.
- **Congelado:** microsserviços v5. Ficam no repositório, sem novo investimento. O monólito segue como produto.
- **Princípio de UX:** o dono sozinho no celular faz o fluxo principal com o mínimo de toques. Recurso avançado fica fora do menu padrão.

Critério de pronto de qualquer item: funciona no celular, não exige leitura de manual, tem teste para a regra de negócio nova e atualiza `docs/05-estado-implementacao.md`.

## Fase 0 — Fundação

| # | Item | Observação |
|---|---|---|
| F0.1 | Multi-tenant: `loja_id` em todas as entidades, filtro automático (Hibernate filter), testes de isolamento entre lojas | Bloqueia a venda a terceiros. Migration com `loja_id NOT NULL DEFAULT` apontando para a loja atual |
| F0.2 | Cadastro self-service da loja e onboarding de 3 passos (nome da loja, produtos de exemplo, primeiro pedido) | Hoje só ADMIN cria usuário |
| F0.3 (concluído) | Generalizar "Páscoa": produto com unidade de venda (unidade, dúzia, cento, kg), categoria (doce, salgado, ovo, outro), sazonal opcional. "Safra" vira "período" | Salgado se vende por cento |
| F0.4 | Modo simples: 6 roles viram 2 (Dono, Equipe). Qualidade, LGPD, auditoria e 2FA vão para "Configurações avançadas". Menu enxuto | Mapear roles antigas para as novas na migration |
| F0.5 | Congelar microsserviços v5 | Decisão confirmada: parar de investir, não apagar |
| F0.6 | Dívidas de segurança: exclusão por GET virar POST com CSRF (`/notificacoes/templates/{id}/excluir`), senha mínima de 4 caracteres, uploads em S3 | Antes de abrir a terceiros |

## Fase 1 — As 3 dores

Ordem de ataque: 1A, 1C, 1B. A 1C pesa mais no dia a dia da cozinha. A 1B depende de custos bem cadastrados.

### 1A. Anotar encomenda rápido

| # | Item |
|---|---|
| 1A.1 | Tela "Novo pedido" de 1 página (cliente, itens, data, sinal), no lugar do wizard de 4 passos |
| 1A.2 | Cliente criado na hora, só com nome e telefone |
| 1A.3 | Colar mensagem do WhatsApp e sugerir itens e quantidades por busca de nome de produto (sem IA) |
| 1A.4 | "Repetir pedido" no cliente e atalhos de quantidade (+10, +50, +100) |
| 1A.5 | Botão "enviar pelo WhatsApp" com o link `/acompanhamento/{token}` já existente |

### 1B. Quanto cobrar e lucrar

| # | Item |
|---|---|
| 1B.1 | Precificação guiada: insumos e rendimento informados, preço sugerido pela margem desejada |
| 1B.2 | Margem visível ao montar o pedido (verde, amarelo, vermelho) |
| 1B.3 | Orçamento em 1 clique a partir do pedido, com PDF e texto pronto para WhatsApp |
| 1B.4 | Alerta de produto com insumo sem custo no cadastro (hoje só aparece na ordem de produção) |

### 1C. Produção e compras

| # | Item |
|---|---|
| 1C.1 | Agenda semanal: entregas por dia, com carga de produção |
| 1C.2 | Lista de produção consolidada por dia (soma dos pedidos, ex.: 200 coxinhas) |
| 1C.3 | Lista de compras: insumos dos pedidos da semana menos estoque, com custo estimado |
| 1C.4 | Tela da cozinha para tablet: itens grandes, marcar "feito" por item |
| 1C.5 | Corrigir `docs/05-estado-implementacao.md`: `estoque/saida.html` existe, mas os docs ainda o listam como ausente |

### Fora da Fase 1 (YAGNI)

IA real para ler WhatsApp, integração com WhatsApp Business API, roteirização de entregas.

## Fase 2 — Polimento (resumido, detalhar ao chegar)

| # | Item |
|---|---|
| F2.1 | Cobrança: saldo a receber por pedido, lembrete de PIX por WhatsApp |
| F2.2 | Resultado mensal simples (DRE) e simulador de preço ("e se eu subir 10%?"), já previstos no roadmap |
| F2.3 | Catálogo público como vitrine com pedido online |
| F2.4 | Notificações simplificadas: poucos eventos, texto pronto, sem configuração de canais |
| F2.5 | Planos e cobrança do SaaS |
| F2.6 | Backup automático e exportação dos dados da loja |
| F2.7 | PWA com uso offline para registrar pedido sem sinal |
| F2.8 | Desempenho: N+1 em `/crm` (795 queries) e `/financeiro/custo-real/{id}` (508 queries); paginar aging |
| F2.9 | Busca e paginação em Produtos, Matérias-Primas, Orçamentos e CRM |
| F2.10 | Fluxo de caixa por competência (F7, adiado antes) |

## Riscos

- **Multi-tenant tardio em dados existentes:** a migration precisa criar a loja inicial e preencher `loja_id` em todas as linhas. Testar em cópia do banco do sandbox.
- **Modo simples vs permissões atuais:** `RolePermissionsTest` (22 testes) precisa ser reescrito junto com F0.4.
- **Unidades de venda (F0.3):** quantidade passa a depender da unidade. Impacta `ItemPedido`, ficha técnica (rendimento) e lista de compras (1C.3). Definir a conversão antes de 1C.
- **Escopo:** a Fase 0 é grande. Cada Fx.y vira spec e plano próprios; este documento é só o mapa.

## Próximo passo

Escolher o primeiro item para detalhar em spec e plano de implementação. Sugestão: F0.1, porque bloqueia os demais.
