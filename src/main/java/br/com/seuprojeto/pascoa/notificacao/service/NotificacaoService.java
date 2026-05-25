package br.com.seuprojeto.pascoa.notificacao.service;

import br.com.seuprojeto.pascoa.notificacao.entity.*;
import br.com.seuprojeto.pascoa.notificacao.repository.ConfiguracaoCanalRepository;
import br.com.seuprojeto.pascoa.notificacao.repository.NotificacaoEnviadaRepository;
import br.com.seuprojeto.pascoa.notificacao.repository.TemplateNotificacaoRepository;
import br.com.seuprojeto.pascoa.pedido.entity.Pedido;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * Orquestra o envio de notificações: consulta templates, substitui variáveis,
 * delega ao canal correto e persiste o registro de envio.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificacaoService {

    private final TemplateNotificacaoRepository templateRepository;
    private final NotificacaoEnviadaRepository enviadaRepository;
    private final ConfiguracaoCanalRepository canalRepository;
    private final WhatsAppService whatsAppService;
    private final EmailService emailService;

    private static final DateTimeFormatter FMT_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /**
     * Processa o evento de status do pedido:
     * para cada canal configurado e ativo, busca o template correspondente,
     * interpola as variáveis e envia a notificação ao cliente.
     */
    @Transactional
    public void processar(Pedido pedido, EventoNotificacao evento) {
        for (CanalNotificacao canal : CanalNotificacao.values()) {
            processarCanal(pedido, evento, canal);
        }
    }

    private void processarCanal(Pedido pedido, EventoNotificacao evento, CanalNotificacao canal) {
        // 1. Verifica se o canal está configurado e ativo
        Optional<ConfiguracaoCanal> optConfig = canalRepository.findByTipo(canal);
        if (optConfig.isEmpty() || !Boolean.TRUE.equals(optConfig.get().getAtivo())) {
            return;
        }
        ConfiguracaoCanal config = optConfig.get();

        // 2. Verifica se o cliente tem opt-in e preferência de canal
        var cliente = pedido.getCliente();
        if (!Boolean.TRUE.equals(cliente.getOptIn())) {
            return;
        }
        var preferencia = cliente.getPreferenciaCanal();
        if (preferencia == null) return; // sem preferência definida → não envia
        boolean quereEsteCanal = switch (preferencia) {
            case WHATSAPP -> canal == CanalNotificacao.WHATSAPP;
            case EMAIL    -> canal == CanalNotificacao.EMAIL;
            case AMBOS    -> true;
            case NENHUM   -> false;
        };
        if (!quereEsteCanal) return;

        // 3. Busca template ativo para o evento + canal
        Optional<TemplateNotificacao> optTemplate = templateRepository
                .findByEventoGatilhoAndCanalAndAtivoTrue(evento, canal);
        if (optTemplate.isEmpty()) {
            log.debug("[NOTIF] Nenhum template ativo para evento={} canal={}", evento, canal);
            return;
        }
        TemplateNotificacao template = optTemplate.get();

        // 4. Interpola variáveis no corpo
        String destinatario = resolverDestinatario(cliente, canal);
        if (destinatario == null || destinatario.isBlank()) {
            log.warn("[NOTIF] Cliente {} sem destinatário para canal {}", cliente.getId(), canal);
            return;
        }
        String corpo = interpolar(template.getCorpo(), pedido);
        String assunto = template.getAssunto() != null ? interpolar(template.getAssunto(), pedido) : "";

        // 5. Envia e persiste resultado
        StatusEnvio statusEnvio = StatusEnvio.ENVIADA;
        String mensagemErro = null;
        try {
            if (canal == CanalNotificacao.WHATSAPP) {
                whatsAppService.enviar(config, destinatario, corpo);
            } else {
                emailService.enviar(config, destinatario, assunto, corpo);
            }
        } catch (Exception e) {
            statusEnvio = StatusEnvio.FALHA;
            mensagemErro = e.getMessage();
            log.error("[NOTIF] Falha ao enviar notificação pedido={} canal={}: {}", pedido.getId(), canal, e.getMessage());
        }

        enviadaRepository.save(NotificacaoEnviada.builder()
                .pedido(pedido)
                .template(template)
                .canal(canal)
                .destinatario(destinatario)
                .dataEnvio(LocalDateTime.now())
                .status(statusEnvio)
                .mensagemErro(mensagemErro)
                .build());
    }

    private String resolverDestinatario(br.com.seuprojeto.pascoa.cadastro.entity.Cliente cliente,
                                        CanalNotificacao canal) {
        return switch (canal) {
            case WHATSAPP -> cliente.getTelefone();
            case EMAIL    -> cliente.getEmail();
        };
    }

    /**
     * Substitui placeholders no template:
     * {nome}, {numeroPedido}, {dataEntrega}, {link}, {valor}
     */
    private String interpolar(String texto, Pedido pedido) {
        if (texto == null) return "";
        var cliente = pedido.getCliente();
        String dataEntrega = pedido.getDataEntrega() != null
                ? pedido.getDataEntrega().format(FMT_DATA) : "-";
        String link = "http://localhost:8080/acompanhamento/" + pedido.getTokenAcompanhamento();
        String valor = pedido.getTotalPedido() != null
                ? "R$ " + pedido.getTotalPedido().toPlainString().replace(".", ",") : "-";

        return texto
                .replace("{nome}", cliente.getNome())
                .replace("{numeroPedido}", String.valueOf(pedido.getId()))
                .replace("{dataEntrega}", dataEntrega)
                .replace("{link}", link)
                .replace("{valor}", valor);
    }
}
