package br.com.seuprojeto.pascoa.gastos.entity;

public enum CategoriaGasto {
    EMBALAGEM("Embalagem"),
    TRANSPORTE("Transporte"),
    MARKETING("Marketing"),
    MATERIA_PRIMA("Matéria-Prima"),
    EQUIPAMENTO("Equipamento"),
    SERVICO("Serviço"),
    OUTROS("Outros");

    private final String descricao;

    CategoriaGasto(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
