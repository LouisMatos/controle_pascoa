# F0.3 — Doces e salgados: categorias livres, unidade de venda e quantidade decimal

Data: 2026-10-05 · Origem: [backlog](2026-10-05-backlog-encomendas-design.md), item F0.3 · Vem antes do F0.2 (o onboarding precisa de categorias e unidades de doce/salgado).

## Objetivo

Tirar do modelo a suposição "só ovos de Páscoa". Uma loja de salgados vende por cento, uma de bolos vende por kg, uma de doces por unidade ou dúzia, e cada uma organiza o catálogo com as próprias categorias. Produção, custo e margem continuam funcionando com essas unidades.

## Decisões

| Tema | Decisão |
|---|---|
| Categorias | Livres por loja, em tabela própria. O enum `Categoria` (Trufado, Recheado, Diet, Vegano, Tradicional, Especial) deixa de existir; seus valores viram categorias da loja 1 |
| Unidade de venda | Enum novo `UnidadeVenda` (UNIDADE, DUZIA, CENTO, PACOTE, KG) em `Produto`, padrão UNIDADE. Separado do `Unidade` existente, que é de insumo (kg, g, L, mL, un, cx) |
| Preço e pedido | Preço e quantidade do pedido ficam **na unidade de venda** ("R$ 8,00 / cento", "2 cento") |
| Fator de conversão | Não existe. O rendimento da ficha já é declarado na unidade de venda, então não há "unidades por venda" |
| Quantidade | Decimal (`NUMERIC(10,3)`) em itens de pedido, itens de orçamento e ordens de produção |
| Fração | UNIDADE, DUZIA, CENTO e PACOTE exigem número inteiro. KG aceita até 3 casas, mínimo 0,001 |
| Ficha técnica | `rendimento` = quantas unidades de venda uma receita produz (bolo: 2 kg; coxinha: 3 cento). `unidade_rendimento` deixa de ser usada |
| Sazonal | Novo campo `sazonal` em `Produto`; `inicioSafra`/`fimSafra` só aparecem se marcado. Nas telas, "Safra" vira "Período"; a comparação por ano não muda |

## Execução em 3 fases

Cada fase termina com a suíte verde e pode ser commitada sozinha. Uma única migration por fase: V17 (A), V18 (B), V19 (C).

### Fase A — Categorias livres (V17)

- Tabela `categorias_produto(id, loja_id, nome, ativo)`, único por `(loja_id, nome)`; entidade `CategoriaProduto` herda `TenantEntity` (e entra no teste de isolamento).
- `produtos.categoria_id BIGINT REFERENCES categorias_produto(id)`, opcional.
- Migration: cria as 6 categorias atuais na loja 1 (nome = descrição do enum: "Trufado", "Recheado", "Diet", "Vegano", "Tradicional", "Especial"), preenche `categoria_id` pelo nome e remove `produtos.categoria`.
- Java: remove `cadastro/entity/Categoria.java`; `Produto.categoria` vira `@ManyToOne CategoriaProduto`; `ProdutoController` (4 pontos), `CatalogoController` (filtro por id em vez de `Categoria.valueOf`), `DashboardController`, `RankingProdutoDto` e a query nativa `ItemPedidoRepository.rankingProdutosPorAno` (`pr.categoria` vira join com `categorias_produto` pelo nome).
- Tela para gerenciar categorias (listar, criar, renomear, inativar) em Cadastros, para o dono e a equipe; uma categoria em uso não é excluída, só inativada.
- Templates com categoria: `produtos/lista|form`, `catalogo/index|produto`, `pedidos/wizard|detalhe`, `analytics/dashboard`, `financeiro/custo-real`.
- Seeds em `infra/seed/*.sql` que inserem `produtos.categoria` passam a resolver `categoria_id` por subconsulta ao nome.

### Fase B — Unidade de venda e sazonal (V18)

- `produtos.unidade_venda VARCHAR(10) NOT NULL DEFAULT 'UNIDADE'` e `produtos.sazonal BOOLEAN NOT NULL DEFAULT FALSE`.
- Migration: produtos que já têm `inicio_safra` ou `fim_safra` ficam `sazonal = TRUE`. `unidade_venda` sai da ficha: `unidade_rendimento` KG vira KG, CX vira PACOTE, qualquer outra vira UNIDADE.
- `fichas_tecnicas.unidade_rendimento` perde o NOT NULL (a coluna fica por compatibilidade; sem uso novo).
- Java: enum `UnidadeVenda` (com rótulo e flag `fracionavel`), `Produto.unidadeVenda` e `Produto.sazonal`; `FichaTecnicaService.salvarInfo` e o formulário deixam de pedir a unidade do rendimento (rótulo passa a "Rendimento (em <unidade de venda>)").
- UI: rótulo da unidade ao lado do preço e da quantidade em produto, pedido, orçamento e catálogo; campos de temporada condicionados ao `sazonal`; "Safra" vira "Período" nos textos de analytics.

### Fase C — Quantidade decimal (V19)

- Migration: `itens_pedido.quantidade`, `orcamento_itens.quantidade` e `ordens_producao.quantidade` passam de `INTEGER` para `NUMERIC(10,3)`.
- Entidades e DTOs: `ItemPedido`, `OrcamentoItem`, `OrdemProducao`, `ItemPedidoForm`, `OrcamentoItemForm`, `CustoRealDto` passam de `Integer`/`int` para `BigDecimal`; `TopProdutoDto.quantidadeVendida` de `Long` para `BigDecimal`.
- Cálculo, pontos exatos: `PedidoService:182,261` (`adicionarItem`, consumo de insumo), `ProducaoService:83,100,131,171,183` (`calcularReceita` já divide por rendimento; a quantidade deixa de passar por `valueOf(int)`; custo por unidade usa `signum()`), `OrcamentoService:169,202`, `CustoRealService:48,51,68,128,147` (somas com `mapToLong` viram `reduce(BigDecimal.ZERO, add)`), `BreakevenService:60`, `FinanceiroService:82`, `ExportService:208`, `ProducaoPdfService:31` (texto "unidade(s)" passa a usar o rótulo da unidade de venda), `OrcamentoPdfService:38`.
- Query nativa `ItemPedidoRepository.rankingProdutosPorAno`: `SUM(i.quantidade)::bigint` vira `SUM(i.quantidade)`, e `RankingProdutoDto` acompanha.
- Validação (Bean Validation + regra no service): `@DecimalMin("0.001")`; se `!produto.unidadeVenda.fracionavel`, a quantidade precisa ser inteira. Mensagem em português no formulário.
- Exibição: sem zeros à direita (`2`, `1,5`, `0,25`), um helper único usado pelos templates e PDFs.
- Parsing de formulário com vírgula decimal (`1,5`) em pt-BR.

## Testes

- A: `CategoriaProduto` isolada por loja (entra em `TenantIsolamentoTest`); migration V17 na cópia do banco (6 categorias, 0 produtos sem vínculo); catálogo filtra por categoria da loja.
- B: validação e padrão de `UnidadeVenda`; migration V18 mapeia KG/CX/outros; `sazonal` derivado das datas.
- C: subtotal e custo com fração (`1,5 kg × R$ 40,00 = R$ 60,00`); `calcularReceita` com quantidade fracionada; rejeição de `1,5` para CENTO e aceitação para KG; soma de unidades em `CustoRealService`; ranking com `NUMERIC`.
- Testes e builders existentes que passam `Integer` ganham `BigDecimal`: churn grande e mecânico em `PedidoStateMachineTest`, `ProducaoReceitaTest`, `PainelDoDiaTest`, `AgingDerivadoTest`, `CustoRealServiceIntegrationTest`, entre outros.
- Cada migration validada em Postgres real com a massa de teste (`infra/seed/seed-massa-teste.sql`) e `ddl-auto=validate`.

## Riscos

- **Dados existentes na ficha:** a migration V18 deduz `unidade_venda` de `unidade_rendimento`. Antes de rodar, conferir `SELECT DISTINCT unidade_rendimento FROM fichas_tecnicas` no dev e no sandbox. Fichas em G, L ou mL viram UNIDADE e o rendimento deixa de significar o que significava; esses casos exigem revisão manual.
- **Fase C é larga:** mais de 20 arquivos e muitos testes. Mantê-la como última fase evita que um problema nela bloqueie A e B.
- **Preço histórico:** `ItemPedido.precoUnitario` continua sendo o preço na unidade de venda vigente quando o item foi criado. Se a loja trocar a unidade de venda de um produto que já tem pedidos, o histórico não é reinterpretado nem rastreado; o rótulo exibido nos pedidos antigos passa a ser o da unidade atual. Aceito por enquanto: trocar a unidade de um produto vendido é raro.
- **Seeds:** `infra/seed/*.sql` mudam na Fase A (categoria) e na C se inserirem quantidade fracionada.

## Fora do escopo

Conversão automática entre unidades (cento para unidade), embalagens e variações do mesmo produto (tamanho P/M/G), tela de preços por unidade alternativa, estoque de produto acabado em unidade de venda. Onboarding e cadastro de loja são o F0.2.

## Próximo passo

Plano de implementação em 3 fases (A, B, C) e execução subagent-driven, depois que a revisão final do F0.1 fechar.
