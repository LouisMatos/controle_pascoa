package br.com.seuprojeto.pascoa.analytics.domain.model;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Getter
@Builder
public class RankingProduto {

    private final int posicao;
    private final Long produtoId;
    private final String nomeProduto;
    private final long quantidadeVendida;
    private final BigDecimal receita;

    public static List<RankingProduto> de(List<RegistroVenda> registros, int limite) {
        record Agregado(Long produtoId, String nome, long qtd, BigDecimal receita) {}

        Map<Long, List<RegistroVenda>> porProduto = registros.stream()
                .collect(Collectors.groupingBy(RegistroVenda::getProdutoId));

        List<Agregado> agregados = porProduto.entrySet().stream()
                .map(e -> {
                    List<RegistroVenda> itens = e.getValue();
                    long qtd = itens.stream().mapToLong(RegistroVenda::getQuantidade).sum();
                    BigDecimal rec = itens.stream()
                            .map(RegistroVenda::getValorTotal)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    String nome = itens.get(0).getNomeProduto();
                    return new Agregado(e.getKey(), nome, qtd, rec);
                })
                .sorted(Comparator.comparingLong(Agregado::qtd).reversed())
                .limit(limite)
                .toList();

        return IntStream.range(0, agregados.size())
                .mapToObj(i -> {
                    Agregado a = agregados.get(i);
                    return RankingProduto.builder()
                            .posicao(i + 1)
                            .produtoId(a.produtoId())
                            .nomeProduto(a.nome())
                            .quantidadeVendida(a.qtd())
                            .receita(a.receita())
                            .build();
                })
                .toList();
    }
}
