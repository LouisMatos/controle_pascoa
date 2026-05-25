package br.com.seuprojeto.pascoa.financeiro.service;

import br.com.seuprojeto.pascoa.financeiro.dto.CustoRealDto;
import br.com.seuprojeto.pascoa.financeiro.repository.ConfiguracaoFinanceiraRepository;
import br.com.seuprojeto.pascoa.financeiro.repository.DespesaFixaRepository;
import br.com.seuprojeto.pascoa.financeiro.repository.DespesaVariavelRepository;
import br.com.seuprojeto.pascoa.pedido.entity.Pedido;
import br.com.seuprojeto.pascoa.pedido.entity.StatusPedido;
import br.com.seuprojeto.pascoa.pedido.repository.ItemPedidoRepository;
import br.com.seuprojeto.pascoa.pedido.repository.PedidoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustoRealService {

    private final PedidoRepository pedidoRepository;
    private final ItemPedidoRepository itemPedidoRepository;
    private final DespesaFixaRepository despesaFixaRepository;
    private final DespesaVariavelRepository despesaVariavelRepository;
    private final ConfiguracaoFinanceiraRepository configuracaoRepository;

    @Transactional(readOnly = true)
    public CustoRealDto calcular(Long pedidoId) {
        Pedido pedido = pedidoRepository.findByIdComItens(pedidoId)
            .orElseThrow(() -> new IllegalArgumentException("Pedido não encontrado: " + pedidoId));

        var config = configuracaoRepository.obter();

        // Camada 1: Custo de Matéria-Prima
        // Σ (FichaTecnicaItem.qtd × MP.custoMedioPonderado / rendimento) × qtdPedido
        var linhasMP = pedido.getItens().stream().map(item -> {
            var produto = item.getProduto();
            BigDecimal custoUnit = BigDecimal.ZERO;

            // produto pode ser null se foi excluído (soft-delete) após o pedido ser criado
            if (produto != null && produto.getFichaTecnica() != null) {
                var ft = produto.getFichaTecnica();
                BigDecimal custoTotalMP = ft.getItens().stream()
                    .map(fti -> {
                        BigDecimal cmp = fti.getMateriaPrima().getCustoMedioPonderado();
                        if (cmp == null || cmp.compareTo(BigDecimal.ZERO) == 0) {
                            cmp = fti.getMateriaPrima().getCustoUnitario();
                        }
                        return fti.getQuantidade().multiply(cmp);
                    })
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal rend = ft.getRendimento();
                if (rend.compareTo(BigDecimal.ZERO) > 0) {
                    custoUnit = custoTotalMP.divide(rend, 4, RoundingMode.HALF_UP);
                }
            }

            BigDecimal subtotal = custoUnit.multiply(BigDecimal.valueOf(item.getQuantidade()));
            return CustoRealDto.LinhaCustoMpDto.builder()
                .produto(produto != null ? produto.getNome() : "(produto removido)")
                .quantidade(item.getQuantidade())
                .custoUnitario(custoUnit.setScale(2, RoundingMode.HALF_UP))
                .subtotal(subtotal.setScale(2, RoundingMode.HALF_UP))
                .build();
        }).toList();

        BigDecimal custoMP = linhasMP.stream()
            .map(CustoRealDto.LinhaCustoMpDto::getSubtotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Camada 2: Rateio de Despesas Fixas
        BigDecimal totalFixoMensal = despesaFixaRepository.sumMensalAtivas();
        long totalUnidadesMes = contarUnidadesMes(pedido);
        BigDecimal rateioFixo = BigDecimal.ZERO;
        if (totalUnidadesMes > 0 && totalFixoMensal.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal rateioUnit = totalFixoMensal.divide(
                BigDecimal.valueOf(totalUnidadesMes), 4, RoundingMode.HALF_UP);
            long qtdPedido = pedido.getItens().stream()
                .mapToLong(i -> i.getQuantidade()).sum();
            rateioFixo = rateioUnit.multiply(BigDecimal.valueOf(qtdPedido))
                .setScale(2, RoundingMode.HALF_UP);
        }

        // Camada 3: Despesas Variáveis do pedido
        var despesasDetalhadas = despesaVariavelRepository.findByPedidoIdOrderByCategoria(pedidoId);
        BigDecimal despesasVariaveis = despesasDetalhadas.stream()
            .map(d -> d.getValor())
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Totais
        BigDecimal custoReal = custoMP.add(rateioFixo).add(despesasVariaveis)
            .setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = pedido.getTotalPedido();
        BigDecimal margemReal = total.compareTo(BigDecimal.ZERO) > 0
            ? total.subtract(custoReal).divide(total, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100)).setScale(1, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;

        // cliente pode estar inacessível se foi excluído após o pedido
        String clienteNome;
        try {
            clienteNome = pedido.getCliente().getNome();
        } catch (Exception e) {
            clienteNome = "(cliente removido)";
        }

        return CustoRealDto.builder()
            .pedidoId(pedidoId)
            .clienteNome(clienteNome)
            .totalPedido(total)
            .custoMP(custoMP)
            .rateioFixo(rateioFixo)
            .despesasVariaveis(despesasVariaveis)
            .custoReal(custoReal)
            .margemReal(margemReal)
            .margemDesejada(config.getMargemDesejadaPadrao())
            .linhasMP(linhasMP)
            .despesasDetalhadas(despesasDetalhadas)
            .build();
    }

    private long contarUnidadesMes(Pedido pedido) {
        var mesRef = pedido.getDataPedido().toLocalDate().withDayOfMonth(1);
        var fimMes = mesRef.plusMonths(1);
        return pedidoRepository.findAllComCliente().stream()
            .filter(p -> p.getStatus() != StatusPedido.CANCELADO)
            .filter(p -> {
                var d = p.getDataPedido().toLocalDate();
                return !d.isBefore(mesRef) && d.isBefore(fimMes);
            })
            .flatMap(p -> p.getItens().stream())
            .mapToLong(i -> i.getQuantidade())
            .sum();
    }
}
