package br.com.seuprojeto.pascoa.production.domain.model;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ItemOrdem {
    private final Long produtoId;
    private final String nomeProduto;
    private final int quantidade;
}
