package br.com.seuprojeto.pascoa.cadastro.service;

import br.com.seuprojeto.pascoa.cadastro.entity.Fornecedor;
import br.com.seuprojeto.pascoa.cadastro.repository.FornecedorRepository;
import br.com.seuprojeto.pascoa.shared.exception.RecursoNaoEncontradoException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FornecedorService {

    private final FornecedorRepository repository;

    @Transactional(readOnly = true)
    public List<Fornecedor> listarTodos() {
        return repository.findAllByOrderByNomeAsc();
    }

    @Transactional(readOnly = true)
    public List<Fornecedor> buscar(String nome) {
        return repository.findByNomeContainingIgnoreCaseOrderByNomeAsc(nome);
    }

    @Transactional(readOnly = true)
    public Fornecedor buscarPorId(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Fornecedor não encontrado: " + id));
    }

    @Transactional
    public Fornecedor salvar(Fornecedor fornecedor) {
        return repository.save(fornecedor);
    }

    @Transactional
    public void excluir(Long id) {
        Fornecedor fornecedor = buscarPorId(id);
        repository.delete(fornecedor);
    }
}
