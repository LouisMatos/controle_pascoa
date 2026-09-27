package br.com.seuprojeto.pascoa.customer.application.port.in;

import br.com.seuprojeto.pascoa.customer.domain.model.Cliente;
import br.com.seuprojeto.pascoa.customer.domain.model.PreferenciaCanal;

import java.time.LocalDate;
import java.util.List;

public interface ClienteUseCase {

    record CriarClienteCommand(
            String nome,
            String email,
            String telefone,
            String cpf,
            LocalDate dataNascimento,
            String enderecoEntrega,
            PreferenciaCanal preferenciaCanal
    ) {}

    record AtualizarClienteCommand(
            Long id,
            String nome,
            String email,
            String telefone,
            String enderecoEntrega,
            PreferenciaCanal preferenciaCanal
    ) {}

    Cliente criar(CriarClienteCommand command);

    Cliente buscarPorId(Long id);

    List<Cliente> listar();

    Cliente atualizar(AtualizarClienteCommand command);

    void inativar(Long id);

    Cliente adicionarPontos(Long id, int pontos);
}
