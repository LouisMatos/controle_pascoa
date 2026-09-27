package br.com.seuprojeto.pascoa.crm.entity;

public enum SegmentoCliente {
    NOVO("Novo", "secondary"),
    REGULAR("Regular", "primary"),
    VIP("VIP", "warning"),
    INATIVO("Inativo", "danger");

    private final String descricao;
    private final String badgeColor;

    SegmentoCliente(String descricao, String badgeColor) {
        this.descricao = descricao;
        this.badgeColor = badgeColor;
    }

    public String getDescricao() { return descricao; }
    public String getBadgeColor() { return badgeColor; }
}
