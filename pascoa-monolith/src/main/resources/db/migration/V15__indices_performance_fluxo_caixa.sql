-- V15: Índices de performance para agregações de fluxo de caixa.
-- Substitui findAll() + filtro em memória por SUM() no DB com seek por data.
-- Refs: FluxoCaixaService.calcular(), PagamentoRepository.sumValorByPeriodo(),
--       MovimentacaoEstoqueRepository.sumCustoByTipoEPeriodo().

CREATE INDEX IF NOT EXISTS idx_pagamento_data_pagamento
    ON pagamentos (data_pagamento);

-- Composto: filtro por tipo + range de data (ENTRADA no período do fluxo de caixa).
CREATE INDEX IF NOT EXISTS idx_movimentacao_estoque_tipo_data
    ON movimentacoes_estoque (tipo, data);
