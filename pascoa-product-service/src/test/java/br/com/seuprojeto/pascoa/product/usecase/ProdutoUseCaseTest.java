package br.com.seuprojeto.pascoa.product.usecase;

import br.com.seuprojeto.pascoa.product.application.port.in.ProdutoUseCase.*;
import br.com.seuprojeto.pascoa.product.application.port.out.ProdutoEventPublisherPort;
import br.com.seuprojeto.pascoa.product.application.port.out.ProdutoRepositoryPort;
import br.com.seuprojeto.pascoa.product.application.usecase.ProdutoUseCaseImpl;
import br.com.seuprojeto.pascoa.product.domain.exception.PrecoInvalidoException;
import br.com.seuprojeto.pascoa.product.domain.exception.ProdutoNotFoundException;
import br.com.seuprojeto.pascoa.product.domain.model.Categoria;
import br.com.seuprojeto.pascoa.product.domain.model.Produto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProdutoUseCaseTest {

    @Mock ProdutoRepositoryPort repository;
    @Mock ProdutoEventPublisherPort eventPublisher;

    private ProdutoUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new ProdutoUseCaseImpl(repository, eventPublisher);
    }

    @Test
    @DisplayName("Criar produto salva e publica evento")
    void criarProduto() {
        when(repository.save(any())).thenAnswer(inv -> {
            Produto p = inv.getArgument(0);
            return p.withId(1L);
        });

        Produto criado = useCase.criar(new CriarProdutoCommand(
                "Ovo de Páscoa Trufado", "Delicioso", new BigDecimal("89.90"),
                Categoria.OVO_TRUFADO, null));

        assertThat(criado.getNome()).isEqualTo("Ovo de Páscoa Trufado");
        assertThat(criado.isDisponivel()).isTrue();
        assertThat(criado.isAtivo()).isTrue();
        assertThat(criado.temFoto()).isFalse();
        verify(eventPublisher).publishProdutoCriado(criado);
    }

    @Test
    @DisplayName("buscarPorId com id inexistente lança ProdutoNotFoundException")
    void buscarInexistente() {
        when(repository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> useCase.buscarPorId(99L))
                .isInstanceOf(ProdutoNotFoundException.class);
    }

    @Test
    @DisplayName("inativar seta ativo=false e disponivel=false")
    void inativar() {
        when(repository.findById(1L)).thenReturn(Optional.of(produtoBase()));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        useCase.inativar(1L);

        verify(repository).save(argThat(p -> !p.isAtivo() && !p.isDisponivel()));
        verify(eventPublisher).publishProdutoInativado(1L);
    }

    @Test
    @DisplayName("alterarPreco com valor zero lança PrecoInvalidoException")
    void precoInvalido() {
        Produto produto = produtoBase();
        assertThatThrownBy(() -> produto.alterarPreco(BigDecimal.ZERO))
                .isInstanceOf(PrecoInvalidoException.class);
    }

    @Test
    @DisplayName("alterarPreco com valor negativo lança PrecoInvalidoException")
    void precoNegativo() {
        Produto produto = produtoBase();
        assertThatThrownBy(() -> produto.alterarPreco(new BigDecimal("-1.00")))
                .isInstanceOf(PrecoInvalidoException.class);
    }

    @Test
    @DisplayName("atualizarFoto persiste nova URL")
    void atualizarFoto() {
        when(repository.findById(1L)).thenReturn(Optional.of(produtoBase()));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Produto atualizado = useCase.atualizarFoto(1L, "https://cdn.example.com/foto.jpg");

        assertThat(atualizado.getFotoUrl()).isEqualTo("https://cdn.example.com/foto.jpg");
        assertThat(atualizado.temFoto()).isTrue();
    }

    private Produto produtoBase() {
        return Produto.builder()
                .id(1L).nome("Ovo Gourmet").descricao("Artesanal")
                .preco(new BigDecimal("79.90")).categoria(Categoria.OVO_GOURMET)
                .disponivel(true).ativo(true).build();
    }
}
