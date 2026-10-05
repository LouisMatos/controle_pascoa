package br.com.seuprojeto.pascoa.financial.domain.model;

public enum CategoriaLancamento {
    VENDA,       // receita de pedido entregue
    INSUMO,      // custo de matéria-prima
    PRODUCAO,    // custo de mão de obra / produção
    OPERACIONAL, // aluguel, energia, etc.
    OUTRO
}
