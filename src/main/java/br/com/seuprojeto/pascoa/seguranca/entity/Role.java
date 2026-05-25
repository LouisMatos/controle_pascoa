package br.com.seuprojeto.pascoa.seguranca.entity;

public enum Role {

    ADMIN("Administrador"),
    FINANCEIRO("Financeiro"),
    ATENDENTE("Atendente"),
    CONFEITEIRO("Confeiteiro");

    private final String descricao;

    Role(String descricao) { this.descricao = descricao; }

    public String getDescricao() { return descricao; }
}
