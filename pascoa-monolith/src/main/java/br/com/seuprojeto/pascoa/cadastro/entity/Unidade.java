package br.com.seuprojeto.pascoa.cadastro.entity;

public enum Unidade {
    KG("kg"),
    G("g"),
    L("L"),
    ML("mL"),
    UN("un"),
    CX("cx");

    private final String simbolo;

    Unidade(String simbolo) {
        this.simbolo = simbolo;
    }

    public String getSimbolo() {
        return simbolo;
    }
}
