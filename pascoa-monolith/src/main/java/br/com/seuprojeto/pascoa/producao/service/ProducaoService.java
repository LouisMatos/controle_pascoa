package br.com.seuprojeto.pascoa.producao.service;

import br.com.seuprojeto.pascoa.estoque.service.EstoqueService;
import br.com.seuprojeto.pascoa.fichaTecnica.entity.FichaTecnica;
import br.com.seuprojeto.pascoa.fichaTecnica.entity.FichaTecnicaItem;
import br.com.seuprojeto.pascoa.fichaTecnica.repository.FichaTecnicaRepository;
import br.com.seuprojeto.pascoa.notificacao.service.AlertaInternoService;
import br.com.seuprojeto.pascoa.common.quantidade.Quantidades;
import br.com.seuprojeto.pascoa.pedido.entity.ItemPedido;
import br.com.seuprojeto.pascoa.pedido.entity.Pedido;
import br.com.seuprojeto.pascoa.producao.entity.OrdemProducao;
import br.com.seuprojeto.pascoa.producao.entity.StatusOrdem;
import br.com.seuprojeto.pascoa.producao.event.ProducaoAtualizadaEvent;
import br.com.seuprojeto.pascoa.producao.repository.OrdemProducaoRepository;
import br.com.seuprojeto.pascoa.shared.exception.EstoqueInsuficienteException;
import br.com.seuprojeto.pascoa.shared.exception.RecursoNaoEncontradoException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProducaoService {

    private final OrdemProducaoRepository ordemRepository;
    private final FichaTecnicaRepository fichaTecnicaRepository;
    private final EstoqueService estoqueService;
    private final AlertaInternoService alertaInternoService;
    private final ApplicationEventPublisher eventPublisher;

    // -----------------------------------------------------------------------
    // Consultas
    // -----------------------------------------------------------------------

    @Transactional(readOnly = true)
    public Page<OrdemProducao> listarFila(StatusOrdem status, int pagina) {
        PageRequest pageRequest = PageRequest.of(pagina, 50);
        return status != null
            ? ordemRepository.findByStatusComDetalhes(status, pageRequest)
            : ordemRepository.findComDetalhes(pageRequest);
    }

    @Transactional(readOnly = true)
    public OrdemProducao buscarPorId(Long id) {
        return ordemRepository.findByIdComDetalhes(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Ordem de produção não encontrada: " + id));
    }

    @Transactional(readOnly = true)
    public List<OrdemProducao> listarPorPedido(Long pedidoId) {
        return ordemRepository.findByPedidoId(pedidoId);
    }

    @Transactional(readOnly = true)
    public boolean existeOrdem(Long pedidoId, StatusOrdem... status) {
        return ordemRepository.existsByPedidoIdAndStatusIn(pedidoId, List.of(status));
    }

    @Transactional(readOnly = true)
    public Optional<FichaTecnica> buscarFicha(Long produtoId) {
        return fichaTecnicaRepository.findByProdutoIdComItens(produtoId);
    }

    public record LinhaReceita(String nome, String unidade, BigDecimal qtdReceita, BigDecimal qtdNecessaria,
                               BigDecimal disponivel, boolean estoqueOk, BigDecimal custoUnitario,
                               BigDecimal custo) {}

    public record ReceitaCalculada(List<LinhaReceita> linhas, BigDecimal custoTotal, BigDecimal custoPorUnidade,
                                   long insuficientes, long semCusto) {}

    /** Quantidade e custo por insumo escalados pela ordem: item × qtdOrdem ÷ rendimento. */
    public ReceitaCalculada calcularReceita(OrdemProducao ordem, FichaTecnica ficha) {
        BigDecimal qtdOrdem = ordem.getQuantidade();
        BigDecimal rendimento = ficha.getRendimento();
        boolean rendimentoValido = rendimento != null && rendimento.compareTo(BigDecimal.ZERO) > 0;
        List<LinhaReceita> linhas = new ArrayList<>();
        for (FichaTecnicaItem item : ficha.getItens()) {
            var mp = item.getMateriaPrima();
            BigDecimal necessaria = rendimentoValido
                ? item.getQuantidade().multiply(qtdOrdem).divide(rendimento, 3, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
            BigDecimal custoUnit = mp.getCustoUnitario();
            BigDecimal custo = custoUnit == null ? BigDecimal.ZERO
                : necessaria.multiply(custoUnit).setScale(2, RoundingMode.HALF_UP);
            linhas.add(new LinhaReceita(mp.getNome(), mp.getUnidade().getSimbolo(), item.getQuantidade(),
                necessaria, mp.getQuantidadeAtual(), mp.getQuantidadeAtual().compareTo(necessaria) >= 0,
                custoUnit, custo));
        }
        BigDecimal total = linhas.stream().map(LinhaReceita::custo).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal porUnidade = qtdOrdem.signum() > 0
            ? total.divide(qtdOrdem, 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        return new ReceitaCalculada(linhas, total, porUnidade,
            linhas.stream().filter(l -> !l.estoqueOk()).count(),
            linhas.stream().filter(l -> l.custoUnitario() == null || l.custoUnitario().signum() == 0).count());
    }

    @Transactional(readOnly = true)
    public java.util.Map<StatusOrdem, List<OrdemProducao>> listarKanban() {
        java.util.Map<StatusOrdem, List<OrdemProducao>> mapa = new java.util.LinkedHashMap<>();
        mapa.put(StatusOrdem.PENDENTE, colunaKanban(StatusOrdem.PENDENTE, 50));
        mapa.put(StatusOrdem.EM_ANDAMENTO, colunaKanban(StatusOrdem.EM_ANDAMENTO, 50));
        mapa.put(StatusOrdem.CONCLUIDA, colunaKanban(StatusOrdem.CONCLUIDA, 20));
        mapa.put(StatusOrdem.CANCELADA, colunaKanban(StatusOrdem.CANCELADA, 10));
        return mapa;
    }

    private List<OrdemProducao> colunaKanban(StatusOrdem status, int max) {
        return ordemRepository.findByStatusComDetalhes(status, PageRequest.of(0, max)).getContent();
    }

    // -----------------------------------------------------------------------
    // Geração de ordens ao confirmar pedido
    // -----------------------------------------------------------------------

    @Transactional
    public void gerarOrdens(Pedido pedido) {
        for (ItemPedido item : pedido.getItens()) {
            ordemRepository.save(OrdemProducao.builder()
                .pedido(pedido)
                .produto(item.getProduto())
                .quantidade(item.getQuantidade())
                .build());
        }
    }

    // -----------------------------------------------------------------------
    // Máquina de estados
    // -----------------------------------------------------------------------

    @Transactional
    public void iniciarProducao(Long id) {
        OrdemProducao ordem = buscarPorId(id);
        if (!ordem.getStatus().podeIniciar()) {
            throw new IllegalStateException(
                "Ordem não pode ser iniciada no status: " + ordem.getStatus().getDescricao());
        }
        ordem.setStatus(StatusOrdem.EM_ANDAMENTO);
        ordemRepository.save(ordem);
        publicarProducaoAtualizada(ordem);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void concluirOrdem(Long id) {
        OrdemProducao ordem = buscarPorId(id);
        if (!ordem.getStatus().podeConcluir()) {
            throw new IllegalStateException(
                "Ordem não pode ser concluída no status: " + ordem.getStatus().getDescricao());
        }

        FichaTecnica ficha = fichaTecnicaRepository.findByProdutoIdComItens(ordem.getProduto().getId())
            .orElseThrow(() -> new IllegalStateException(
                "Produto '" + ordem.getProduto().getNome() +
                "' não possui ficha técnica. Cadastre antes de registrar a produção."));

        if (ficha.getItens().isEmpty()) {
            throw new IllegalStateException(
                "A ficha técnica de '" + ordem.getProduto().getNome() +
                "' não possui ingredientes cadastrados.");
        }

        BigDecimal qtdOrdem = ordem.getQuantidade();
        BigDecimal rendimento = ficha.getRendimento();

        if (rendimento == null || rendimento.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException(
                "Rendimento da ficha técnica de '" + ordem.getProduto().getNome() +
                "' deve ser maior que zero. Corrija antes de concluir a produção.");
        }

        // Valida disponibilidade de TODAS as MPs antes de deduzir qualquer uma
        verificarDisponibilidadeMP(ficha.getItens(), qtdOrdem, rendimento, ordem.getProduto().getNome());

        String motivoBase = "Produção: " + Quantidades.formatar(ordem.getQuantidade(), ordem.getProduto().getUnidadeVenda()) +
                            " de " + ordem.getProduto().getNome() +
                            " | Ordem #" + ordem.getId();

        // Ordenação por ID da MP evita deadlock quando múltiplas MPs são bloqueadas em paralelo
        List<FichaTecnicaItem> itensOrdenados = ficha.getItens().stream()
            .sorted(java.util.Comparator.comparing(i -> i.getMateriaPrima().getId()))
            .toList();

        for (FichaTecnicaItem item : itensOrdenados) {
            BigDecimal qtdNecessaria = item.getQuantidade()
                .multiply(qtdOrdem)
                .divide(rendimento, 4, RoundingMode.HALF_UP);
            estoqueService.registrarSaida(item.getMateriaPrima().getId(), qtdNecessaria, motivoBase);
        }

        ordem.setStatus(StatusOrdem.CONCLUIDA);
        ordem.setDataConclusao(LocalDateTime.now());
        ordemRepository.save(ordem);
        publicarProducaoAtualizada(ordem);
    }

    private void publicarProducaoAtualizada(OrdemProducao ordem) {
        if (ordem.getPedido() != null) {
            eventPublisher.publishEvent(new ProducaoAtualizadaEvent(ordem.getPedido().getId()));
        }
    }

    private void verificarDisponibilidadeMP(List<FichaTecnicaItem> itens, BigDecimal qtdOrdem,
                                             BigDecimal rendimento, String nomeProduto) {
        List<String> insuficientes = new ArrayList<>();
        for (FichaTecnicaItem item : itens) {
            BigDecimal necessario = item.getQuantidade()
                .multiply(qtdOrdem)
                .divide(rendimento, 4, RoundingMode.HALF_UP);
            BigDecimal disponivel = item.getMateriaPrima().getQuantidadeAtual();
            if (disponivel.compareTo(necessario) < 0) {
                insuficientes.add(String.format(
                    "'%s': precisa %.3f %s, disponível %.3f %s",
                    item.getMateriaPrima().getNome(),
                    necessario, item.getMateriaPrima().getUnidade().getSimbolo(),
                    disponivel, item.getMateriaPrima().getUnidade().getSimbolo()
                ));
            }
        }
        if (!insuficientes.isEmpty()) {
            throw new EstoqueInsuficienteException(
                "Estoque insuficiente para produzir '" + nomeProduto + "'. " +
                "Faça uma entrada de estoque antes de continuar:\n" +
                String.join("\n", insuficientes)
            );
        }
    }

    @Transactional
    public void cancelarOrdem(Long id) {
        OrdemProducao ordem = buscarPorId(id);
        if (!ordem.getStatus().podeCancelar()) {
            throw new IllegalStateException(
                "Ordem não pode ser cancelada no status: " + ordem.getStatus().getDescricao());
        }
        boolean estaEmAndamento = ordem.getStatus() == StatusOrdem.EM_ANDAMENTO;
        ordem.setStatus(StatusOrdem.CANCELADA);
        ordemRepository.save(ordem);
        publicarProducaoAtualizada(ordem);

        if (estaEmAndamento) {
            alertaInternoService.criar(
                "Ordem de produção #" + ordem.getId() + " cancelada — o pedido #" +
                ordem.getPedido().getId() + " (" + ordem.getProduto().getNome() +
                ") foi cancelado enquanto estava em andamento.",
                "/producao/" + ordem.getId(),
                "bi-clipboard2-x",
                "danger"
            );
        }
    }
}
