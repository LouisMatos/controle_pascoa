package br.com.seuprojeto.pascoa.cadastro.entity;

public enum UnidadeVenda {
    UNIDADE("Unidade", "un", false),
    DUZIA("Dúzia", "dz", false),
    CENTO("Cento", "cento", false),
    PACOTE("Pacote", "pct", false),
    KG("Quilo", "kg", true);

    private final String descricao;
    private final String simbolo;
    private final boolean fracionavel;

    UnidadeVenda(String descricao, String simbolo, boolean fracionavel) {
        this.descricao = descricao;
        this.simbolo = simbolo;
        this.fracionavel = fracionavel;
    }

    public String getDescricao() {
        return descricao;
    }

    public String getSimbolo() {
        return simbolo;
    }

    public boolean isFracionavel() {
        return fracionavel;
    }
}
