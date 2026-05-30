package br.com.seuprojeto.pascoa.cadastro.entity;

public enum Categoria {
    TRUFADO("Trufado"),
    RECHEADO("Recheado"),
    DIET("Diet"),
    VEGANO("Vegano"),
    TRADICIONAL("Tradicional"),
    ESPECIAL("Especial");

    private final String descricao;

    Categoria(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
