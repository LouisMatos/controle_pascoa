package br.com.seuprojeto.pascoa.crm.dto;

import br.com.seuprojeto.pascoa.cadastro.entity.Cliente;
import br.com.seuprojeto.pascoa.crm.entity.SegmentoCliente;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ClienteCrmDto(
        Cliente cliente,
        BigDecimal ltv,
        long totalPedidos,
        int saldoPontos,
        SegmentoCliente segmento,
        LocalDateTime ultimoPedido
) {
    /** Posição no ranking (definida externamente pelo serviço). */
    public String ltvFormatado() {
        return "R$ " + String.format("%.2f", ltv).replace(".", ",");
    }
}
