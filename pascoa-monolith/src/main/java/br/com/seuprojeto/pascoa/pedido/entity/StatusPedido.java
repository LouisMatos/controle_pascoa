package br.com.seuprojeto.pascoa.pedido.entity;

public enum StatusPedido {
    NOVO("Novo", "bg-primary"),
    CONFIRMADO("Confirmado", "bg-info text-dark"),
    EM_PRODUCAO("Em Produção", "bg-warning text-dark"),
    PRONTO("Pronto", "bg-success"),
    ENTREGUE("Entregue", "bg-dark"),
    CANCELADO("Cancelado", "bg-danger");

    private final String descricao;
    private final String badgeCss;

    StatusPedido(String descricao, String badgeCss) {
        this.descricao = descricao;
        this.badgeCss = badgeCss;
    }

    public String getDescricao() {
        return descricao;
    }

    public String getBadgeCss() {
        return badgeCss;
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
