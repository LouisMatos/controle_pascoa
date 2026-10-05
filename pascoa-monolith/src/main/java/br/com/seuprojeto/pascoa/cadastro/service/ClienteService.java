package br.com.seuprojeto.pascoa.cadastro.service;

import br.com.seuprojeto.pascoa.cadastro.entity.Cliente;
import br.com.seuprojeto.pascoa.cadastro.repository.ClienteRepository;
import br.com.seuprojeto.pascoa.shared.exception.RecursoNaoEncontradoException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ClienteService {

    private final ClienteRepository repository;

    @Transactional(readOnly = true)
    public List<Cliente> listarTodos() {
        return repository.findAllByOrderByNomeAsc();
    }

    @Transactional(readOnly = true)
    public List<Cliente> buscar(String nome) {
        return repository.findByNomeContainingIgnoreCaseOrderByNomeAsc(nome);
    }

    @Transactional(readOnly = true)
    public Page<Cliente> listarPaginado(String busca, int pagina) {
        PageRequest pageRequest = PageRequest.of(pagina, 50);
        return (busca != null && !busca.isBlank())
            ? repository.buscarPorNomePaginado(busca.trim(), pageRequest)
            : repository.findPaginado(pageRequest);
    }

    @Transactional(readOnly = true)
    public Cliente buscarPorId(Long id) {
        return repository.findVigenteById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente não encontrado: " + id));
    }

    @Transactional
    public Cliente salvar(Cliente cliente) {
        return repository.save(cliente);
    }

    @Transactional(readOnly = true)
    public boolean emailDuplicado(String email, Long id) {
        if (email == null || email.isBlank()) {
            return false;
        }
        return id == null
            ? repository.existsByEmailIgnoreCase(email)
            : repository.existsByEmailIgnoreCaseAndIdNot(email, id);
    }

    @Transactional
    public void excluir(Long id) {
        Cliente cliente = buscarPorId(id);
        repository.delete(cliente);
    }
}
