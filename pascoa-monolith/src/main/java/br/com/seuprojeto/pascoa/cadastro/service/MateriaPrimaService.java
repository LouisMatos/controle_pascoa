package br.com.seuprojeto.pascoa.cadastro.service;

import br.com.seuprojeto.pascoa.cadastro.entity.MateriaPrima;
import br.com.seuprojeto.pascoa.cadastro.repository.MateriaPrimaRepository;
import br.com.seuprojeto.pascoa.shared.exception.RecursoNaoEncontradoException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MateriaPrimaService {

    private final MateriaPrimaRepository repository;

    @Transactional(readOnly = true)
    public List<MateriaPrima> listarTodas() {
        return repository.findAllByOrderByNomeAsc();
    }

    @Transactional(readOnly = true)
    public List<MateriaPrima> buscar(String nome) {
        return repository.findByNomeContainingIgnoreCaseOrderByNomeAsc(nome);
    }

    @Transactional(readOnly = true)
    public MateriaPrima buscarPorId(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Matéria-prima não encontrada: " + id));
    }

    @Transactional(readOnly = true)
    public List<MateriaPrima> listarComEstoqueCritico() {
        return repository.findComEstoqueCritico();
    }

    @Transactional
    public MateriaPrima salvar(MateriaPrima materiaPrima) {
        return repository.save(materiaPrima);
    }

    @Transactional
    public void excluir(Long id) {
        MateriaPrima mp = buscarPorId(id);
        repository.delete(mp);
    }
}
