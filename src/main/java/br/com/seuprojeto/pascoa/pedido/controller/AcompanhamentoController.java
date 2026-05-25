package br.com.seuprojeto.pascoa.pedido.controller;

import br.com.seuprojeto.pascoa.pedido.entity.Pedido;
import br.com.seuprojeto.pascoa.pedido.repository.PagamentoRepository;
import br.com.seuprojeto.pascoa.pedido.repository.PedidoRepository;
import br.com.seuprojeto.pascoa.producao.repository.OrdemProducaoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.math.BigDecimal;

/**
 * Página pública de acompanhamento de pedido — acessível sem autenticação.
 * URL: /acompanhamento/{tokenAcompanhamento}
 */
@Controller
@RequestMapping("/acompanhamento")
@RequiredArgsConstructor
public class AcompanhamentoController {

    private final PedidoRepository pedidoRepository;
    private final PagamentoRepository pagamentoRepository;
    private final OrdemProducaoRepository ordemRepository;

    @GetMapping("/{token}")
    public String rastrear(@PathVariable String token, Model model) {
        var optPedido = pedidoRepository.findByTokenAcompanhamento(token);
        if (optPedido.isEmpty()) {
            model.addAttribute("tokenInvalido", true);
            return "acompanhamento/pedido";
        }

        Pedido pedido = optPedido.get();
        BigDecimal totalPago   = pagamentoRepository.somarPorPedido(pedido.getId());
        BigDecimal saldoDevedor = pedido.getTotalPedido().subtract(totalPago);
        var ordens = ordemRepository.findByPedidoId(pedido.getId());

        // Percentual do progresso baseado no status do pedido
        int progresso = switch (pedido.getStatus()) {
            case NOVO       -> 10;
            case CONFIRMADO -> 30;
            case EM_PRODUCAO-> 55;
            case PRONTO     -> 80;
            case ENTREGUE   -> 100;
            case CANCELADO  -> 0;
        };

        // Percentual pago — calculado no controller para evitar SpEL com BigDecimal
        int pctPago = 0;
        if (pedido.getTotalPedido().compareTo(BigDecimal.ZERO) > 0) {
            pctPago = totalPago
                .divide(pedido.getTotalPedido(), 4, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(0, java.math.RoundingMode.HALF_UP)
                .intValue();
            pctPago = Math.min(pctPago, 100);
        }

        model.addAttribute("pedido", pedido);
        model.addAttribute("totalPago", totalPago);
        model.addAttribute("saldoDevedor", saldoDevedor);
        model.addAttribute("ordens", ordens);
        model.addAttribute("progresso", progresso);
        model.addAttribute("pctPago", pctPago);
        return "acompanhamento/pedido";
    }
}
