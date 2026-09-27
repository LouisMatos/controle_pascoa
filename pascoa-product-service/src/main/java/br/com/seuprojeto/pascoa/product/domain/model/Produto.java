package br.com.seuprojeto.pascoa.product.domain.model;

import lombok.Builder;
import lombok.Getter;
import lombok.With;

import java.math.BigDecimal;

@Getter
@Builder
@With
public class Produto {

    private final Long id;
    private final String nome;
    private final String descricao;
    private final BigDecimal preco;
    private final Categoria categoria;
    private final String fotoUrl;
    private final boolean disponivel;
    private final boolean ativo;

    public boolean temFoto() {
        return fotoUrl != null && !fotoUrl.isBlank();
    }

    public Produto inativar() {
        return this.withAtivo(false).withDisponivel(false);
    }

    public Produto alterarPreco(BigDecimal novoPreco) {
        if (novoPreco == null || novoPreco.compareTo(BigDecimal.ZERO) <= 0) {
            throw new br.com.seuprojeto.pascoa.product.domain.exception.PrecoInvalidoException(novoPreco);
        }
        return this.withPreco(novoPreco);
    }
}
