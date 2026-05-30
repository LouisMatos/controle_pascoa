package br.com.seuprojeto.pascoa.customer.adapter.in.rest.dto;

import br.com.seuprojeto.pascoa.customer.domain.model.Cliente;
import br.com.seuprojeto.pascoa.customer.domain.model.PreferenciaCanal;

import java.time.LocalDate;

public record ClienteResponse(
        Long id,
        String nome,
        String email,
        String telefone,
        String cpf,
        LocalDate dataNascimento,
        String enderecoEntrega,
        PreferenciaCanal preferenciaCanal,
        int pontosFidelidade,
        boolean ativo
) {
    public static ClienteResponse from(Cliente c) {
        return new ClienteResponse(
                c.getId(), c.getNome(), c.getEmail(), c.getTelefone(),
                c.getCpf(), c.getDataNascimento(), c.getEnderecoEntrega(),
                c.getPreferenciaCanal(), c.getPontosFidelidade(), c.isAtivo()
        );
    }
}
