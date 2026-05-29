package br.com.seuprojeto.pascoa.crm.service;

import br.com.seuprojeto.pascoa.crm.dto.ClienteCrmDto;
import br.com.seuprojeto.pascoa.crm.entity.CampanhaItem;
import br.com.seuprojeto.pascoa.crm.entity.SegmentoCliente;
import br.com.seuprojeto.pascoa.notificacao.entity.CanalNotificacao;
import br.com.seuprojeto.pascoa.notificacao.entity.ConfiguracaoCanal;
import br.com.seuprojeto.pascoa.notificacao.repository.ConfiguracaoCanalRepository;
import br.com.seuprojeto.pascoa.notificacao.service.EmailService;
import br.com.seuprojeto.pascoa.notificacao.service.WhatsAppService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CampanhaService {

    private final CrmService crmService;
    private final CampanhaQueue campanhaQueue;
    private final ConfiguracaoCanalRepository canalRepository;
    private final WhatsAppService whatsAppService;
    private final EmailService emailService;

    /**
     * Carrega os clientes do segmento (null = todos), filtra por opt-in e
     * destinatário válido para o canal, e enfileira cada envio.
     *
     * @return quantidade de destinatários enfileirados
     */
    public int disparar(SegmentoCliente segmento, CanalNotificacao canal,
                        String assunto, String mensagem) {
        List<ClienteCrmDto> clientes = crmService.gerarRanking();

        int count = 0;
        for (ClienteCrmDto dto : clientes) {
            if (segmento != null && dto.segmento() != segmento) continue;
            if (!Boolean.TRUE.equals(dto.cliente().getOptIn())) continue;

            String destinatario = switch (canal) {
                case WHATSAPP, SMS -> dto.cliente().getTelefone();
                case EMAIL         -> dto.cliente().getEmail();
            };
            if (destinatario == null || destinatario.isBlank()) continue;

            campanhaQueue.enqueue(new CampanhaItem(
                    dto.cliente().getId(),
                    dto.cliente().getNome(),
                    destinatario,
                    canal,
                    assunto,
                    mensagem));
            count++;
        }
        log.info("[CAMPANHA] {} mensagens enfileiradas | canal={} segmento={}", count, canal, segmento);
        return count;
    }

    /** Worker: processa 1 item a cada 6 segundos = 10 envios/minuto. */
    @Scheduled(fixedRate = 6_000)
    @SchedulerLock(name = "campanha_processarProximo", lockAtMostFor = "PT10S", lockAtLeastFor = "PT5S")
    public void processarProximo() {
        CampanhaItem item = campanhaQueue.poll();
        if (item == null) return;

        Optional<ConfiguracaoCanal> optConfig = canalRepository.findByTipo(item.canal());
        if (optConfig.isEmpty() || !Boolean.TRUE.equals(optConfig.get().getAtivo())) {
            log.warn("[CAMPANHA] Canal {} inativo — descartando envio para {}", item.canal(), item.destinatario());
            campanhaQueue.registrarFalha();
            return;
        }
        ConfiguracaoCanal config = optConfig.get();

        try {
            if (item.canal() == CanalNotificacao.WHATSAPP) {
                whatsAppService.enviar(config, item.destinatario(), item.mensagem());
            } else {
                String assunto = item.assunto() != null ? item.assunto() : "(sem assunto)";
                emailService.enviar(config, item.destinatario(), assunto, item.mensagem());
            }
            campanhaQueue.registrarEnvio();
            log.debug("[CAMPANHA] Enviado para {} via {}", item.destinatario(), item.canal());
        } catch (Exception e) {
            campanhaQueue.registrarFalha();
            log.error("[CAMPANHA] Falha ao enviar para {} via {}: {}", item.destinatario(), item.canal(), e.getMessage());
        }
    }
}
