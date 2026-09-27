package br.com.seuprojeto.pascoa.customer.application.usecase;

import br.com.seuprojeto.pascoa.customer.application.port.in.ClienteUseCase;
import br.com.seuprojeto.pascoa.customer.application.port.out.ClienteEventPublisherPort;
import br.com.seuprojeto.pascoa.customer.application.port.out.ClienteRepositoryPort;
import br.com.seuprojeto.pascoa.customer.domain.exception.ClienteNotFoundException;
import br.com.seuprojeto.pascoa.customer.domain.exception.EmailDuplicadoException;
import br.com.seuprojeto.pascoa.customer.domain.model.Cliente;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class ClienteUseCaseImpl implements ClienteUseCase {

    private final ClienteRepositoryPort repository;
    private final ClienteEventPublisherPort eventPublisher;

    @Override
    public Cliente criar(CriarClienteCommand cmd) {
        if (repository.existsByEmail(cmd.email())) {
            throw new EmailDuplicadoException(cmd.email());
        }

        Cliente cliente = Cliente.builder()
                .nome(cmd.nome())
                .email(cmd.email())
                .telefone(cmd.telefone())
                .cpf(cmd.cpf())
                .dataNascimento(cmd.dataNascimento())
                .enderecoEntrega(cmd.enderecoEntrega())
                .preferenciaCanal(cmd.preferenciaCanal())
                .pontosFidelidade(0)
                .ativo(true)
                .build();

        Cliente salvo = repository.save(cliente);
        eventPublisher.publishClienteCriado(salvo);
        return salvo;
    }

    @Override
    @Transactional(readOnly = true)
    public Cliente buscarPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ClienteNotFoundException(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Cliente> listar() {
        return repository.findAllAtivos();
    }

    @Override
    public Cliente atualizar(AtualizarClienteCommand cmd) {
        Cliente existente = repository.findById(cmd.id())
                .orElseThrow(() -> new ClienteNotFoundException(cmd.id()));

        if (!existente.getEmail().equals(cmd.email())
                && repository.existsByEmail(cmd.email())) {
            throw new EmailDuplicadoException(cmd.email());
        }

        Cliente atualizado = existente
                .withNome(cmd.nome())
                .withEmail(cmd.email())
                .withTelefone(cmd.telefone())
                .withEnderecoEntrega(cmd.enderecoEntrega())
                .withPreferenciaCanal(cmd.preferenciaCanal());

        Cliente salvo = repository.save(atualizado);
        eventPublisher.publishClienteAtualizado(salvo);
        return salvo;
    }

    @Override
    public void inativar(Long id) {
        Cliente cliente = repository.findById(id)
                .orElseThrow(() -> new ClienteNotFoundException(id));
        repository.save(cliente.inativar());
        eventPublisher.publishClienteInativado(id);
    }

    @Override
    public Cliente adicionarPontos(Long id, int pontos) {
        Cliente cliente = repository.findById(id)
                .orElseThrow(() -> new ClienteNotFoundException(id));
        return repository.save(cliente.adicionarPontos(pontos));
    }
}
