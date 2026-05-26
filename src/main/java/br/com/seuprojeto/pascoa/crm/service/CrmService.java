package br.com.seuprojeto.pascoa.crm.service;

import br.com.seuprojeto.pascoa.cadastro.entity.Cliente;
import br.com.seuprojeto.pascoa.cadastro.repository.ClienteRepository;
import br.com.seuprojeto.pascoa.crm.dto.ClienteCrmDto;
import br.com.seuprojeto.pascoa.crm.entity.*;
import br.com.seuprojeto.pascoa.crm.repository.NotaClienteRepository;
import br.com.seuprojeto.pascoa.crm.repository.PontoFidelidadeRepository;
import br.com.seuprojeto.pascoa.pedido.entity.Pedido;
import br.com.seuprojeto.pascoa.pedido.repository.PedidoRepository;
import br.com.seuprojeto.pascoa.shared.exception.RecursoNaoEncontradoException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CrmService {

    private final ClienteRepository clienteRepo;
    private final PedidoRepository pedidoRepo;
    private final PontoFidelidadeRepository pontoRepo;
    private final NotaClienteRepository notaRepo;

    // ── Ranking / Dashboard ────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ClienteCrmDto> gerarRanking() {
        List<Cliente> clientes = clienteRepo.findAllByOrderByNomeAsc();
        Map<Long, Object[]> statsMap = pedidoRepo.statsPorCliente().stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> row));

        return clientes.stream().map(c -> {
            Object[] stats = statsMap.get(c.getId());
            BigDecimal ltv = stats != null ? (BigDecimal) stats[1] : BigDecimal.ZERO;
            long count = stats != null ? (Long) stats[2] : 0L;
            LocalDateTime ultimoPedido = stats != null ? (LocalDateTime) stats[3] : null;
            int saldo = pontoRepo.saldoPorCliente(c.getId());
            return new ClienteCrmDto(c, ltv, count, saldo, calcularSegmento(ltv, count, ultimoPedido), ultimoPedido);
        })
        .sorted(Comparator.comparing(ClienteCrmDto::ltv).reversed())
        .toList();
    }

    // ── Perfil individual ─────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public ClienteCrmDto perfilCliente(Long id) {
        Cliente cliente = clienteRepo.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente não encontrado: " + id));

        List<Object[]> stats = pedidoRepo.statsPorCliente().stream()
                .filter(row -> id.equals(row[0])).toList();

        BigDecimal ltv = stats.isEmpty() ? BigDecimal.ZERO : (BigDecimal) stats.get(0)[1];
        long count = stats.isEmpty() ? 0L : (Long) stats.get(0)[2];
        LocalDateTime ultimoPedido = stats.isEmpty() ? null : (LocalDateTime) stats.get(0)[3];
        int saldo = pontoRepo.saldoPorCliente(id);
        return new ClienteCrmDto(cliente, ltv, count, saldo, calcularSegmento(ltv, count, ultimoPedido), ultimoPedido);
    }

    @Transactional(readOnly = true)
    public List<Pedido> ultimosPedidos(Long clienteId) {
        return pedidoRepo.ultimosPedidosPorCliente(clienteId);
    }

    @Transactional(readOnly = true)
    public List<NotaCliente> notasCliente(Long clienteId) {
        return notaRepo.findByClienteIdOrderByCriadoEmDesc(clienteId);
    }

    @Transactional(readOnly = true)
    public List<PontoFidelidade> pontosCliente(Long clienteId) {
        return pontoRepo.findByClienteIdOrderByDataOperacaoDesc(clienteId);
    }

    // ── Notas ─────────────────────────────────────────────────────────────

    @Transactional
    public void salvarNota(Long clienteId, String texto, String usuario) {
        Cliente cliente = clienteRepo.findById(clienteId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente não encontrado: " + clienteId));
        NotaCliente nota = NotaCliente.builder()
                .cliente(cliente)
                .texto(texto.strip())
                .criadoPor(usuario)
                .build();
        notaRepo.save(nota);
    }

    @Transactional
    public void excluirNota(Long notaId) {
        notaRepo.deleteById(notaId);
    }

    // ── Pontos ────────────────────────────────────────────────────────────

    @Transactional
    public void lancarPontos(Long clienteId, int pontos, TipoPonto tipo, String descricao, String usuario) {
        Cliente cliente = clienteRepo.findById(clienteId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente não encontrado: " + clienteId));

        if (tipo == TipoPonto.DEBITO) {
            int saldo = pontoRepo.saldoPorCliente(clienteId);
            if (saldo < pontos) {
                throw new IllegalStateException("Saldo insuficiente. Saldo atual: " + saldo + " pontos.");
            }
        }

        PontoFidelidade ponto = PontoFidelidade.builder()
                .cliente(cliente)
                .pontos(pontos)
                .tipo(tipo)
                .descricao(descricao != null ? descricao.strip() : null)
                .build();
        pontoRepo.save(ponto);
    }

    // ── Segmentação ───────────────────────────────────────────────────────

    private SegmentoCliente calcularSegmento(BigDecimal ltv, long totalPedidos, LocalDateTime ultimoPedido) {
        if (totalPedidos == 0) return SegmentoCliente.NOVO;
        if (ultimoPedido != null && ultimoPedido.isBefore(LocalDateTime.now().minusDays(90))) {
            return SegmentoCliente.INATIVO;
        }
        if (ltv.compareTo(BigDecimal.valueOf(300)) >= 0 || totalPedidos >= 3) {
            return SegmentoCliente.VIP;
        }
        return SegmentoCliente.REGULAR;
    }
}
