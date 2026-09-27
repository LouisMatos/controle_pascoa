package br.com.seuprojeto.pascoa.auth.domain.model;

public enum Role {
    ADMIN,
    FINANCEIRO,
    ATENDENTE,
    CONFEITEIRO,
    GESTOR_QUALIDADE,
    ANALISTA;

    public String authority() {
        return "ROLE_" + this.name();
    }
}
