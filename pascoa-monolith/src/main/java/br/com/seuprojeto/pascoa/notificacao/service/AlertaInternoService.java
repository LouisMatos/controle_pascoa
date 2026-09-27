package br.com.seuprojeto.pascoa.notificacao.service;

import br.com.seuprojeto.pascoa.notificacao.entity.AlertaInterno;
import br.com.seuprojeto.pascoa.notificacao.repository.AlertaInternoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AlertaInternoService {

    private final AlertaInternoRepository repository;

    // ── Criação ──────────────────────────────────────────────────────────────

    @Transactional
    public AlertaInterno criar(String mensagem, String link, String icone, String cor) {
        return repository.save(AlertaInterno.builder()
                .mensagem(mensagem)
                .link(link)
                .icone(icone)
                .cor(cor)
                .build());
    }

    // ── Consultas ────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public long contarNaoLidos() {
        return repository.countByLidoFalse();
    }

    /** Top 20 para o dropdown do navbar (não lidos primeiro). */
    @Transactional(readOnly = true)
    public List<AlertaInterno> recentes() {
        return repository.findTop20ByOrderByLidoAscCriadoEmDesc();
    }

    /** Lista completa para a página de alertas. */
    @Transactional(readOnly = true)
    public List<AlertaInterno> todos() {
        return repository.findAllByOrderByCriadoEmDesc();
    }

    /** Apenas alertas não-lidos para filtro. */
    @Transactional(readOnly = true)
    public List<AlertaInterno> naoLidos() {
        return repository.findByLidoFalseOrderByCriadoEmDesc();
    }

    // ── Ações ─────────────────────────────────────────────────────────────────

    @Transactional
    public void marcarLido(Long id) {
        repository.findById(id).ifPresent(a -> {
            a.setLido(true);
            repository.save(a);
        });
    }

    @Transactional
    public void marcarTodasLidas() {
        repository.marcarTodasLidas();
    }

    @Transactional
    public void excluir(Long id) {
        repository.deleteById(id);
    }

    @Transactional
    public int excluirLidas() {
        return repository.excluirLidas();
    }
}
