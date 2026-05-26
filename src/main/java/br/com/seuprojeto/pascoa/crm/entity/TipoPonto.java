package br.com.seuprojeto.pascoa.crm.entity;

public enum TipoPonto {
    CREDITO("Crédito"),
    DEBITO("Débito");

    private final String descricao;

    TipoPonto(String descricao) { this.descricao = descricao; }
    public String getDescricao() { return descricao; }
}
