package br.com.seuprojeto.pascoa.product.application.port.in;

import br.com.seuprojeto.pascoa.product.domain.model.Categoria;
import br.com.seuprojeto.pascoa.product.domain.model.Produto;

import java.math.BigDecimal;
import java.util.List;

public interface ProdutoUseCase {

    record CriarProdutoCommand(
            String nome,
            String descricao,
            BigDecimal preco,
            Categoria categoria,
            String fotoUrl
    ) {}

    record AtualizarProdutoCommand(
            Long id,
            String nome,
            String descricao,
            BigDecimal preco,
            Categoria categoria,
            String fotoUrl,
            boolean disponivel
    ) {}

    Produto criar(CriarProdutoCommand command);

    Produto buscarPorId(Long id);

    List<Produto> listar();

    List<Produto> listarDisponiveis();

    List<Produto> listarPorCategoria(Categoria categoria);

    Produto atualizar(AtualizarProdutoCommand command);

    Produto atualizarFoto(Long id, String fotoUrl);

    void inativar(Long id);
}
