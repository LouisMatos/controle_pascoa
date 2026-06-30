package br.com.seuprojeto.pascoa.cadastro.dto;

/** Projeção mínima para combos/selects de cliente — evita hidratar a entidade completa. */
public record ClienteComboDto(Long id, String nome) {
}
