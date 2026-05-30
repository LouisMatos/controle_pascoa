package br.com.seuprojeto.pascoa.qualidade.service;

import br.com.seuprojeto.pascoa.cadastro.entity.Produto;
import br.com.seuprojeto.pascoa.cadastro.repository.ProdutoRepository;
import br.com.seuprojeto.pascoa.notificacao.event.InspecaoReprovadaEvent;
import br.com.seuprojeto.pascoa.producao.entity.OrdemProducao;
import br.com.seuprojeto.pascoa.producao.repository.OrdemProducaoRepository;
import br.com.seuprojeto.pascoa.qualidade.dto.ItemVerificadoDto;
import br.com.seuprojeto.pascoa.qualidade.entity.ChecklistItem;
import br.com.seuprojeto.pascoa.qualidade.entity.InspecaoQualidade;
import br.com.seuprojeto.pascoa.qualidade.repository.ChecklistItemRepository;
import br.com.seuprojeto.pascoa.qualidade.repository.InspecaoRepository;
import br.com.seuprojeto.pascoa.shared.exception.RecursoNaoEncontradoException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class QualidadeService {

    private final ChecklistItemRepository checklistRepo;
    private final InspecaoRepository inspecaoRepo;
    private final ProdutoRepository produtoRepo;
    private final OrdemProducaoRepository ordemRepo;
    private final ApplicationEventPublisher eventPublisher;

    // ── Checklist ─────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ChecklistItem> listarChecklist(Long produtoId) {
        return checklistRepo.findByProdutoIdOrderByOrdemAscIdAsc(produtoId);
    }

    @Transactional(readOnly = true)
    public List<ChecklistItem> listarChecklistAtivo(Long produtoId) {
        return checklistRepo.findByProdutoIdAndAtivoTrueOrderByOrdemAscIdAsc(produtoId);
    }

    @Transactional
    public ChecklistItem adicionarItem(Long produtoId, String descricao) {
        Produto produto = produtoRepo.findById(produtoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Produto não encontrado: " + produtoId));

        long proxOrdem = checklistRepo.countByProdutoId(produtoId);
        ChecklistItem item = ChecklistItem.builder()
                .produto(produto)
                .item(descricao.strip())
                .ordem((int) proxOrdem)
                .build();
        return checklistRepo.save(item);
    }

    @Transactional
    public void excluirItem(Long itemId) {
        checklistRepo.deleteById(itemId);
    }

    @Transactional
    public void toggleAtivo(Long itemId) {
        ChecklistItem item = checklistRepo.findById(itemId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Item não encontrado: " + itemId));
        item.setAtivo(!item.getAtivo());
        checklistRepo.save(item);
    }

    // ── Inspeção ──────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<InspecaoQualidade> listarInspecoes() {
        return inspecaoRepo.findAllComDetalhes();
    }

    @Transactional(readOnly = true)
    public InspecaoQualidade buscarInspecao(Long id) {
        return inspecaoRepo.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Inspeção não encontrada: " + id));
    }

    @Transactional(readOnly = true)
    public Optional<InspecaoQualidade> buscarInspecaoPorOrdem(Long ordemId) {
        return inspecaoRepo.findByOrdemProducaoId(ordemId);
    }

    @Transactional(readOnly = true)
    public OrdemProducao buscarOrdem(Long ordemId) {
        return ordemRepo.findByIdComDetalhes(ordemId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Ordem de produção não encontrada: " + ordemId));
    }

    @Transactional
    public InspecaoQualidade registrarInspecao(Long ordemId,
                                               String inspetor,
                                               boolean aprovado,
                                               String observacoes,
                                               List<Long> idsVerificados) {
        OrdemProducao ordem = buscarOrdem(ordemId);

        // Constrói a lista de itens verificados a partir do checklist ativo
        List<ChecklistItem> checklist = listarChecklistAtivo(ordem.getProduto().getId());
        Set<Long> verificadosSet = idsVerificados == null ? Set.of() : Set.copyOf(idsVerificados);

        List<ItemVerificadoDto> itens = checklist.stream()
                .map(ci -> new ItemVerificadoDto(ci.getId(), ci.getItem(), verificadosSet.contains(ci.getId())))
                .toList();

        InspecaoQualidade inspecao = InspecaoQualidade.builder()
                .ordemProducao(ordem)
                .inspetor(inspetor.strip())
                .aprovado(aprovado)
                .observacoes(observacoes != null ? observacoes.strip() : null)
                .itensVerificados(itens.isEmpty() ? null : itens)
                .build();

        InspecaoQualidade salva = inspecaoRepo.save(inspecao);

        // Publica alerta interno quando a inspeção é reprovada
        if (!aprovado) {
            eventPublisher.publishEvent(new InspecaoReprovadaEvent(salva));
        }

        return salva;
    }

    // ── KPIs ─────────────────────────────────────────────────────────────

    public long totalAprovadas() { return inspecaoRepo.countByAprovadoTrue(); }
    public long totalReprovadas() { return inspecaoRepo.countByAprovadoFalse(); }
}
