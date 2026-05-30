package br.com.seuprojeto.pascoa.product.application.usecase;

import br.com.seuprojeto.pascoa.product.application.port.in.ProdutoUseCase;
import br.com.seuprojeto.pascoa.product.application.port.out.ProdutoEventPublisherPort;
import br.com.seuprojeto.pascoa.product.application.port.out.ProdutoRepositoryPort;
import br.com.seuprojeto.pascoa.product.domain.exception.ProdutoNotFoundException;
import br.com.seuprojeto.pascoa.product.domain.model.Categoria;
import br.com.seuprojeto.pascoa.product.domain.model.Produto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class ProdutoUseCaseImpl implements ProdutoUseCase {

    private final ProdutoRepositoryPort repository;
    private final ProdutoEventPublisherPort eventPublisher;

    @Override
    public Produto criar(CriarProdutoCommand cmd) {
        Produto produto = Produto.builder()
                .nome(cmd.nome())
                .descricao(cmd.descricao())
                .preco(cmd.preco())
                .categoria(cmd.categoria())
                .fotoUrl(cmd.fotoUrl())
                .disponivel(true)
                .ativo(true)
                .build();

        Produto salvo = repository.save(produto);
        eventPublisher.publishProdutoCriado(salvo);
        return salvo;
    }

    @Override
    @Transactional(readOnly = true)
    public Produto buscarPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ProdutoNotFoundException(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Produto> listar() {
        return repository.findAllAtivos();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Produto> listarDisponiveis() {
        return repository.findDisponiveis();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Produto> listarPorCategoria(Categoria categoria) {
        return repository.findByCategoria(categoria);
    }

    @Override
    public Produto atualizar(AtualizarProdutoCommand cmd) {
        Produto existente = repository.findById(cmd.id())
                .orElseThrow(() -> new ProdutoNotFoundException(cmd.id()));

        Produto atualizado = existente
                .withNome(cmd.nome())
                .withDescricao(cmd.descricao())
                .withCategoria(cmd.categoria())
                .withFotoUrl(cmd.fotoUrl())
                .withDisponivel(cmd.disponivel())
                .alterarPreco(cmd.preco());

        Produto salvo = repository.save(atualizado);
        eventPublisher.publishProdutoAtualizado(salvo);
        return salvo;
    }

    @Override
    public Produto atualizarFoto(Long id, String fotoUrl) {
        Produto existente = repository.findById(id)
                .orElseThrow(() -> new ProdutoNotFoundException(id));
        Produto atualizado = existente.withFotoUrl(fotoUrl);
        return repository.save(atualizado);
    }

    @Override
    public void inativar(Long id) {
        Produto produto = repository.findById(id)
                .orElseThrow(() -> new ProdutoNotFoundException(id));
        repository.save(produto.inativar());
        eventPublisher.publishProdutoInativado(id);
    }
}
