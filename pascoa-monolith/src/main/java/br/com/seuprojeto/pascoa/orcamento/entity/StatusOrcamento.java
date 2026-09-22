package br.com.seuprojeto.pascoa.orcamento.entity;

public enum StatusOrcamento {
    PENDENTE("Pendente", "bg-warning text-dark"),
    APROVADO("Aprovado", "bg-success"),
    RECUSADO("Recusado", "bg-danger"),
    EXPIRADO("Expirado", "bg-secondary");

    private final String descricao;
    private final String badgeCss;

    StatusOrcamento(String descricao, String badgeCss) {
        this.descricao = descricao;
        this.badgeCss = badgeCss;
    }

    public String getDescricao() {
        return descricao;
    }

    public String getBadgeCss() {
        return badgeCss;
    }
}
