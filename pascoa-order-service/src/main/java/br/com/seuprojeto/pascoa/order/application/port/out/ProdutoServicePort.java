package br.com.seuprojeto.pascoa.order.application.port.out;

import java.math.BigDecimal;
import java.util.Optional;

public interface ProdutoServicePort {
    record ProdutoInfo(Long id, String nome, BigDecimal preco, boolean disponivel) {}
    Optional<ProdutoInfo> findById(Long produtoId);
}
