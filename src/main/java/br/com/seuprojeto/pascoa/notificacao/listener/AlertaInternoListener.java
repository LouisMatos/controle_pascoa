package br.com.seuprojeto.pascoa.notificacao.listener;

import br.com.seuprojeto.pascoa.notificacao.event.InspecaoReprovadaEvent;
import br.com.seuprojeto.pascoa.notificacao.event.OrcamentoAcaoEvent;
import br.com.seuprojeto.pascoa.notificacao.service.AlertaInternoService;
import br.com.seuprojeto.pascoa.qualidade.entity.InspecaoQualidade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AlertaInternoListener {

    private final AlertaInternoService alertaService;

    // ── Orçamento aprovado / recusado ────────────────────────────────────────

    @Async
    @EventListener
    public void onOrcamentoAcao(OrcamentoAcaoEvent event) {
        var orc = event.orcamento();
        String nomeCliente = orc.getCliente() != null ? orc.getCliente().getNome() : "Cliente";

        if (event.aprovado()) {
            alertaService.criar(
                    "Orçamento #" + orc.getId() + " aprovado por " + nomeCliente + "!",
                    "/orcamentos/" + orc.getId(),
                    "bi-file-earmark-check-fill",
                    "success"
            );
        } else {
            alertaService.criar(
                    "Orçamento #" + orc.getId() + " recusado por " + nomeCliente + ".",
                    "/orcamentos/" + orc.getId(),
                    "bi-file-earmark-x-fill",
                    "warning"
            );
        }
        log.info("[ALERTA] Orçamento #{} {} por {}", orc.getId(),
                event.aprovado() ? "aprovado" : "recusado", nomeCliente);
    }

    // ── Inspeção reprovada ────────────────────────────────────────────────────

    @Async
    @EventListener
    public void onInspecaoReprovada(InspecaoReprovadaEvent event) {
        InspecaoQualidade ins = event.inspecao();
        String produto = ins.getOrdemProducao().getProduto().getNome();
        String ordemId = String.valueOf(ins.getOrdemProducao().getId());

        alertaService.criar(
                "⚠️ Inspeção #" + ins.getId() + " REPROVADA — " + produto
                        + " (Ordem #" + ordemId + ")",
                "/qualidade/inspecao/" + ins.getId(),
                "bi-clipboard2-x-fill",
                "danger"
        );
        log.warn("[ALERTA] Inspeção #{} reprovada — produto: {}", ins.getId(), produto);
    }
}
