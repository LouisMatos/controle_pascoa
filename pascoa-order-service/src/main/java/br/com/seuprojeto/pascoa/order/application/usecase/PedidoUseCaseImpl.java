package br.com.seuprojeto.pascoa.order.application.usecase;

import br.com.seuprojeto.pascoa.order.application.port.in.PedidoUseCase;
import br.com.seuprojeto.pascoa.order.application.port.out.*;
import br.com.seuprojeto.pascoa.order.domain.exception.PedidoNotFoundException;
import br.com.seuprojeto.pascoa.order.domain.model.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class PedidoUseCaseImpl implements PedidoUseCase {

    private final PedidoRepositoryPort pedidoRepository;
    private final PedidoEventPublisherPort eventPublisher;
    private final ClienteServicePort clienteService;
    private final ProdutoServicePort produtoService;

    @Override
    public Pedido criar(CriarPedidoCommand cmd) {
        var cliente = clienteService.findById(cmd.clienteId())
                .orElseThrow(() -> new IllegalArgumentException("Cliente não encontrado: " + cmd.clienteId()));

        Pedido pedido = Pedido.novo(cliente.id(), cliente.nome(), cmd.observacao());
        Pedido salvo = pedidoRepository.save(pedido);
        eventPublisher.publishPedidoCriado(salvo);
        return salvo;
    }

    @Override
    @Transactional(readOnly = true)
    public Pedido buscarPorId(Long id) {
        return pedidoRepository.findById(id)
                .orElseThrow(() -> new PedidoNotFoundException(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Pedido buscarPorToken(String token) {
        return pedidoRepository.findByToken(token)
                .orElseThrow(() -> new PedidoNotFoundException(token));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Pedido> listar() {
        return pedidoRepository.findAll();
    }

    @Override
    public Pedido adicionarItem(AdicionarItemCommand cmd) {
        Pedido pedido = buscarPorId(cmd.pedidoId());
        if (pedido.getStatus() != StatusPedido.NOVO) {
            throw new IllegalStateException("Itens só podem ser adicionados em pedidos com status NOVO.");
        }

        var produto = produtoService.findById(cmd.produtoId())
                .orElseThrow(() -> new IllegalArgumentException("Produto não encontrado: " + cmd.produtoId()));

        if (!produto.disponivel()) {
            throw new IllegalStateException("Produto indisponível: " + produto.nome());
        }

        ItemPedido item = ItemPedido.builder()
                .produtoId(produto.id())
                .nomeProduto(produto.nome())
                .precoUnitario(produto.preco())
                .quantidade(cmd.quantidade())
                .build();

        List<ItemPedido> novosItens = new ArrayList<>(pedido.getItens());
        novosItens.add(item);
        return pedidoRepository.save(pedido.withItens(novosItens));
    }

    @Override
    public Pedido removerItem(Long pedidoId, Long itemId) {
        Pedido pedido = buscarPorId(pedidoId);
        if (pedido.getStatus() != StatusPedido.NOVO) {
            throw new IllegalStateException("Itens só podem ser removidos de pedidos com status NOVO.");
        }
        List<ItemPedido> novosItens = pedido.getItens().stream()
                .filter(i -> !i.getId().equals(itemId))
                .toList();
        return pedidoRepository.save(pedido.withItens(new ArrayList<>(novosItens)));
    }

    @Override
    public Pedido confirmar(Long pedidoId) {
        Pedido pedido = buscarPorId(pedidoId);
        if (pedido.getItens().isEmpty()) {
            throw new IllegalStateException("Não é possível confirmar um pedido sem itens.");
        }
        Pedido confirmado = pedido.transicionarPara(StatusPedido.CONFIRMADO);
        Pedido salvo = pedidoRepository.save(confirmado);
        eventPublisher.publishPedidoConfirmado(salvo);
        return salvo;
    }

    @Override
    public Pedido cancelar(Long pedidoId) {
        Pedido pedido = buscarPorId(pedidoId);
        Pedido cancelado = pedido.transicionarPara(StatusPedido.CANCELADO);
        Pedido salvo = pedidoRepository.save(cancelado);
        eventPublisher.publishPedidoCancelado(salvo);
        return salvo;
    }

    @Override
    public Pedido marcarPronto(Long pedidoId) {
        Pedido pedido = buscarPorId(pedidoId);
        return pedidoRepository.save(pedido.transicionarPara(StatusPedido.PRONTO));
    }

    @Override
    public Pedido registrarEntrega(Long pedidoId) {
        Pedido pedido = buscarPorId(pedidoId);
        Pedido entregue = pedido.transicionarPara(StatusPedido.ENTREGUE);
        Pedido salvo = pedidoRepository.save(entregue);
        eventPublisher.publishPedidoEntregue(salvo);
        return salvo;
    }

    @Override
    public Pedido registrarPagamento(RegistrarPagamentoCommand cmd) {
        Pedido pedido = buscarPorId(cmd.pedidoId());
        return pedidoRepository.save(pedido.withFormaPagamento(cmd.forma()));
    }
}
