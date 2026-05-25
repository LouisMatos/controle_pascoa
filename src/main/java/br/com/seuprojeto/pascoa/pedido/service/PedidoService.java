package br.com.seuprojeto.pascoa.pedido.service;

import br.com.seuprojeto.pascoa.cadastro.entity.Cliente;
import br.com.seuprojeto.pascoa.cadastro.entity.Produto;
import br.com.seuprojeto.pascoa.cadastro.repository.ClienteRepository;
import br.com.seuprojeto.pascoa.cadastro.repository.ProdutoRepository;
import br.com.seuprojeto.pascoa.notificacao.entity.EventoNotificacao;
import br.com.seuprojeto.pascoa.notificacao.event.PedidoStatusEvent;
import br.com.seuprojeto.pascoa.pedido.dto.PagamentoForm;
import br.com.seuprojeto.pascoa.pedido.dto.PedidoForm;
import br.com.seuprojeto.pascoa.pedido.entity.*;
import br.com.seuprojeto.pascoa.pedido.repository.ItemPedidoRepository;
import br.com.seuprojeto.pascoa.pedido.repository.PagamentoRepository;
import br.com.seuprojeto.pascoa.pedido.repository.PedidoRepository;
import br.com.seuprojeto.pascoa.producao.service.ProducaoService;
import br.com.seuprojeto.pascoa.shared.exception.RecursoNaoEncontradoException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final ItemPedidoRepository itemRepository;
    private final PagamentoRepository pagamentoRepository;
    private final ClienteRepository clienteRepository;
    private final ProdutoRepository produtoRepository;
    private final ProducaoService producaoService;
    private final ApplicationEventPublisher eventPublisher;

    // -----------------------------------------------------------------------
    // Consultas
    // -----------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<Pedido> listarTodos() {
        return pedidoRepository.findAllComCliente();
    }

    @Transactional(readOnly = true)
    public List<Pedido> listarPorStatus(StatusPedido status) {
        return pedidoRepository.findByStatusComCliente(status);
    }

    @Transactional(readOnly = true)
    public Pedido buscarPorId(Long id) {
        return pedidoRepository.findByIdComItens(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Pedido não encontrado: " + id));
    }

    @Transactional
    public void salvarSemRecalculo(Pedido pedido) {
        pedidoRepository.save(pedido);
    }

    // -----------------------------------------------------------------------
    // CRUD básico
    // -----------------------------------------------------------------------

    @Transactional
    public Pedido criar(PedidoForm form) {
        Cliente cliente = clienteRepository.findById(form.getClienteId())
            .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente não encontrado"));
        Pedido pedido = Pedido.builder()
            .cliente(cliente)
            .dataEntrega(form.getDataEntrega())
            .observacoes(form.getObservacoes())
            .build();
        return pedidoRepository.save(pedido);
    }

    /**
     * Wizard: cria pedido e seus itens em uma única transação.
     * Recebe listas paralelas de produtoIds e quantidades.
     */
    @Transactional
    public Pedido criarComItens(Long clienteId,
                                java.time.LocalDate dataEntrega,
                                java.time.LocalTime slotEntrega,
                                String observacoes,
                                List<Long> produtoIds,
                                List<Integer> quantidades) {
        if (produtoIds == null || produtoIds.isEmpty()) {
            throw new IllegalArgumentException("O pedido precisa ter pelo menos um produto.");
        }
        Cliente cliente = clienteRepository.findById(clienteId)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente não encontrado"));

        Pedido pedido = Pedido.builder()
            .cliente(cliente)
            .dataEntrega(dataEntrega)
            .slotEntrega(slotEntrega)
            .observacoes(observacoes)
            .build();
        pedido = pedidoRepository.save(pedido);

        BigDecimal total = BigDecimal.ZERO;
        for (int i = 0; i < produtoIds.size(); i++) {
            Long produtoId = produtoIds.get(i);
            Integer qtd = (quantidades != null && i < quantidades.size())
                    ? quantidades.get(i) : 1;
            if (produtoId == null || qtd == null || qtd <= 0) continue;

            Produto produto = produtoRepository.findById(produtoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Produto não encontrado: " + produtoId));

            ItemPedido item = ItemPedido.builder()
                .pedido(pedido)
                .produto(produto)
                .quantidade(qtd)
                .precoUnitario(produto.getPrecoVenda())
                .build();
            itemRepository.save(item);
            total = total.add(produto.getPrecoVenda().multiply(BigDecimal.valueOf(qtd)));
        }

        pedido.setTotalPedido(total);
        return pedidoRepository.save(pedido);
    }

    @Transactional
    public Pedido atualizar(Long id, PedidoForm form) {
        Pedido pedido = buscarPorId(id);
        if (pedido.getStatus() != StatusPedido.NOVO) {
            throw new IllegalStateException("Apenas pedidos com status NOVO podem ser editados.");
        }
        Cliente cliente = clienteRepository.findById(form.getClienteId())
            .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente não encontrado"));
        pedido.setCliente(cliente);
        pedido.setDataEntrega(form.getDataEntrega());
        pedido.setObservacoes(form.getObservacoes());
        return pedidoRepository.save(pedido);
    }

    // -----------------------------------------------------------------------
    // Itens
    // -----------------------------------------------------------------------

    @Transactional
    public void adicionarItem(Long pedidoId, Long produtoId, Integer quantidade) {
        Pedido pedido = buscarPorId(pedidoId);
        if (!pedido.getStatus().podeAdicionarItens()) {
            throw new IllegalStateException("Itens só podem ser adicionados a pedidos com status NOVO.");
        }
        if (itemRepository.existsByPedidoIdAndProdutoId(pedidoId, produtoId)) {
            throw new IllegalArgumentException("Este produto já está no pedido. Remova-o antes de adicionar novamente.");
        }
        Produto produto = produtoRepository.findById(produtoId)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Produto não encontrado"));

        ItemPedido item = ItemPedido.builder()
            .pedido(pedido)
            .produto(produto)
            .quantidade(quantidade)
            .precoUnitario(produto.getPrecoVenda())
            .build();
        itemRepository.save(item); // @PrePersist calcula o subtotal automaticamente
        recalcularTotal(pedidoId);
    }

    @Transactional
    public void removerItem(Long pedidoId, Long itemId) {
        Pedido pedido = buscarPorId(pedidoId);
        if (!pedido.getStatus().podeAdicionarItens()) {
            throw new IllegalStateException("Itens só podem ser removidos de pedidos com status NOVO.");
        }
        itemRepository.deleteById(itemId);
        recalcularTotal(pedidoId);
    }

    private void recalcularTotal(Long pedidoId) {
        Pedido pedido = pedidoRepository.findByIdComItens(pedidoId)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Pedido não encontrado"));
        BigDecimal total = pedido.getItens().stream()
            .map(i -> i.getSubtotal() != null ? i.getSubtotal() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        pedido.setTotalPedido(total);
        pedidoRepository.save(pedido);
    }

    // -----------------------------------------------------------------------
    // Máquina de estados
    // -----------------------------------------------------------------------

    @Transactional
    public Pedido confirmar(Long id) {
        Pedido pedido = buscarPorId(id);
        if (!pedido.getStatus().podeConfirmar()) {
            throw new IllegalStateException("Pedido não está no status NOVO.");
        }
        if (pedido.getItens().isEmpty()) {
            throw new IllegalStateException("Pedido sem itens não pode ser confirmado.");
        }
        pedido.setStatus(StatusPedido.CONFIRMADO);
        pedidoRepository.save(pedido);
        producaoService.gerarOrdens(pedido);   // usa pedido (com itens carregados via findByIdComItens)
        eventPublisher.publishEvent(new PedidoStatusEvent(pedido, EventoNotificacao.PEDIDO_CONFIRMADO));
        return pedido;
    }

    @Transactional
    public Pedido cancelar(Long id) {
        Pedido pedido = buscarPorId(id);
        if (!pedido.getStatus().podeCancelar()) {
            throw new IllegalStateException("Pedido entregue não pode ser cancelado.");
        }
        pedido.setStatus(StatusPedido.CANCELADO);
        return pedidoRepository.save(pedido);
    }

    @Transactional
    public Pedido marcarPronto(Long id) {
        Pedido pedido = buscarPorId(id);
        if (!pedido.getStatus().podePronto()) {
            throw new IllegalStateException("Pedido deve estar CONFIRMADO ou EM_PRODUCAO.");
        }
        pedido.setStatus(StatusPedido.PRONTO);
        Pedido pedidoPronto = pedidoRepository.save(pedido);
        eventPublisher.publishEvent(new PedidoStatusEvent(pedidoPronto, EventoNotificacao.PEDIDO_PRONTO));
        return pedidoPronto;
    }

    @Transactional
    public Pedido registrarEntrega(Long id) {
        Pedido pedido = buscarPorId(id);
        if (!pedido.getStatus().podeEntregar()) {
            throw new IllegalStateException("Pedido deve estar PRONTO para ser entregue.");
        }
        pedido.setStatus(StatusPedido.ENTREGUE);
        Pedido pedidoEntregue = pedidoRepository.save(pedido);
        eventPublisher.publishEvent(new PedidoStatusEvent(pedidoEntregue, EventoNotificacao.PEDIDO_ENTREGUE));
        return pedidoEntregue;
    }

    // -----------------------------------------------------------------------
    // Pagamentos
    // -----------------------------------------------------------------------

    @Transactional
    public void registrarPagamento(Long pedidoId, PagamentoForm form) {
        Pedido pedido = pedidoRepository.findById(pedidoId)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Pedido não encontrado"));
        if (!pedido.getStatus().podeAdicionarPagamento()) {
            throw new IllegalStateException("Pagamentos só podem ser registrados após a confirmação do pedido.");
        }
        Pagamento pagamento = Pagamento.builder()
            .pedido(pedido)
            .valor(form.getValor())
            .tipoPagamento(form.getTipoPagamento())
            .dataPagamento(form.getDataPagamento())
            .observacoes(form.getObservacoes())
            .build();
        pagamentoRepository.save(pagamento);
        eventPublisher.publishEvent(new PedidoStatusEvent(pedido, EventoNotificacao.PAGAMENTO_RECEBIDO));
    }

    @Transactional(readOnly = true)
    public BigDecimal totalPago(Long pedidoId) {
        return pagamentoRepository.somarPorPedido(pedidoId);
    }

    @Transactional(readOnly = true)
    public java.util.List<Pagamento> listarPagamentos(Long pedidoId) {
        return pagamentoRepository.findByPedidoIdOrderByDataPagamentoDesc(pedidoId);
    }
}
