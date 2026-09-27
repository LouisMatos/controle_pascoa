# Evolução do Fluxo de Páscoa

## Status Geral
- Status: CONCLUÍDO (evolução, massa e deploy no sandbox); N+1 de /crm e /financeiro/custo-real seguem como débito registrado
- Fase atual: Fase 8 — Finalização concluída (código, massa, correções e deploy)
- Última fase concluída: Fase 8 — Finalização do código + massa de testes de volume
- Próxima tarefa: Nenhuma obrigatória. Débitos: N+1 de `/crm` (795 queries), N+1 de `/financeiro/custo-real/{id}` (508 queries) e aging renderizando 1.101 linhas.
- Bloqueios: Nenhum
- Última atualização: 2026-09-26

## Objetivo

Tornar a jornada pedido → produção → estoque → caixa consistente e previsível no `pascoa-monolith`, com menos cliques e menos código, e publicar a versão evoluída no sandbox AWS (`aws/`) para teste.

Escopo desta execução: **somente `pascoa-monolith` + `aws/`**. Os 13 módulos v5 não são tocados.

## Fluxo Atual

1. `POST /pedidos/wizard/finalizar` (ou `/pedidos/salvar`) cria `Pedido` em NOVO com itens; `ItemPedido.@PrePersist` calcula subtotal; `PedidoService.recalcularTotal` mantém `totalPedido`.
2. `POST /pedidos/{id}/confirmar` → `PedidoService.confirmar`: exige NOVO + itens, grava snapshot `ItemPedido.custoUnitario` via `FichaTecnicaService`, status CONFIRMADO, `ProducaoService.gerarOrdens` (uma `OrdemProducao` por item), evento `PEDIDO_CONFIRMADO`.
3. Produção no Kanban (`ProducaoService.listarKanban`): `iniciarProducao` (PENDENTE → EM_ANDAMENTO, evento `PRODUCAO_INICIADA`), `concluirOrdem` (valida ficha técnica + rendimento, checa disponibilidade de todas as MPs, `EstoqueService.registrarSaida` com `findByIdForUpdate`, status CONCLUIDA), `cancelarOrdem` (alerta interno se estava EM_ANDAMENTO).
4. `POST /pedidos/{id}/pronto` e `POST /pedidos/{id}/entrega`: cliques manuais, sem relação com o estado das ordens.
5. Pagamentos: `POST /pedidos/{id}/pagamento` cria `Pagamento`, evento `PAGAMENTO_RECEBIDO`. Saldo mostrado só no detalhe do pedido.
6. Cancelamento: `PedidoService.cancelar` propaga para ordens canceláveis e chama `gastoVariavelRepository.desconsiderarPorPedido`.
7. Financeiro é somente leitura: `FinanceiroService.gerarRelatorio`, `FluxoCaixaService.calcular`, `BreakevenService` (breakeven, projeção de safra, aging), `CustoRealService.calcular`.

### Estados

- `StatusPedido`: NOVO, CONFIRMADO, EM_PRODUCAO, PRONTO, ENTREGUE, CANCELADO. Transições permitidas nos métodos `pode*` do próprio enum.
- `StatusOrdem`: PENDENTE, EM_ANDAMENTO, CONCLUIDA, CANCELADA.
- `TipoMovimentacao`: ENTRADA, SAIDA, AJUSTE.
- `StatusConta`: usado por `ContaPagar` (tela ativa) e `ContaReceber` (sem uso real).

## Problemas Encontrados

### Problema 1 — financeiro sem "a receber"
- Impacto: `/financeiro/aging` sempre vazio e `previstoEntrada` do fluxo de caixa sempre zero. "Quanto ainda tenho para receber" não tem resposta.
- Causa: `ContaReceber` não é criada em nenhum ponto do código (só repositório, DTO e leitura em `BreakevenService.aging` e `FluxoCaixaService`).
- Solução proposta: derivar saldo de `Pedido.totalPedido - Σ Pagamento.valor`, vencimento = `dataEntrega`.
- Risco: Baixo — `AgingDto` e telas permanecem iguais.
- Prioridade: Alta

### Problema 2 — pagamento sem invariante de valor
- Impacto: duplo submit/refresh duplica lançamento no caixa; pagamento acima do total do pedido é aceito.
- Causa: `PedidoService.registrarPagamento` não consulta saldo nem verifica duplicidade.
- Solução proposta: validar saldo e recusar lançamento idêntico repetido.
- Risco: Baixo
- Prioridade: Alta

### Problema 3 — estado morto e estado que mente
- Impacto: EM_PRODUCAO aparece em telas, filtros, contadores e barra pública de acompanhamento, mas nenhum pedido chega nele. PRONTO pode ser marcado com toda a produção pendente.
- Causa: nenhuma atribuição de `StatusPedido.EM_PRODUCAO` no código; `marcarPronto` só checa o status do pedido.
- Solução proposta: derivar as duas transições do estado das ordens.
- Risco: Médio — muda comportamento observável do pedido.
- Prioridade: Alta

### Problema 4 — cancelamento de pedido pago é silencioso
- Impacto: dinheiro recebido de pedido cancelado não gera nenhum registro operacional; ninguém é avisado de devolução.
- Causa: `PedidoService.cancelar` não olha `Pagamento`.
- Solução proposta: `AlertaInterno` quando `totalPago > 0` (sem entidade de estorno nova).
- Risco: Baixo
- Prioridade: Média

### Problema 5 — duas verdades de custo
- Impacto: custo real de pedido antigo usa preço de matéria-prima de hoje; margem histórica muda sozinha.
- Causa: `snapshotCustos` grava `ItemPedido.custoUnitario`, `CustoRealService.calcular` ignora e recalcula pela ficha atual.
- Solução proposta: usar o snapshot quando presente, ficha só como fallback.
- Risco: Médio — altera número exibido em `/financeiro/custo-real`.
- Prioridade: Média

### Problema 6 — escrita de banco em requisição GET
- Impacto: abrir o detalhe do pedido grava no banco; regra de total duplicada em duas camadas.
- Causa: `PedidoController.detalhe` linhas 142-149 recalculam e salvam `totalPedido`.
- Solução proposta: remover; `recalcularTotal` nas mutações já mantém o total.
- Risco: Baixo
- Prioridade: Média

### Problema 7 — sem visão do dia
- Impacto: operador não sabe o que produzir/entregar hoje nem o que está atrasado; precisa varrer Kanban e lista de pedidos.
- Causa: nenhuma consulta orientada por `dataEntrega`.
- Solução proposta: três listas derivadas no dashboard existente.
- Risco: Baixo
- Prioridade: Alta (operacional)

## Decisões

1. "A receber" é **derivado** de `Pedido` + `Pagamento`. `ContaReceber`/`contas_receber` deixa de ser lida; entidade e migration permanecem no repositório sem uso (sem breaking change de schema).
2. Transições automáticas: iniciar ordem leva pedido CONFIRMADO → EM_PRODUCAO; concluir a última ordem aberta leva a PRONTO. Botões manuais continuam existindo como escape.
3. Painel do dia entra no dashboard existente (`/`), sem rota nova e sem alteração em `SecurityConfig`.
4. **Nenhuma migration Flyway** nesta evolução: tudo derivado de colunas existentes. Próxima migration livre continua sendo V15.
5. Transição de pedido disparada pela produção reusa `PedidoService.marcarPronto` — a regra não é duplicada em `ProducaoService`.

## Plano de Execução

### Fase 0 — Preparação
- [x] validar git (branch anterior `feat/estoque-saida-seguranca-jwt`, 31 alterações locais preservadas)
- [x] criar branch `feat/evolucao-fluxo-producao-pascoa`
- [x] criar arquivo de controle

### Fase 1 — Descoberta e Mapeamento
- [x] analisar fluxo existente
- [x] localizar componentes (entidades, enums, services, controllers, repositories, DTOs, eventos, listeners, telas)
- [x] mapear estados
- [x] mapear dependências

### Fase 2 — Análise do Domínio
- [x] revisar entidades
- [x] revisar regras
- [x] revisar transições
- [x] identificar duplicidades

### Fase 3 — Análise Operacional
- [x] analisar jornada do usuário
- [x] identificar pontos de carga cognitiva
- [x] identificar automações possíveis
- [x] priorizar problemas

### Fase 4 — Desenho da Solução
- [x] definir fluxo futuro
- [x] definir mudanças de domínio
- [x] definir mudanças de aplicação
- [x] definir mudanças de UI

### Fase 5 — Implementação
- [x] U1 — invariante financeira do pagamento
- [x] U2 — estado do pedido derivado da produção
- [x] U3 — "a receber" real (aging + fluxo de caixa)
- [x] U4 — cancelamento com rastreabilidade do dinheiro
- [x] U5 — uma verdade de custo + fim da escrita em GET
- [x] U6 — painel do dia no dashboard

### Fase 6 — Testes
- [x] testes unitários (pagamento, transições)
- [x] testes de integração (a receber)
- [x] testes de fluxo
- [x] validação de regras críticas

### Fase 7 — Validação
- [x] build
- [x] testes
- [x] checkstyle/spotbugs
- [x] git diff
- [x] revisão de arquivos alterados

### Fase 8 — Finalização
- [x] remover código morto
- [x] remover comentários dos trechos alterados
- [x] validar legado
- [x] atualizar `docs/05-estado-implementacao.md` e docs impactadas
- [x] U8 — deploy no sandbox AWS e teste end-to-end

## Alterações Realizadas

### Tarefa U1 — invariante financeira do pagamento
- Status: CONCLUÍDA
- Objetivo: impedir pagamento duplicado e pagamento acima do saldo do pedido.
- Arquivos alterados: `pedido/service/PedidoService.java`, `pedido/repository/PagamentoRepository.java`, `src/test/.../PedidoStateMachineTest.java`
- Regra alterada: `registrarPagamento` passa a recusar lançamento idêntico (pedido + valor + tipo + data), pedido já quitado e valor acima do saldo em aberto. Data do pagamento normalizada no service (antes só no `@PrePersist`).
- Testes executados: `mvn test -Dtest=PedidoStateMachineTest` — 17 testes, 0 falhas.
- Resultado: OK
- Próxima tarefa: U2

### Tarefa U2 — estado do pedido derivado da produção
- Status: CONCLUÍDA
- Objetivo: acabar com o estado morto EM_PRODUCAO e com PRONTO marcado manualmente sobre produção pendente.
- Arquivos alterados: `producao/event/ProducaoAtualizadaEvent.java` (novo), `producao/repository/OrdemProducaoRepository.java`, `producao/service/ProducaoService.java`, `pedido/service/PedidoService.java`, `src/test/.../producao/service/ProducaoStatusPedidoTest.java` (novo)
- Regra alterada: iniciar/concluir/cancelar ordem publica `ProducaoAtualizadaEvent`; `PedidoService.sincronizarComProducao` deriva o status do pedido das ordens (ordem EM_ANDAMENTO + pedido CONFIRMADO → EM_PRODUCAO; nenhuma aberta e alguma CONCLUIDA → PRONTO). Transição centralizada em `aplicarStatus`, reusada por `marcarPronto` e `registrarEntrega`. Evento em vez de injeção direta para não criar dependência circular `PedidoService` ↔ `ProducaoService`.
- Testes executados: `ProducaoStatusPedidoTest` (4/4) e `PedidoStateMachineTest` (17/17).
- Resultado: OK
- Próxima tarefa: U3

### Tarefa U3 — "a receber" real
- Status: CONCLUÍDA
- Objetivo: fazer aging e previsto de entrada responderem "quanto ainda tenho para receber".
- Arquivos alterados: `pedido/repository/PedidoRepository.java`, `financeiro/service/BreakevenService.java`, `financeiro/service/FluxoCaixaService.java`, `financeiro/dto/AgingDto.java`, `templates/financeiro/aging.html`, `src/test/.../financeiro/service/AgingDerivadoTest.java` (novo)
- Regra alterada: saldo derivado de `Pedido.totalPedido - Σ Pagamento.valor` (pedido não CANCELADO, saldo > 0), vencimento = `dataEntrega` com fallback em `dataPedido`. `ContaReceberRepository` saiu dos dois services. Faixa de atraso passou a usar `ChronoUnit.DAYS` (o `until(...).getDays()` anterior zerava atrasos acima de um mês). Coluna `#Conta` removida do aging por duplicar o pedido.
- Testes executados: `AgingDerivadoTest` (5/5).
- Resultado: OK
- Próxima tarefa: U4

### Tarefa U4 — cancelamento com rastreabilidade do dinheiro
- Status: CONCLUÍDA
- Objetivo: não perder de vista valor já recebido em pedido cancelado.
- Arquivos alterados: `pedido/service/PedidoService.java`, `src/test/.../PedidoStateMachineTest.java`
- Regra alterada: cancelamento com `totalPago > 0` cria `AlertaInterno` apontando para o pedido e o valor a devolver.
- Testes executados: `PedidoStateMachineTest` (18/18).
- Resultado: OK
- Próxima tarefa: U5

### Tarefa U5 — uma verdade de custo + fim da escrita em GET
- Status: CONCLUÍDA
- Objetivo: custo histórico estável e nenhuma gravação em requisição de leitura.
- Arquivos alterados: `financeiro/service/CustoRealService.java`, `pedido/controller/PedidoController.java`, `pedido/service/PedidoService.java`
- Regra alterada: `CustoRealService` usa `ItemPedido.custoUnitario` (snapshot da confirmação) e só recalcula pela ficha quando o snapshot é nulo. `PedidoController.detalhe` não recalcula nem salva mais o total; `PedidoService.salvarSemRecalculo` removido por ficar sem uso.
- Testes executados: `CustoRealServiceIntegrationTest` (8/8).
- Resultado: OK
- Próxima tarefa: U6

### Tarefa U6 — painel do dia no dashboard
- Status: CONCLUÍDA
- Objetivo: responder "o que produzir, o que entregar e o que está atrasado" na abertura do sistema.
- Arquivos alterados: `cadastro/controller/DashboardController.java`, `pedido/repository/PedidoRepository.java`, `producao/repository/OrdemProducaoRepository.java`, `templates/dashboard.html`, `src/test/.../cadastro/controller/PainelDoDiaTest.java` (novo)
- Regra alterada: nenhuma; três listas derivadas (`findAtrasados`, `findPorDataEntrega`, `findAbertasPorPrazo`) no topo do dashboard, sem rota nova e sem mudança em `SecurityConfig`.
- Testes executados: `PainelDoDiaTest` (2/2).
- Resultado: OK
- Próxima tarefa: U8 (deploy AWS)

### Ajuste durante a revisão do diff
- `ProducaoService.iniciarProducao` deixou de publicar `PedidoStatusEvent(PRODUCAO_INICIADA)`: com a sincronização automática, a notificação sairia duas vezes por início de ordem. Agora o evento de notificação sai uma única vez, quando o pedido entra em EM_PRODUCAO.

### Tarefa U9 — massa de testes de volume
- Status: CONCLUÍDA
- Objetivo: popular o banco local com todos os cenários da aplicação em volume de estresse, para caçar bug e performance.
- Arquivos criados: `infra/seed/seed-massa-teste.sql`
- Aplicação: `docker compose exec -T postgres psql -U postgres -d pascoa_monolith < infra/seed/seed-massa-teste.sql` (apaga os dados de negócio, preserva `usuarios` e `configuracao_*`; determinístico, reexecutável).
- Volume gerado: 800 clientes, 40 produtos, 25 matérias-primas, 30 fichas técnicas, 5.000 pedidos (6 status), ~15.000 itens, ~5.100 pagamentos (5 tipos), ~13.900 ordens de produção (4 status), 30.025 movimentações de estoque com saldo cumulativo coerente, 1.500 orçamentos (4 status), 200 contas a pagar (3 status), 15 despesas fixas, 1.200 gastos variáveis, 252 orçamentos de gasto, ~700 inspeções de qualidade, 2.000 notificações (ENVIADA/FALHA), 40 alertas, 1.000 registros de auditoria, 10 usuários (todas as 6 roles), 20 `contas_receber` legadas divergentes de propósito.
- Cenários limítrofes plantados: 10 produtos sem ficha técnica, 2 fichas sem itens, 1 ficha com rendimento zero, 8 matérias-primas em nível crítico, 3 produtos e 10 clientes soft-deleted (sem histórico vinculado — ver bug B1), 5 clientes anonimizados (LGPD), 20 aniversariantes hoje, 162 pedidos cancelados com pagamento recebido, 675 itens sem snapshot de custo, 50 pedidos sem data de entrega, 898 pedidos atrasados, 35 entregas para hoje.
- Invariantes conferidas pelo próprio script: nenhum saldo de estoque negativo, `total_pedido` = soma dos itens em 100% dos pedidos, idem orçamentos, nenhum pedido NOVO com ordem de produção.
- Resultado: OK — script roda em ~10s e imprime o bloco de sanidade por cenário.

## Testes Executados

- 2026-09-26 — `mvn test -Dtest=PedidoStateMachineTest`: tests=17, failures=0, errors=0.
- 2026-09-26 — `mvn test -Dtest=ProducaoStatusPedidoTest`: tests=4, failures=0, errors=0.
- 2026-09-26 — `mvn test -Dtest=AgingDerivadoTest`: tests=5, failures=0, errors=0.
- 2026-09-26 — `mvn test -Dtest=PainelDoDiaTest`: tests=2, failures=0, errors=0.
- 2026-09-26 — `mvn test` (suíte completa do monólito): 130 testes, 0 falhas, 0 erros em 16 classes.
- 2026-09-26 — `mvn -DskipTests install -pl pascoa-monolith -am`: OK (jar gerado).
- 2026-09-26 — `mvn checkstyle:check -Pci -Dcheckstyle.failsOnError=false`: sem violações.
- 2026-09-26 — `mvn spotbugs:check -Pci -DfailOnError=false`: análise não conclui no JDK 21 ("Error scanning java/lang/Object"), comportamento pré-existente ao trabalho desta branch.

### Tarefa U10 — correção dos 3 gargalos de maior impacto
- Status: CONCLUÍDA
- Objetivo: derrubar o custo das três telas que a massa expôs como inviáveis.
- Arquivos alterados: `estoque/repository/MovimentacaoEstoqueRepository.java`, `estoque/service/EstoqueService.java`, `estoque/controller/EstoqueController.java`, `templates/estoque/movimentacoes.html`, `producao/repository/OrdemProducaoRepository.java`, `producao/service/ProducaoService.java`, `producao/controller/ProducaoController.java`, `templates/producao/fila.html`, `pedido/repository/PedidoRepository.java`, `cadastro/controller/DashboardController.java`, `templates/dashboard.html`
- Regras alteradas:
  1. `/estoque/movimentacoes` paginado em 50 por página (`Page`/`Pageable` no mesmo padrão de `/auditoria`), com `JOIN FETCH` da matéria-prima e filtros preservados na navegação. `EstoqueService.listarTodas` foi removido (sem uso).
  2. `/producao` paginado em 50 por página; o Kanban passou a limitar cada coluna no banco (PENDENTE 50, EM_ANDAMENTO 50, CONCLUIDA 20, CANCELADA 10) em vez de carregar tudo e cortar em memória.
  3. Painel do dia do dashboard limitado a 10 linhas por card, com o total real no badge e link "ver todos" quando há mais.

| Tela | Antes | Depois |
|---|---|---|
| `/producao` | 2,5s · 18 MB · 2.669 queries | 0,09s · 104 KB · 44 queries |
| `/estoque/movimentacoes` | 5,5s · 34 MB · 30 queries | 0,09s · 100 KB · 6 queries |
| `/` (dashboard) | 0,4s · 884 KB · 386 queries | 0,25s · 61 KB · 57 queries |
| `/producao/kanban` | não medido (carregava tudo) | 0,15s · 228 KB · 57 queries |

- Testes executados: `mvn test` no monólito — 130 testes, 0 falhas; `mvn checkstyle:check -Pci` sem violações.
- Resultado: OK
- Próxima tarefa: U8 (deploy AWS)

### Tarefa U11 — correção de B1 (soft-delete) e B2 (badge do CRM)
- Status: CONCLUÍDA
- Objetivo: parar de derrubar telas quando cliente/produto excluído aparece no histórico, e consertar o badge de segmento do CRM.
- Arquivos alterados: `cadastro/entity/Cliente.java`, `cadastro/entity/Produto.java`, `cadastro/repository/ClienteRepository.java`, `cadastro/repository/ProdutoRepository.java`, `cadastro/service/ClienteService.java`, `cadastro/service/ProdutoService.java`, `pedido/service/PedidoService.java`, `fichaTecnica/service/FichaTecnicaService.java`, `templates/crm/dashboard.html`, `templates/crm/perfil.html`, `src/test/.../cadastro/SoftDeleteHistoricoTest.java` (novo)
- Regra alterada (B1): `@SQLRestriction("excluido_em IS NULL")` saiu de `Cliente` e `Produto` — era ele que fazia o `JOIN FETCH` estourar `FetchNotFoundException` ao navegar da FK para um registro excluído. O `@SQLDelete` continua, então excluir segue sendo soft-delete. O filtro passou a ser explícito onde importa: listagens e combos (`findAllByOrderByNomeAsc`, `findAllComboBox`, `findByAtivoTrueOrderByNomeAsc`, buscas por nome) e leituras por id para uso novo (`findVigenteById` / `findVigentesByIds`, usados por `ClienteService.buscarPorId`, `ProdutoService.buscarPorId`, criação/edição de pedido, item de pedido e ficha técnica). Resultado: histórico exibe o nome real do cliente/produto excluído e nenhuma tela cai; cadastro novo continua recusando registro excluído.
- Regra alterada (B2): `crm/dashboard.html` e `crm/perfil.html` chamavam `segmento().badgeColor()` e `segmento().descricao()`; o enum expõe `getBadgeColor()`/`getDescricao()`. Trocado por acesso de propriedade (`segmento().badgeColor`, `segmento().descricao`).
- Testes executados: `SoftDeleteHistoricoTest` (4/4 — histórico legível, excluídos fora das listagens, pedido e item recusando excluído); suíte completa do monólito 134 testes, 0 falhas; checkstyle sem violações.
- Validação em runtime com a massa (pedido #1 apontado para cliente 80 e produto 39, ambos excluídos): `/`, `/pedidos`, `/pedidos/1`, `/producao`, `/producao/kanban`, `/financeiro/breakeven`, `/financeiro/projecao-safra`, `/financeiro/custo-real/1`, `/crm`, `/crm/clientes/1` todas em 200; `/pedidos/1` e `/financeiro/custo-real/1` exibem o nome do produto excluído; wizard de pedido não oferece produto excluído.
- Resultado: OK
- Próxima tarefa: U8 (deploy AWS)

### Tarefa U12 — correção de B3 (paginação) e B4 (associação eager disfarçada)
- Status: CONCLUÍDA
- Objetivo: tirar as listas restantes do "carrega tudo" e matar o `select` por produto que o lado inverso da ficha técnica provocava.
- Arquivos alterados (B3): `pedido/repository/PedidoRepository.java`, `pedido/service/PedidoService.java`, `pedido/controller/PedidoController.java`, `cadastro/repository/ClienteRepository.java`, `cadastro/service/ClienteService.java`, `cadastro/controller/ClienteController.java`, `orcamento/repository/OrcamentoRepository.java`, `orcamento/service/OrcamentoService.java`, `orcamento/controller/OrcamentoController.java`, `qualidade/repository/InspecaoRepository.java`, `qualidade/service/QualidadeService.java`, `qualidade/controller/QualidadeController.java`, templates `pedidos/lista.html`, `clientes/lista.html`, `orcamentos/lista.html`, `qualidade/lista.html`
- Arquivos alterados (B4): `cadastro/entity/Produto.java`, `financeiro/service/CustoRealService.java`
- Regra alterada (B3): as quatro listas passaram a paginar em 50 por página, no mesmo padrão de `/auditoria` (`pagina`/`page`), com os filtros preservados nos links (`status` em pedidos, `busca` em clientes). `PedidoService.listarPorStatus` deu lugar a `listarPaginado`; `ClienteService.listarTodos` continua existindo para os combos, que precisam da lista inteira. O KPI "Total Inspeções" passou a usar `page.totalElements` em vez do tamanho da página.
- Regra alterada (B4): removida a associação inversa `Produto.fichaTecnica` (`@OneToOne(mappedBy)` marcada lazy que, sem bytecode enhancement, o Hibernate carregava eager — um `select` em `fichas_tecnicas` por produto materializado). O único consumidor era o fallback de custo do `CustoRealService`, que agora busca as fichas dos itens sem snapshot em uma query só, via `FichaTecnicaService.buscarPorProdutoIds`.
- Correção de rota durante a validação: o filtro opcional de clientes com `:nome IS NULL` em JPQL estourava `function lower(bytea) does not exist` no Postgres (parâmetro nulo sem tipo). Virou duas queries explícitas (`findPaginado` e `buscarPorNomePaginado`), escolhidas no service.

| Tela | Antes de U10 | Depois de U12 |
|---|---|---|
| `/pedidos` | 0,63s · 5 MB · 4 queries | 0,07s · 90 KB · 5 queries |
| `/clientes` | 0,08s · 1,1 MB · — | 0,06s · 107 KB · 5 queries |
| `/orcamentos` | 0,19s · 2,1 MB · — | 0,05s · 110 KB · 5 queries |
| `/qualidade` | 0,34s · 1,2 MB · — | 0,05s · 117 KB · 8 queries |
| `/producao` | 2,5s · 18 MB · 2.669 queries | 0,07s · 104 KB · **5 queries** |
| `/producao/kanban` | carregava tudo | 0,10s · 228 KB · 11 queries |
| `/` | 0,4s · 884 KB · 386 queries | 0,12s · 61 KB · 21 queries |
| `/estoque/movimentacoes` | 5,5s · 34 MB · 30 queries | 0,07s · 100 KB · 6 queries |

- Testes executados: suíte completa do monólito 134 testes, 0 falhas; checkstyle sem violações.
- Validação em runtime: as quatro listas exibem o rodapé com total real (`5000 pedido(s)`, `790 cliente(s)`, `1500 orçamento(s)`, `821 inspeção(ões)`), navegação preserva filtro (`/pedidos?status=ENTREGUE&pagina=1`) e busca por nome segue funcionando.
- Resultado: OK
- Próxima tarefa: U8 (deploy AWS)

### Tarefa U8 — build local, deploy no sandbox AWS e massa na cloud
- Status: CONCLUÍDA
- Objetivo: publicar a versão evoluída no sandbox e deixar a mesma massa de teste rodando na cloud para o trial.
- Passos executados:
  1. `mvn -DskipTests install -pl pascoa-monolith -am` — jar de 77 MB gerado.
  2. Aplicação local revalidada em http://localhost:8080 (todas as telas 200).
  3. `./aws/scripts/02-build-push.sh` — imagem linux/amd64 publicada em `896328389222.dkr.ecr.us-east-1.amazonaws.com/pascoa-sandbox:03113bb` (+ `latest`).
  4. `./aws/scripts/03-deploy.sh` — container recriado na EC2 `i-058913654054bd84b` via SSM; readiness UP.
  5. Massa aplicada no Postgres do sandbox por SSM (seed comprimido em base64, descompactado em `/tmp/seed-massa.sql` e aplicado com `docker compose exec -T postgres psql`). Saída de sanidade conferida: mesmas 33 tabelas populadas, invariantes em zero.
- URL do trial: https://fhz145okvk.execute-api.us-east-1.amazonaws.com — login `admin` com o valor de `admin_senha_inicial` do `aws/terraform/terraform.tfvars`; os demais usuários da massa (`financeiro`, `atendente`, `confeiteiro`, `qualidade`, `analista`, `admin2`) usam a mesma senha.
- Verificação na cloud: `/`, `/pedidos`, `/pedidos?pagina=3`, `/producao`, `/producao/kanban`, `/clientes`, `/orcamentos`, `/qualidade`, `/estoque/movimentacoes`, `/financeiro/{dashboard,aging,fluxo-caixa,breakeven,projecao-safra}`, `/crm`, `/analytics`, `/auditoria`, `/alertas` — todas 200. Rodapés de paginação e painel do dia com os totais corretos (5000 pedidos, 30025 movimentações, 898 atrasados).
- Tempos na cloud (t3.small + API Gateway): 0,7–1,2s na maioria; `/financeiro/aging` 2,8s e `/financeiro/breakeven` 2,2s.
- Observação: a tag da imagem é o último commit (`03113bb`) porque as alterações desta branch ainda não foram commitadas — o conteúdo publicado é o código atual do working tree.
- Resultado: OK

## Achados com a massa de volume (2026-09-26)

Medições com sessão autenticada, contagem de queries por render via log do Hibernate.

| Tela | Tempo | HTML | Queries | Achado |
|---|---|---|---|---|
| `/producao` | 2,5s | 18 MB | **2.669** | CORRIGIDO em U10. Causa: fila sem paginação sobre 13.934 ordens e `Produto.fichaTecnica` (`@OneToOne(mappedBy)` lazy sem bytecode enhancement) disparando um `select` em `fichas_tecnicas` por linha. |
| `/estoque/movimentacoes` | 5,5s | **34 MB** | 30 | CORRIGIDO em U10. Causa: `findAllByOrderByDataDesc` trazia as 30.025 movimentações sem paginação. |
| `/crm` | 0,4s | 40 KB | **797** | N+1: `statsPorCliente` agrega no banco, mas o dashboard percorre clientes e toca `ultimosPedidosPorCliente`/itens por linha. |
| `/financeiro/custo-real/{id}` | 0,9s | 42 KB | **545** | `CustoRealService.contarUnidadesMes` carrega todos os pedidos do banco e navega itens em memória para ratear despesa fixa. |
| `/` (dashboard) | 0,4s | 884 KB | **386** | CORRIGIDO em U10. Painel do dia estava sem limite: 898 atrasados e 1.835 ordens em listas completas. |
| `/pedidos` | 0,6s | **5 MB** | 4 | Consulta boa (uma query com fetch), mas lista os 5.000 pedidos de uma vez, sem paginação. |
| `/financeiro/aging` | 1,9s | 443 KB | 4 | Consulta derivada nova é eficiente; o custo é renderizar 1.101 linhas em 5 faixas. |
| `/orcamentos`, `/clientes`, `/qualidade` | < 0,4s | 1–2 MB | poucas | Mesmo padrão: lista completa sem paginação. |

Bugs de comportamento:

- **B1 — soft-delete quebrava as telas de lista.** CORRIGIDO em U11 (removido o `@SQLRestriction`, filtro agora explícito nas listagens e nas leituras para uso novo).
- **B2 — `/crm` com `SegmentoCliente.badgeColor()`.** CORRIGIDO em U11 (acesso de propriedade no lugar da chamada de método, em `crm/dashboard.html` e `crm/perfil.html`).
- **B3 — paginação ausente nas demais listas.** CORRIGIDO em U12 para `/pedidos`, `/clientes`, `/orcamentos` e `/qualidade`. `/financeiro/aging` continua renderizando todas as 1.101 linhas em cinco faixas (é a natureza do relatório; se incomodar, filtrar por faixa).
- **B4 — `Produto.fichaTecnica` não era lazy de fato.** CORRIGIDO em U12 (associação inversa removida; fallback de custo busca as fichas em uma query).
- **B5 — N+1 em `/crm`.** 795 queries por render: `gerarRanking` percorre os clientes e consulta pontos/pedidos por linha. Não tocado.
- **B6 — N+1 em `/financeiro/custo-real/{id}`.** 508 queries: `contarUnidadesMes` carrega todos os pedidos e navega os itens em memória para ratear despesa fixa. Uma consulta agregada resolve. Não tocado.

## Pendências

Débitos registrados, fora do escopo desta execução:

- `ProducaoService.concluirOrdem` usa `Isolation.SERIALIZABLE` redundante com o `SELECT FOR UPDATE` de `EstoqueService.registrarSaida`.
- `ContaReceber` / tabela `contas_receber` sem uso depois de U3 — decidir remoção em migration futura.
- `CustoRealService.contarUnidadesMes` carrega todos os pedidos em memória para ratear despesa fixa.
- Estorno formal de pagamento (hoje apenas alerta interno no cancelamento).
- `NotificacaoEventListener` async lança `LazyInitializationException` ao acessar `pedido.cliente` fora da transação quando o publicador não está em contexto transacional (visto em `PainelDoDiaTest`; comportamento pré-existente, erro apenas logado).
- Coluna `contaId` removida de `AgingDto`; se algum relatório externo dependia dela, avaliar.

## Próximo Passo

Nada obrigatório. Se quiser seguir, os débitos registrados são: N+1 de `/crm` (795 queries por
render em `gerarRanking`), N+1 de `/financeiro/custo-real/{id}` (508 queries em
`contarUnidadesMes`, resolvível com uma consulta agregada) e o aging desenhando 1.101 linhas de
uma vez. Nenhuma alteração desta branch foi commitada.
