package br.com.seuprojeto.pascoa.pedido.controller;

import br.com.seuprojeto.pascoa.pedido.entity.Pedido;
import br.com.seuprojeto.pascoa.pedido.entity.StatusPedido;
import br.com.seuprojeto.pascoa.pedido.repository.PagamentoRepository;
import br.com.seuprojeto.pascoa.pedido.repository.PedidoRepository;
import br.com.seuprojeto.pascoa.pedido.service.ExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Endpoints de exportação — Excel e PDF.
 * Todos requerem autenticação (coberto por anyRequest().authenticated() no SecurityConfig).
 */
@Controller
@RequestMapping("/export")
@RequiredArgsConstructor
public class ExportController {

    private final PedidoRepository pedidoRepository;
    private final PagamentoRepository pagamentoRepository;
    private final ExportService exportService;

    /**
     * GET /export/pedidos/excel[?status=NOVO|CONFIRMADO|...]
     * Exporta a lista de pedidos (filtrada por status se informado) em formato Excel.
     */
    @GetMapping("/pedidos/excel")
    public ResponseEntity<byte[]> exportarExcel(
            @RequestParam(required = false) StatusPedido status) throws IOException {

        List<Pedido> pedidos = (status != null)
                ? pedidoRepository.findByStatusComCliente(status)
                : pedidoRepository.findAllComCliente();

        byte[] bytes = exportService.gerarExcelPedidos(pedidos);

        String filename = "pedidos_" + LocalDate.now() + ".xlsx";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }

    /**
     * GET /export/pedido/{id}/pdf
     * Exporta o comprovante de um pedido específico em PDF.
     */
    @GetMapping("/pedido/{id}/pdf")
    public ResponseEntity<byte[]> exportarPdfPedido(@PathVariable Long id) {

        Pedido pedido = pedidoRepository.findByIdComItens(id)
                .orElseThrow(() -> new IllegalArgumentException("Pedido não encontrado: " + id));

        BigDecimal totalPago = pagamentoRepository.somarPorPedido(id);
        BigDecimal saldo     = pedido.getTotalPedido().subtract(totalPago);

        byte[] bytes = exportService.gerarPdfPedido(pedido, totalPago, saldo);

        String filename = "pedido_" + id + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(bytes);
    }
}
