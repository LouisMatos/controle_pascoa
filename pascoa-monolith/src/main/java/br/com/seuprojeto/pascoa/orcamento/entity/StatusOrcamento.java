package br.com.seuprojeto.pascoa.orcamento.entity;

public enum StatusOrcamento {
    PENDENTE("Pendente"),
    APROVADO("Aprovado"),
    RECUSADO("Recusado"),
    EXPIRADO("Expirado");

    private final String descricao;

    StatusOrcamento(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
