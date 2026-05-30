package br.com.seuprojeto.pascoa.pedido.entity;

public enum StatusPedido {
    NOVO("Novo"),
    CONFIRMADO("Confirmado"),
    EM_PRODUCAO("Em Produção"),
    PRONTO("Pronto"),
    ENTREGUE("Entregue"),
    CANCELADO("Cancelado");

    private final String descricao;

    StatusPedido(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }

    public boolean podeConfirmar() {
        return this == NOVO;
    }

    public boolean podeCancelar() {
        return this != ENTREGUE && this != CANCELADO;
    }

    public boolean podePronto() {
        return this == CONFIRMADO || this == EM_PRODUCAO;
    }

    public boolean podeEntregar() {
        return this == PRONTO;
    }

    public boolean podeAdicionarItens() {
        return this == NOVO;
    }

    public boolean podeAdicionarPagamento() {
        return this != NOVO && this != CANCELADO;
    }
}
