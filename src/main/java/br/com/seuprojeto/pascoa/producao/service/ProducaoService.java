package br.com.seuprojeto.pascoa.producao.service;

import br.com.seuprojeto.pascoa.estoque.service.EstoqueService;
import br.com.seuprojeto.pascoa.fichaTecnica.entity.FichaTecnica;
import br.com.seuprojeto.pascoa.fichaTecnica.entity.FichaTecnicaItem;
import br.com.seuprojeto.pascoa.fichaTecnica.repository.FichaTecnicaRepository;
import br.com.seuprojeto.pascoa.notificacao.entity.EventoNotificacao;
import br.com.seuprojeto.pascoa.notificacao.event.PedidoStatusEvent;
import br.com.seuprojeto.pascoa.notificacao.service.AlertaInternoService;
import br.com.seuprojeto.pascoa.pedido.entity.ItemPedido;
import br.com.seuprojeto.pascoa.pedido.entity.Pedido;
import br.com.seuprojeto.pascoa.producao.entity.OrdemProducao;
import br.com.seuprojeto.pascoa.producao.entity.StatusOrdem;
import br.com.seuprojeto.pascoa.producao.repository.OrdemProducaoRepository;
import br.com.seuprojeto.pascoa.shared.exception.EstoqueInsuficienteException;
import br.com.seuprojeto.pascoa.shared.exception.RecursoNaoEncontradoException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
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
    public List<OrdemProducao> listarTodas() {
        return ordemRepository.findAllComDetalhes();
    }

    @Transactional(readOnly = true)
    public List<OrdemProducao> listarPorStatus(StatusOrdem status) {
        return ordemRepository.findByStatusComDetalhes(status);
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
    public Optional<FichaTecnica> buscarFicha(Long produtoId) {
        return fichaTecnicaRepository.findByProdutoIdComItens(produtoId);
    }

    /**
     * Retorna ordens agrupadas por status para o Kanban.
     * Colunas exibidas: PENDENTE, EM_ANDAMENTO, CONCLUIDA (as últimas 20).
     */
    @Transactional(readOnly = true)
    public java.util.Map<StatusOrdem, java.util.List<OrdemProducao>> listarKanban() {
        var todas = ordemRepository.findAllComDetalhes();
        // Mantém todas as PENDENTE e EM_ANDAMENTO; limita CONCLUIDA/CANCELADA às 20 mais recentes
        java.util.Map<StatusOrdem, java.util.List<OrdemProducao>> mapa = new java.util.LinkedHashMap<>();
        for (StatusOrdem s : StatusOrdem.values()) {
            mapa.put(s, new java.util.ArrayList<>());
        }
        todas.forEach(o -> mapa.get(o.getStatus()).add(o));
        // Limita concluídas e canceladas para não poluir o board
        limitarLista(mapa, StatusOrdem.CONCLUIDA, 20);
        limitarLista(mapa, StatusOrdem.CANCELADA, 10);
        return mapa;
    }

    private void limitarLista(java.util.Map<StatusOrdem, java.util.List<OrdemProducao>> mapa,
                               StatusOrdem status, int max) {
        var lista = mapa.get(status);
        if (lista.size() > max) {
            mapa.put(status, lista.subList(0, max));
        }
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
        eventPublisher.publishEvent(new PedidoStatusEvent(ordem.getPedido(), EventoNotificacao.PRODUCAO_INICIADA));
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

        BigDecimal qtdOrdem = BigDecimal.valueOf(ordem.getQuantidade());
        BigDecimal rendimento = ficha.getRendimento();

        if (rendimento == null || rendimento.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException(
                "Rendimento da ficha técnica de '" + ordem.getProduto().getNome() +
                "' deve ser maior que zero. Corrija antes de concluir a produção.");
        }

        // Valida disponibilidade de TODAS as MPs antes de deduzir qualquer uma
        verificarDisponibilidadeMP(ficha.getItens(), qtdOrdem, rendimento, ordem.getProduto().getNome());

        String motivoBase = "Produção: " + ordem.getQuantidade() +
                            "x " + ordem.getProduto().getNome() +
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
