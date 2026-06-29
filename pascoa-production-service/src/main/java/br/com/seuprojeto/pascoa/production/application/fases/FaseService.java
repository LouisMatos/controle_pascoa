package br.com.seuprojeto.pascoa.production.application.fases;

import br.com.seuprojeto.pascoa.production.adapter.in.rest.dto.FaseRequest;
import br.com.seuprojeto.pascoa.production.adapter.out.persistence.*;
import br.com.seuprojeto.pascoa.production.domain.exception.OrdemNotFoundException;
import br.com.seuprojeto.pascoa.production.domain.model.StatusOrdem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * v6 Etapa 9 — Avança a ordem entre fases livres definidas pelo tenant
 * (alinhadas ao config-engine §3.1 / §4 / §7).
 *
 * <p>O nome da fase é cru — não validamos contra config-engine aqui (essa integração
 * vem com o HTTP client em etapa posterior). A UI/monólito sabe a lista válida e
 * envia o nome correto.
 *
 * <p>Mapeamento automático para {@link StatusOrdem} legado:
 * <ul>
 *   <li>primeira mudança → {@code EM_ANDAMENTO} + iniciada_em</li>
 *   <li>fase com nome contendo "embal" / "expedi" / "entrega" e ordem máxima
 *       conhecida → considera fim do fluxo, mas a transição para {@code CONCLUIDA}
 *       continua sendo decisão explícita do caller via {@code ProducaoUseCase} legado.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class FaseService {

    private final OrdemJpaRepository ordemRepo;
    private final HistoricoFaseJpaRepository historicoRepo;

    public OrdemProducaoJpaEntity transicionar(Long ordemId, FaseRequest req) {
        OrdemProducaoJpaEntity ordem = ordemRepo.findById(ordemId)
                .orElseThrow(() -> new OrdemNotFoundException(ordemId));

        String faseAnterior = ordem.getFaseAtual();
        Integer ordemAnterior = ordem.getFaseAtual() != null ? ordem.getFaseOrdem() : null;

        ordem.setFaseAtual(req.fase());
        ordem.setFaseOrdem(req.ordem());

        // Status macro: primeira fase ativa → EM_ANDAMENTO
        if (ordem.getStatus() == StatusOrdem.PENDENTE) {
            ordem.setStatus(StatusOrdem.EM_ANDAMENTO);
            ordem.setIniciadaEm(LocalDateTime.now());
        }

        historicoRepo.save(HistoricoFaseJpaEntity.builder()
                .ordemId(ordemId)
                .faseDe(faseAnterior)
                .fasePara(req.fase())
                .ordemDe(ordemAnterior)
                .ordemPara(req.ordem())
                .mudadoPor(req.mudadoPor())
                .observacao(req.observacao())
                .build());

        log.info("Ordem {} transicionou {} (#{}) → {} (#{})",
                ordemId, faseAnterior, ordemAnterior, req.fase(), req.ordem());
        return ordem;
    }

    @Transactional(readOnly = true)
    public List<HistoricoFaseJpaEntity> historico(Long ordemId) {
        if (!ordemRepo.existsById(ordemId)) throw new OrdemNotFoundException(ordemId);
        return historicoRepo.findByOrdemIdOrderByMudadoEmAsc(ordemId);
    }
}
