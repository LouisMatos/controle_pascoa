package br.com.seuprojeto.pascoa.financeiro.service;

import br.com.seuprojeto.pascoa.fichaTecnica.entity.FichaTecnica;
import br.com.seuprojeto.pascoa.fichaTecnica.service.FichaTecnicaService;
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
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustoRealService {

    private final PedidoRepository pedidoRepository;
    private final ItemPedidoRepository itemPedidoRepository;
    private final DespesaFixaRepository despesaFixaRepository;
    private final DespesaVariavelRepository despesaVariavelRepository;
    private final ConfiguracaoFinanceiraRepository configuracaoRepository;
    private final FichaTecnicaService fichaTecnicaService;

    @Transactional(readOnly = true)
    public CustoRealDto calcular(Long pedidoId) {
        Pedido pedido = pedidoRepository.findByIdComItens(pedidoId)
            .orElseThrow(() -> new IllegalArgumentException("Pedido não encontrado: " + pedidoId));

        var config = configuracaoRepository.obter();
        var fichasPorProduto = fichasDosItensSemSnapshot(pedido);

        var linhasMP = pedido.getItens().stream().map(item -> {
            var produto = item.getProduto();
            BigDecimal custoUnit = item.getCustoUnitario() != null
                ? item.getCustoUnitario()
                : custoUnitarioPelaFicha(fichasPorProduto.get(produto != null ? produto.getId() : null));

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

        var despesasDetalhadas = despesaVariavelRepository.findByPedidoIdOrderByCategoria(pedidoId);
        BigDecimal despesasVariaveis = despesasDetalhadas.stream()
            .map(d -> d.getValor())
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal custoReal = custoMP.add(rateioFixo).add(despesasVariaveis)
            .setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = pedido.getTotalPedido();
        BigDecimal margemReal = total.compareTo(BigDecimal.ZERO) > 0
            ? total.subtract(custoReal).divide(total, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100)).setScale(1, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;

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

    private Map<Long, FichaTecnica> fichasDosItensSemSnapshot(Pedido pedido) {
        List<Long> produtoIds = pedido.getItens().stream()
            .filter(item -> item.getCustoUnitario() == null && item.getProduto() != null)
            .map(item -> item.getProduto().getId())
            .distinct()
            .toList();
        return fichaTecnicaService.buscarPorProdutoIds(produtoIds).stream()
            .collect(Collectors.toMap(f -> f.getProduto().getId(), f -> f));
    }

    private BigDecimal custoUnitarioPelaFicha(FichaTecnica ficha) {
        if (ficha == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal custoTotalMP = ficha.getItens().stream()
            .map(item -> {
                BigDecimal custoMedio = item.getMateriaPrima().getCustoMedioPonderado();
                if (custoMedio == null || custoMedio.compareTo(BigDecimal.ZERO) == 0) {
                    custoMedio = item.getMateriaPrima().getCustoUnitario();
                }
                return item.getQuantidade().multiply(custoMedio);
            })
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal rendimento = ficha.getRendimento();
        return rendimento.compareTo(BigDecimal.ZERO) > 0
            ? custoTotalMP.divide(rendimento, 4, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;
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
