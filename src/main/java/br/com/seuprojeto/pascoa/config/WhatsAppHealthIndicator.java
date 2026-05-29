package br.com.seuprojeto.pascoa.config;

import br.com.seuprojeto.pascoa.notificacao.entity.CanalNotificacao;
import br.com.seuprojeto.pascoa.notificacao.entity.ConfiguracaoCanal;
import br.com.seuprojeto.pascoa.notificacao.repository.ConfiguracaoCanalRepository;
import br.com.seuprojeto.pascoa.notificacao.service.WhatsAppService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Health indicator customizado para verificar a conectividade com a Evolution API (WhatsApp).
 * Exposto em /actuator/health como componente "whatsapp".
 */
@Component("whatsapp")
@RequiredArgsConstructor
public class WhatsAppHealthIndicator implements HealthIndicator {

    private final WhatsAppService whatsAppService;
    private final ConfiguracaoCanalRepository canalRepository;

    @Override
    public Health health() {
        Optional<ConfiguracaoCanal> optConfig = canalRepository.findByTipo(CanalNotificacao.WHATSAPP);

        if (optConfig.isEmpty()) {
            return Health.unknown()
                .withDetail("motivo", "Canal WhatsApp não configurado no banco.")
                .build();
        }

        ConfiguracaoCanal config = optConfig.get();

        if (!Boolean.TRUE.equals(config.getAtivo())) {
            return Health.unknown()
                .withDetail("motivo", "Canal WhatsApp desativado.")
                .build();
        }

        if (Boolean.TRUE.equals(config.getTestMode())) {
            return Health.up()
                .withDetail("modo", "test — envio real desabilitado")
                .withDetail("instancia", config.getRemetente())
                .build();
        }

        try {
            var resultado = whatsAppService.verificarConexao(config);
            if (resultado.isConectado()) {
                return Health.up()
                    .withDetail("instancia", config.getRemetente())
                    .withDetail("estado", resultado.getEstado())
                    .build();
            } else {
                return Health.down()
                    .withDetail("instancia", config.getRemetente())
                    .withDetail("estado", resultado.getEstado())
                    .withDetail("mensagem", resultado.getMensagem())
                    .build();
            }
        } catch (Exception e) {
            return Health.down(e)
                .withDetail("instancia", config.getRemetente())
                .withDetail("mensagem", "Erro ao conectar com a Evolution API: " + e.getMessage())
                .build();
        }
    }
}
