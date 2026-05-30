package br.com.seuprojeto.pascoa.producao.entity;

public enum StatusOrdem {

    PENDENTE("Pendente", "bg-secondary"),
    EM_ANDAMENTO("Em Andamento", "bg-primary"),
    CONCLUIDA("Concluída", "bg-success"),
    CANCELADA("Cancelada", "bg-danger");

    private final String descricao;
    private final String badgeCss;

    StatusOrdem(String descricao, String badgeCss) {
        this.descricao = descricao;
        this.badgeCss  = badgeCss;
    }

    public String getDescricao() { return descricao; }
    public String getBadgeCss()  { return badgeCss; }

    public boolean podeIniciar()  { return this == PENDENTE; }
    public boolean podeConcluir() { return this == PENDENTE || this == EM_ANDAMENTO; }
    public boolean podeCancelar() { return this != CONCLUIDA && this != CANCELADA; }
}
