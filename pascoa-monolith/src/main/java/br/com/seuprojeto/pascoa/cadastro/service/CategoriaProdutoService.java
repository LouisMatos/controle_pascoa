package br.com.seuprojeto.pascoa.cadastro.service;

import br.com.seuprojeto.pascoa.cadastro.entity.CategoriaProduto;
import br.com.seuprojeto.pascoa.cadastro.repository.CategoriaProdutoRepository;
import br.com.seuprojeto.pascoa.shared.exception.RecursoNaoEncontradoException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoriaProdutoService {

    private final CategoriaProdutoRepository repository;

    @Transactional(readOnly = true)
    public List<CategoriaProduto> listarAtivas() {
        return repository.findByAtivoTrueOrderByNomeAsc();
    }

    @Transactional(readOnly = true)
    public CategoriaProduto buscarPorId(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Categoria não encontrada: " + id));
    }

    @Transactional(readOnly = true)
    public List<CategoriaProduto> listarTodas() {
        return repository.findAllByOrderByNomeAsc();
    }

    @Transactional
    public CategoriaProduto salvar(CategoriaProduto form) {
        String nome = form.getNome().trim();
        boolean duplicada = form.getId() == null
            ? repository.existsByNomeIgnoreCase(nome)
            : repository.existsByNomeIgnoreCaseAndIdNot(nome, form.getId());
        if (duplicada) {
            throw new IllegalArgumentException("Já existe uma categoria com este nome.");
        }
        CategoriaProduto categoria = form.getId() == null ? new CategoriaProduto() : buscarPorId(form.getId());
        categoria.setNome(nome);
        return repository.save(categoria);
    }

    @Transactional
    public void alternarAtivo(Long id) {
        CategoriaProduto categoria = buscarPorId(id);
        categoria.setAtivo(!categoria.getAtivo());
        repository.save(categoria);
    }
}
