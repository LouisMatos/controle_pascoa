package br.com.seuprojeto.pascoa.orcamento.service;

import br.com.seuprojeto.pascoa.cadastro.entity.Cliente;
import br.com.seuprojeto.pascoa.cadastro.entity.Produto;
import br.com.seuprojeto.pascoa.cadastro.repository.ClienteRepository;
import br.com.seuprojeto.pascoa.cadastro.repository.ProdutoRepository;
import br.com.seuprojeto.pascoa.orcamento.dto.OrcamentoForm;
import br.com.seuprojeto.pascoa.orcamento.dto.OrcamentoItemForm;
import br.com.seuprojeto.pascoa.orcamento.entity.Orcamento;
import br.com.seuprojeto.pascoa.orcamento.entity.OrcamentoItem;
import br.com.seuprojeto.pascoa.orcamento.entity.StatusOrcamento;
import br.com.seuprojeto.pascoa.orcamento.repository.OrcamentoRepository;
import br.com.seuprojeto.pascoa.notificacao.event.OrcamentoAcaoEvent;
import br.com.seuprojeto.pascoa.pedido.entity.ItemPedido;
import br.com.seuprojeto.pascoa.pedido.entity.Pedido;
import br.com.seuprojeto.pascoa.pedido.repository.ItemPedidoRepository;
import br.com.seuprojeto.pascoa.pedido.repository.PedidoRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrcamentoService {

    private final OrcamentoRepository orcamentoRepo;
    private final ClienteRepository clienteRepo;
    private final ProdutoRepository produtoRepo;
    private final PedidoRepository pedidoRepo;
    private final ItemPedidoRepository itemPedidoRepo;
    private final OrcamentoPdfService pdfService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public List<Orcamento> listar() {
        return orcamentoRepo.findAllComCliente();
    }

    @Transactional(readOnly = true)
    public Orcamento buscarPorId(Long id) {
        return orcamentoRepo.findByIdComItens(id)
                .orElseThrow(() -> new EntityNotFoundException("Orçamento não encontrado: " + id));
    }

    @Transactional(readOnly = true)
    public Orcamento buscarPorToken(String token) {
        return orcamentoRepo.findByTokenAprovacao(token)
                .orElseThrow(() -> new EntityNotFoundException("Orçamento não encontrado para este link."));
    }

    @Transactional
    public Orcamento criar(OrcamentoForm form, String usuario) {
        Cliente cliente = clienteRepo.findById(form.getClienteId())
                .orElseThrow(() -> new EntityNotFoundException("Cliente não encontrado"));

        Orcamento orc = Orcamento.builder()
                .cliente(cliente)
                .validade(form.getValidade())
                .observacoes(form.getObservacoes())
                .criadoPor(usuario)
                .build();
        orc = orcamentoRepo.save(orc);

        orc = adicionarItens(orc, form.getItens());
        return orc;
    }

    @Transactional
    public Orcamento atualizar(Long id, OrcamentoForm form) {
        Orcamento orc = buscarPorId(id);
        if (!orc.isPendente()) {
            throw new IllegalStateException("Apenas orçamentos pendentes podem ser editados.");
        }
        Cliente cliente = clienteRepo.findById(form.getClienteId())
                .orElseThrow(() -> new EntityNotFoundException("Cliente não encontrado"));

        orc.setCliente(cliente);
        orc.setValidade(form.getValidade());
        orc.setObservacoes(form.getObservacoes());
        orc.getItens().clear();
        orc = orcamentoRepo.save(orc);
        orc = adicionarItens(orc, form.getItens());
        return orc;
    }

    @Transactional
    public void excluir(Long id) {
        Orcamento orc = buscarPorId(id);
        if (!orc.isPendente()) {
            throw new IllegalStateException("Apenas orçamentos pendentes podem ser excluídos.");
        }
        orcamentoRepo.delete(orc);
    }

    @Transactional
    public void aprovar(String token) {
        Orcamento orc = buscarPorToken(token);
        if (orc.isExpirado()) {
            orc.setStatus(StatusOrcamento.EXPIRADO);
            orcamentoRepo.save(orc);
            throw new IllegalStateException("Este orçamento expirou em " + orc.getValidade() + ".");
        }
        if (!orc.isPendente()) {
            throw new IllegalStateException("Este orçamento já foi " + orc.getStatus().getDescricao().toLowerCase() + ".");
        }
        orc.setStatus(StatusOrcamento.APROVADO);
        orcamentoRepo.save(orc);
        eventPublisher.publishEvent(new OrcamentoAcaoEvent(orc, true));
    }

    @Transactional
    public void recusar(String token) {
        Orcamento orc = buscarPorToken(token);
        if (!orc.isPendente()) {
            throw new IllegalStateException("Este orçamento já foi " + orc.getStatus().getDescricao().toLowerCase() + ".");
        }
        orc.setStatus(StatusOrcamento.RECUSADO);
        orcamentoRepo.save(orc);
        eventPublisher.publishEvent(new OrcamentoAcaoEvent(orc, false));
    }

    @Transactional
    public Pedido converter(Long id) {
        Orcamento orc = buscarPorId(id);
        if (!orc.isAprovado()) {
            throw new IllegalStateException("Somente orçamentos aprovados podem ser convertidos em pedido.");
        }
        if (orc.getPedido() != null) {
            throw new IllegalStateException("Este orçamento já foi convertido no pedido #" + orc.getPedido().getId() + ".");
        }

        Pedido pedido = Pedido.builder()
                .cliente(orc.getCliente())
                .observacoes("Gerado a partir do Orçamento #" + orc.getId()
                        + (orc.getObservacoes() != null ? "\n" + orc.getObservacoes() : ""))
                .build();
        pedido = pedidoRepo.save(pedido);

        BigDecimal total = BigDecimal.ZERO;
        for (OrcamentoItem oi : orc.getItens()) {
            ItemPedido item = ItemPedido.builder()
                    .pedido(pedido)
                    .produto(oi.getProduto())
                    .quantidade(oi.getQuantidade())
                    .precoUnitario(oi.getPrecoUnitario())
                    .build();
            itemPedidoRepo.save(item);
            total = total.add(oi.getSubtotal());
        }

        pedido.setTotalPedido(total);
        pedido = pedidoRepo.save(pedido);

        orc.setPedido(pedido);
        orcamentoRepo.save(orc);

        return pedido;
    }

    public byte[] gerarPdf(Long id) {
        Orcamento orc = buscarPorId(id);
        return pdfService.gerar(orc);
    }

    // ── Auxiliar ─────────────────────────────────────────────────────────────

    private Orcamento adicionarItens(Orcamento orc, List<OrcamentoItemForm> itemForms) {
        if (itemForms == null || itemForms.isEmpty()) return orc;

        BigDecimal total = BigDecimal.ZERO;
        for (OrcamentoItemForm itemForm : itemForms) {
            if (itemForm.getProdutoId() == null) continue;
            Integer qtd = itemForm.getQuantidade() != null ? itemForm.getQuantidade() : 1;
            if (qtd <= 0) continue;

            Produto produto = produtoRepo.findById(itemForm.getProdutoId())
                    .orElseThrow(() -> new EntityNotFoundException("Produto não encontrado"));

            BigDecimal preco = itemForm.getPrecoUnitario() != null
                    ? itemForm.getPrecoUnitario()
                    : produto.getPrecoVenda();

            OrcamentoItem oi = OrcamentoItem.builder()
                    .orcamento(orc)
                    .produto(produto)
                    .quantidade(qtd)
                    .precoUnitario(preco)
                    .subtotal(preco.multiply(BigDecimal.valueOf(qtd)))
                    .build();
            orc.getItens().add(oi);
            total = total.add(oi.getSubtotal());
        }

        orc.setTotal(total);
        return orcamentoRepo.save(orc);
    }
}
