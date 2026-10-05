package br.com.seuprojeto.pascoa.estoque.entity;

public enum TipoMovimentacao {
    ENTRADA("Entrada", "bg-success"),
    SAIDA("Saída", "bg-danger"),
    AJUSTE("Ajuste", "bg-warning text-dark");

    private final String descricao;
    private final String badgeCss;

    TipoMovimentacao(String descricao, String badgeCss) {
        this.descricao = descricao;
        this.badgeCss = badgeCss;
    }

    public String getDescricao() { return descricao; }
    public String getBadgeCss()  { return badgeCss;  }
}
