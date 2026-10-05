package br.com.seuprojeto.pascoa.pedido.service;

import br.com.seuprojeto.pascoa.cadastro.entity.Cliente;
import br.com.seuprojeto.pascoa.cadastro.entity.Produto;
import br.com.seuprojeto.pascoa.cadastro.repository.ClienteRepository;
import br.com.seuprojeto.pascoa.cadastro.repository.ProdutoRepository;
import br.com.seuprojeto.pascoa.fichaTecnica.entity.FichaTecnica;
import br.com.seuprojeto.pascoa.fichaTecnica.service.FichaTecnicaService;
import br.com.seuprojeto.pascoa.notificacao.entity.EventoNotificacao;
import br.com.seuprojeto.pascoa.notificacao.event.PedidoStatusEvent;
import br.com.seuprojeto.pascoa.notificacao.service.AlertaInternoService;
import br.com.seuprojeto.pascoa.pedido.dto.PagamentoForm;
import br.com.seuprojeto.pascoa.pedido.dto.PedidoForm;
import br.com.seuprojeto.pascoa.pedido.entity.ItemPedido;
import br.com.seuprojeto.pascoa.pedido.entity.Pagamento;
import br.com.seuprojeto.pascoa.pedido.entity.Pedido;
import br.com.seuprojeto.pascoa.pedido.entity.StatusPedido;
import br.com.seuprojeto.pascoa.pedido.repository.ItemPedidoRepository;
import br.com.seuprojeto.pascoa.pedido.repository.PagamentoRepository;
import br.com.seuprojeto.pascoa.pedido.repository.PedidoRepository;
import br.com.seuprojeto.pascoa.gastos.repository.GastoVariavelRepository;
import br.com.seuprojeto.pascoa.producao.entity.StatusOrdem;
import br.com.seuprojeto.pascoa.producao.event.ProducaoAtualizadaEvent;
import br.com.seuprojeto.pascoa.producao.service.ProducaoService;
import br.com.seuprojeto.pascoa.shared.exception.RecursoNaoEncontradoException;
import lombok.RequiredArgsConstructor;
import br.com.seuprojeto.pascoa.auditoria.annotation.Auditavel;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final ItemPedidoRepository itemRepository;
    private final PagamentoRepository pagamentoRepository;
    private final ClienteRepository clienteRepository;
    private final ProdutoRepository produtoRepository;
    private final ProducaoService producaoService;
    private final FichaTecnicaService fichaTecnicaService;
    private final GastoVariavelRepository gastoVariavelRepository;
    private final AlertaInternoService alertaInternoService;
    private final ApplicationEventPublisher eventPublisher;

    // -----------------------------------------------------------------------
    // Consultas
    // -----------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<Pedido> listarTodos() {
        return pedidoRepository.findAllComCliente();
    }

    @Transactional(readOnly = true)
    public Page<Pedido> listarPaginado(StatusPedido status, int pagina) {
        PageRequest pageRequest = PageRequest.of(pagina, 50);
        return status != null
            ? pedidoRepository.findByStatusComCliente(status, pageRequest)
            : pedidoRepository.findComCliente(pageRequest);
    }

    @Transactional(readOnly = true)
    public Pedido buscarPorId(Long id) {
        return pedidoRepository.findByIdComItens(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Pedido não encontrado: " + id));
    }

    // -----------------------------------------------------------------------
    // CRUD básico
    // -----------------------------------------------------------------------

    @Transactional
    public Pedido criar(PedidoForm form) {
        Cliente cliente = clienteRepository.findVigenteById(form.getClienteId())
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
        if (dataEntrega != null && dataEntrega.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Data de entrega não pode ser no passado.");
        }
        Cliente cliente = clienteRepository.findVigenteById(clienteId)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente não encontrado"));

        Pedido pedido = Pedido.builder()
            .cliente(cliente)
            .dataEntrega(dataEntrega)
            .slotEntrega(slotEntrega)
            .observacoes(observacoes)
            .build();
        pedido = pedidoRepository.save(pedido);

        // Batch fetch dos produtos (evita N+1) e inserts em batch.
        List<Long> idsValidos = produtoIds.stream()
            .filter(Objects::nonNull)
            .distinct()
            .toList();
        Map<Long, Produto> produtosPorId = produtoRepository.findVigentesByIds(idsValidos).stream()
            .collect(Collectors.toMap(Produto::getId, p -> p));
        if (produtosPorId.size() != idsValidos.size()) {
            throw new RecursoNaoEncontradoException("Um ou mais produtos não foram encontrados.");
        }

        List<ItemPedido> itens = new ArrayList<>(produtoIds.size());
        BigDecimal total = BigDecimal.ZERO;
        for (int i = 0; i < produtoIds.size(); i++) {
            Long produtoId = produtoIds.get(i);
            Integer qtd = (quantidades != null && i < quantidades.size())
                    ? quantidades.get(i) : 1;
            if (produtoId == null || qtd == null || qtd <= 0) { continue; }

            Produto produto = produtosPorId.get(produtoId);
            itens.add(ItemPedido.builder()
                .pedido(pedido)
                .produto(produto)
                .quantidade(qtd)
                .precoUnitario(produto.getPrecoVenda())
                .build());
            total = total.add(produto.getPrecoVenda().multiply(BigDecimal.valueOf(qtd)));
        }
        if (itens.isEmpty()) {
            throw new IllegalArgumentException("Informe pelo menos um produto com quantidade maior que zero.");
        }
        itemRepository.saveAll(itens);

        pedido.setTotalPedido(total);
        return pedidoRepository.save(pedido);
    }

    @Transactional
    public Pedido atualizar(Long id, PedidoForm form) {
        Pedido pedido = buscarPorId(id);
        if (pedido.getStatus() != StatusPedido.NOVO) {
            throw new IllegalStateException("Apenas pedidos com status NOVO podem ser editados.");
        }
        Cliente cliente = clienteRepository.findVigenteById(form.getClienteId())
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
        Produto produto = produtoRepository.findVigenteById(produtoId)
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

    @Auditavel(acao = "CONFIRMAR_PEDIDO", entidade = "Pedido")
    @PreAuthorize("@authService.owns(#id, authentication)")
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
        snapshotCustos(pedido);
        pedidoRepository.save(pedido);
        producaoService.gerarOrdens(pedido);   // usa pedido (com itens carregados via findByIdComItens)
        eventPublisher.publishEvent(new PedidoStatusEvent(pedido, EventoNotificacao.PEDIDO_CONFIRMADO));
        return pedido;
    }

    @Transactional(readOnly = true)
    public List<String> avisosProducao(Long pedidoId) {
        Pedido pedido = buscarPorId(pedidoId);
        List<Long> produtoIds = pedido.getItens().stream().map(i -> i.getProduto().getId()).distinct().toList();
        Map<Long, FichaTecnica> fichas = fichaTecnicaService.buscarPorProdutoIds(produtoIds).stream()
            .collect(Collectors.toMap(f -> f.getProduto().getId(), f -> f));
        List<String> avisos = new ArrayList<>();
        for (ItemPedido item : pedido.getItens()) {
            String nome = item.getProduto().getNome();
            FichaTecnica ficha = fichas.get(item.getProduto().getId());
            if (ficha == null || ficha.getItens().isEmpty()
                    || ficha.getRendimento() == null || ficha.getRendimento().signum() <= 0) {
                avisos.add(nome + ": sem ficha técnica válida");
                continue;
            }
            BigDecimal qtd = BigDecimal.valueOf(item.getQuantidade());
            for (var fi : ficha.getItens()) {
                BigDecimal necessario = fi.getQuantidade().multiply(qtd)
                    .divide(ficha.getRendimento(), 4, java.math.RoundingMode.HALF_UP);
                if (fi.getMateriaPrima().getQuantidadeAtual().compareTo(necessario) < 0) {
                    avisos.add(nome + ": estoque insuficiente de " + fi.getMateriaPrima().getNome());
                }
            }
        }
        return avisos;
    }

    @Auditavel(acao = "CANCELAR_PEDIDO", entidade = "Pedido")
    @PreAuthorize("@authService.owns(#id, authentication)")
    @Transactional
    public Pedido cancelar(Long id) {
        Pedido pedido = buscarPorId(id);
        if (!pedido.getStatus().podeCancelar()) {
            throw new IllegalStateException("Pedido entregue não pode ser cancelado.");
        }
        BigDecimal pagoAntesDoCancelamento = pagamentoRepository.somarPorPedido(id);
        pedido.setStatus(StatusPedido.CANCELADO);
        Pedido pedidoCancelado = pedidoRepository.save(pedido);

        producaoService.listarPorPedido(id).stream()
            .filter(o -> o.getStatus().podeCancelar())
            .forEach(o -> producaoService.cancelarOrdem(o.getId()));

        gastoVariavelRepository.desconsiderarPorPedido(id);

        if (pagoAntesDoCancelamento.compareTo(BigDecimal.ZERO) > 0) {
            alertaInternoService.criar(
                "Pedido #" + id + " cancelado com R$ " + pagoAntesDoCancelamento
                    + " já recebido — verificar devolução ao cliente.",
                "/pedidos/" + id,
                "bi-cash-coin",
                "warning"
            );
        }

        eventPublisher.publishEvent(new PedidoStatusEvent(pedidoCancelado, EventoNotificacao.PEDIDO_CANCELADO));
        return pedidoCancelado;
    }

    @Auditavel(acao = "PEDIDO_PRONTO", entidade = "Pedido")
    @PreAuthorize("@authService.owns(#id, authentication)")
    @Transactional
    public Pedido marcarPronto(Long id) {
        Pedido pedido = buscarPorId(id);
        if (!pedido.getStatus().podePronto()) {
            throw new IllegalStateException("Pedido deve estar CONFIRMADO ou EM_PRODUCAO.");
        }
        if (producaoService.existeOrdem(id, StatusOrdem.PENDENTE, StatusOrdem.EM_ANDAMENTO)) {
            throw new IllegalStateException("Há ordens de produção em aberto. Conclua a produção antes de marcar como PRONTO.");
        }
        return aplicarStatus(pedido, StatusPedido.PRONTO, EventoNotificacao.PEDIDO_PRONTO);
    }

    @EventListener
    @Transactional
    public void sincronizarComProducao(ProducaoAtualizadaEvent evento) {
        Pedido pedido = pedidoRepository.findById(evento.pedidoId()).orElse(null);
        if (pedido == null) {
            return;
        }
        boolean emFabricacao = producaoService.existeOrdem(pedido.getId(), StatusOrdem.EM_ANDAMENTO);
        boolean pendente = producaoService.existeOrdem(pedido.getId(), StatusOrdem.PENDENTE);
        boolean concluida = producaoService.existeOrdem(pedido.getId(), StatusOrdem.CONCLUIDA);

        if (emFabricacao && pedido.getStatus() == StatusPedido.CONFIRMADO) {
            aplicarStatus(pedido, StatusPedido.EM_PRODUCAO, EventoNotificacao.PRODUCAO_INICIADA);
        } else if (!emFabricacao && !pendente && concluida && pedido.getStatus().podePronto()) {
            aplicarStatus(pedido, StatusPedido.PRONTO, EventoNotificacao.PEDIDO_PRONTO);
        }
    }

    private Pedido aplicarStatus(Pedido pedido, StatusPedido status, EventoNotificacao evento) {
        pedido.setStatus(status);
        Pedido salvo = pedidoRepository.save(pedido);
        eventPublisher.publishEvent(new PedidoStatusEvent(salvo, evento));
        return salvo;
    }

    @Auditavel(acao = "ENTREGAR_PEDIDO", entidade = "Pedido")
    @PreAuthorize("@authService.owns(#id, authentication)")
    @Transactional
    public Pedido registrarEntrega(Long id) {
        Pedido pedido = buscarPorId(id);
        if (!pedido.getStatus().podeEntregar()) {
            throw new IllegalStateException("Pedido deve estar PRONTO para ser entregue.");
        }
        return aplicarStatus(pedido, StatusPedido.ENTREGUE, EventoNotificacao.PEDIDO_ENTREGUE);
    }

    private void snapshotCustos(Pedido pedido) {
        if (pedido.getItens().isEmpty()) { return; }

        // 1 query traz todas as fichas + itens + matérias-primas dos produtos do pedido.
        List<Long> produtoIds = pedido.getItens().stream()
            .map(i -> i.getProduto().getId())
            .distinct()
            .toList();
        Map<Long, FichaTecnica> fichasPorProduto = fichaTecnicaService
            .buscarPorProdutoIds(produtoIds).stream()
            .collect(Collectors.toMap(f -> f.getProduto().getId(), f -> f));

        for (ItemPedido item : pedido.getItens()) {
            FichaTecnica ficha = fichasPorProduto.get(item.getProduto().getId());
            item.setCustoUnitario(fichaTecnicaService.calcularCustoPorUnidade(ficha));
        }
        // Flush em batch (com hibernate.jdbc.batch_size configurado em application.properties).
        itemRepository.saveAll(pedido.getItens());
    }

    @Transactional
    public void registrarPagamento(Long pedidoId, PagamentoForm form) {
        Pedido pedido = pedidoRepository.findById(pedidoId)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Pedido não encontrado"));
        if (!pedido.getStatus().podeAdicionarPagamento()) {
            throw new IllegalStateException("Pagamentos só podem ser registrados após a confirmação do pedido.");
        }

        LocalDate dataPagamento = form.getDataPagamento() != null ? form.getDataPagamento() : LocalDate.now();
        if (pagamentoRepository.existsByPedidoIdAndValorAndTipoPagamentoAndDataPagamento(
                pedidoId, form.getValor(), form.getTipoPagamento(), dataPagamento)) {
            throw new IllegalArgumentException(
                "Este pagamento já foi registrado neste pedido. Recarregue a tela antes de lançar novamente.");
        }

        BigDecimal saldoEmAberto = pedido.getTotalPedido().subtract(pagamentoRepository.somarPorPedido(pedidoId));
        if (saldoEmAberto.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Pedido já está quitado.");
        }
        if (form.getValor().compareTo(saldoEmAberto) > 0) {
            throw new IllegalArgumentException("Valor acima do saldo em aberto do pedido: " + saldoEmAberto);
        }

        pagamentoRepository.save(Pagamento.builder()
            .pedido(pedido)
            .valor(form.getValor())
            .tipoPagamento(form.getTipoPagamento())
            .dataPagamento(dataPagamento)
            .observacoes(form.getObservacoes())
            .build());
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
