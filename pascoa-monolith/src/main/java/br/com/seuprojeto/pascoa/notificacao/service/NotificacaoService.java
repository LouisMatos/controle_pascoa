package br.com.seuprojeto.pascoa.notificacao.service;

import br.com.seuprojeto.pascoa.cadastro.entity.Cliente;
import br.com.seuprojeto.pascoa.notificacao.entity.*;
import br.com.seuprojeto.pascoa.notificacao.repository.ConfiguracaoCanalRepository;
import br.com.seuprojeto.pascoa.notificacao.repository.NotificacaoEnviadaRepository;
import br.com.seuprojeto.pascoa.notificacao.repository.TemplateNotificacaoRepository;
import br.com.seuprojeto.pascoa.orcamento.entity.Orcamento;
import br.com.seuprojeto.pascoa.pedido.entity.Pedido;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Orquestra o envio de notificações: consulta templates, substitui variáveis,
 * delega ao canal correto e persiste o registro de envio.
 *
 * <p>Pontos de entrada:
 * <ul>
 *   <li>{@link #processar(Pedido, EventoNotificacao)} — eventos ligados a pedidos</li>
 *   <li>{@link #processarParaCliente(Cliente, EventoNotificacao)} — ex: aniversário</li>
 *   <li>{@link #processarParaOrcamento(Orcamento, EventoNotificacao)} — ex: orçamento expirando</li>
 * </ul>
 *
 * <p>SMS fallback (Item 25): quando o envio via WhatsApp falha e o canal SMS está ativo,
 * o serviço tenta um segundo envio via SMS automaticamente.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificacaoService {

    private final TemplateNotificacaoRepository templateRepository;
    private final NotificacaoEnviadaRepository  enviadaRepository;
    private final ConfiguracaoCanalRepository   canalRepository;
    private final WhatsAppService               whatsAppService;
    private final EmailService                  emailService;
    private final SmsService                    smsService;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    private static final DateTimeFormatter FMT_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // ══════════════════════════════════════════════════════════════════════════
    // Pontos de entrada públicos
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Processa evento de status de pedido — para todos os canais configurados.
     */
    @Transactional
    public void processar(Pedido pedido, EventoNotificacao evento) {
        Cliente cliente = pedido.getCliente();
        Map<String, String> vars = variaveisPedido(pedido);
        for (CanalNotificacao canal : CanalNotificacao.values()) {
            processarCanal(cliente, pedido, null, evento, canal, vars);
        }
    }

    /**
     * Item 25 — Processa notificação vinculada diretamente ao cliente (sem pedido).
     * Exemplo: ANIVERSARIO_CLIENTE.
     */
    @Transactional
    public void processarParaCliente(Cliente cliente, EventoNotificacao evento) {
        Map<String, String> vars = variaveisCliente(cliente);
        for (CanalNotificacao canal : CanalNotificacao.values()) {
            if (canal == CanalNotificacao.SMS) continue; // SMS só como fallback
            processarCanal(cliente, null, null, evento, canal, vars);
        }
    }

    /**
     * Item 25 — Processa notificação vinculada a um orçamento (sem pedido).
     * Exemplo: ORCAMENTO_EXPIRANDO.
     */
    @Transactional
    public void processarParaOrcamento(Orcamento orcamento, EventoNotificacao evento) {
        Cliente cliente = orcamento.getCliente();
        Map<String, String> vars = variaveisOrcamento(orcamento);
        for (CanalNotificacao canal : CanalNotificacao.values()) {
            if (canal == CanalNotificacao.SMS) continue; // SMS só como fallback
            processarCanal(cliente, null, orcamento, evento, canal, vars);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Núcleo de processamento de canal
    // ══════════════════════════════════════════════════════════════════════════

    private void processarCanal(Cliente cliente,
                                 Pedido pedido,
                                 Orcamento orcamento,
                                 EventoNotificacao evento,
                                 CanalNotificacao canal,
                                 Map<String, String> vars) {

        // 0. Idempotência: evita reenvio para (pedido, evento, canal)
        if (pedido != null
                && enviadaRepository.existsByPedidoIdAndEventoAndCanalAndStatus(
                        pedido.getId(), evento, canal, StatusEnvio.ENVIADA)) {
            log.debug("[NOTIF] Duplicata ignorada: pedido={} evento={} canal={}",
                    pedido.getId(), evento, canal);
            return;
        }

        // Idempotência para orçamento expirando
        if (orcamento != null
                && enviadaRepository.existsByOrcamentoIdAndEventoAndCanalAndStatus(
                        orcamento.getId(), evento, canal, StatusEnvio.ENVIADA)) {
            log.debug("[NOTIF] Duplicata ignorada: orcamento={} evento={} canal={}",
                    orcamento.getId(), evento, canal);
            return;
        }

        // Idempotência para aniversário (um por cliente por canal por ano)
        if (evento == EventoNotificacao.ANIVERSARIO_CLIENTE) {
            int anoAtual = LocalDate.now().getYear();
            if (enviadaRepository.jaEnviouAniversarioNoAno(
                    cliente.getId(), evento.name(), canal.name(), anoAtual)) {
                log.debug("[NOTIF] Aniversário já enviado este ano: cliente={} canal={}",
                        cliente.getId(), canal);
                return;
            }
        }

        // 1. Canal configurado e ativo?
        Optional<ConfiguracaoCanal> optConfig = canalRepository.findByTipo(canal);
        if (optConfig.isEmpty() || !Boolean.TRUE.equals(optConfig.get().getAtivo())) {
            return;
        }
        ConfiguracaoCanal config = optConfig.get();

        // 2. Cliente com opt-in e preferência de canal?
        if (!Boolean.TRUE.equals(cliente.getOptIn())) return;
        var preferencia = cliente.getPreferenciaCanal();
        if (preferencia == null) return;
        boolean quereEsteCanal = switch (preferencia) {
            case WHATSAPP -> canal == CanalNotificacao.WHATSAPP;
            case EMAIL    -> canal == CanalNotificacao.EMAIL;
            case AMBOS    -> canal != CanalNotificacao.SMS; // SMS apenas como fallback
            case NENHUM   -> false;
        };
        if (!quereEsteCanal) return;

        // 3. Template ativo para evento + canal
        Optional<TemplateNotificacao> optTemplate =
                templateRepository.findByEventoGatilhoAndCanalAndAtivoTrue(evento, canal);
        if (optTemplate.isEmpty()) {
            log.debug("[NOTIF] Sem template ativo para evento={} canal={}", evento, canal);
            return;
        }
        TemplateNotificacao template = optTemplate.get();

        // 4. Destinatário
        String destinatario = resolverDestinatario(cliente, canal);
        if (destinatario == null || destinatario.isBlank()) {
            log.warn("[NOTIF] Cliente {} sem destinatário para canal {}", cliente.getId(), canal);
            return;
        }

        // 5. Interpolação
        String corpo   = interpolar(template.getCorpo(), vars);
        String assunto = template.getAssunto() != null ? interpolar(template.getAssunto(), vars) : "";

        // 6. Envio e persistência
        StatusEnvio statusEnvio = StatusEnvio.ENVIADA;
        String mensagemErro = null;
        boolean whatsAppFalhou = false;

        try {
            switch (canal) {
                case WHATSAPP -> whatsAppService.enviar(config, destinatario, corpo);
                case EMAIL    -> emailService.enviar(config, destinatario, assunto, corpo);
                case SMS      -> smsService.enviar(config, destinatario, corpo);
            }
        } catch (Exception e) {
            statusEnvio  = StatusEnvio.FALHA;
            mensagemErro = e.getMessage();
            whatsAppFalhou = (canal == CanalNotificacao.WHATSAPP);
            log.error("[NOTIF] Falha ao enviar: pedido={} orcamento={} canal={} erro={}",
                    pedido != null ? pedido.getId() : "-",
                    orcamento != null ? orcamento.getId() : "-",
                    canal, e.getMessage());
        }

        persistirEnvio(template, cliente, pedido, orcamento, evento,
                canal, destinatario, statusEnvio, mensagemErro);

        // 7. SMS fallback: se WhatsApp falhou e canal SMS está disponível
        if (whatsAppFalhou && cliente.getTelefone() != null && !cliente.getTelefone().isBlank()) {
            tentarSmsFallback(cliente, pedido, orcamento, evento, vars);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // SMS fallback
    // ══════════════════════════════════════════════════════════════════════════

    private void tentarSmsFallback(Cliente cliente, Pedido pedido, Orcamento orcamento,
                                    EventoNotificacao evento, Map<String, String> vars) {
        Optional<ConfiguracaoCanal> optSms = canalRepository.findByTipo(CanalNotificacao.SMS);
        if (optSms.isEmpty() || !Boolean.TRUE.equals(optSms.get().getAtivo())) {
            log.debug("[SMS-FALLBACK] Canal SMS inativo ou não configurado — fallback ignorado");
            return;
        }
        Optional<TemplateNotificacao> optTemplate =
                templateRepository.findByEventoGatilhoAndCanalAndAtivoTrue(evento, CanalNotificacao.SMS);
        if (optTemplate.isEmpty()) {
            // Sem template SMS específico: usa o corpo do WhatsApp (já interpolado)
            log.debug("[SMS-FALLBACK] Sem template SMS para evento={} — sem fallback", evento);
            return;
        }

        String destinatario = resolverDestinatario(cliente, CanalNotificacao.SMS);
        if (destinatario == null || destinatario.isBlank()) return;

        String corpo = interpolar(optTemplate.get().getCorpo(), vars);
        StatusEnvio statusEnvio = StatusEnvio.ENVIADA;
        String mensagemErro = null;

        try {
            smsService.enviar(optSms.get(), destinatario, corpo);
            log.info("[SMS-FALLBACK] SMS enviado para cliente={} evento={}", cliente.getId(), evento);
        } catch (Exception e) {
            statusEnvio  = StatusEnvio.FALHA;
            mensagemErro = e.getMessage();
            log.error("[SMS-FALLBACK] Falha no SMS fallback para cliente={}: {}",
                    cliente.getId(), e.getMessage());
        }

        persistirEnvio(optTemplate.get(), cliente, pedido, orcamento, evento,
                CanalNotificacao.SMS, destinatario, statusEnvio, mensagemErro);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Helpers internos
    // ══════════════════════════════════════════════════════════════════════════

    private void persistirEnvio(TemplateNotificacao template,
                                 Cliente cliente,
                                 Pedido pedido,
                                 Orcamento orcamento,
                                 EventoNotificacao evento,
                                 CanalNotificacao canal,
                                 String destinatario,
                                 StatusEnvio status,
                                 String mensagemErro) {
        enviadaRepository.save(NotificacaoEnviada.builder()
                .pedido(pedido)
                .cliente(cliente)
                .orcamento(orcamento)
                .template(template)
                .evento(evento)
                .canal(canal)
                .destinatario(destinatario)
                .dataEnvio(LocalDateTime.now())
                .status(status)
                .mensagemErro(mensagemErro)
                .build());
    }

    private String resolverDestinatario(Cliente cliente, CanalNotificacao canal) {
        return switch (canal) {
            case WHATSAPP, SMS -> cliente.getTelefone();
            case EMAIL         -> cliente.getEmail();
        };
    }

    /** Interpola placeholders {@code {chave}} usando o mapa de variáveis. */
    private String interpolar(String texto, Map<String, String> vars) {
        if (texto == null) return "";
        String result = texto;
        for (Map.Entry<String, String> entry : vars.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return result;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Construtores de variáveis por contexto
    // ══════════════════════════════════════════════════════════════════════════

    private Map<String, String> variaveisPedido(Pedido pedido) {
        var cliente = pedido.getCliente();
        String dataEntrega = pedido.getDataEntrega() != null
                ? pedido.getDataEntrega().format(FMT_DATA) : "-";
        String link  = baseUrl + "/acompanhamento/" + pedido.getTokenAcompanhamento();
        String valor = pedido.getTotalPedido() != null
                ? "R$ " + pedido.getTotalPedido().toPlainString().replace(".", ",") : "-";

        Map<String, String> vars = new HashMap<>();
        vars.put("nome",          cliente.getNome());
        vars.put("numeroPedido",  String.valueOf(pedido.getId()));
        vars.put("dataEntrega",   dataEntrega);
        vars.put("link",          link);
        vars.put("valor",         valor);
        return vars;
    }

    private Map<String, String> variaveisCliente(Cliente cliente) {
        Map<String, String> vars = new HashMap<>();
        vars.put("nome", cliente.getNome());
        if (cliente.getDataNascimento() != null) {
            vars.put("dataAniversario", cliente.getDataNascimento().format(FMT_DATA));
        }
        return vars;
    }

    private Map<String, String> variaveisOrcamento(Orcamento orcamento) {
        var cliente = orcamento.getCliente();
        String link    = baseUrl + "/orcamento-publico/" + orcamento.getTokenAprovacao();
        String validade = orcamento.getValidade() != null
                ? orcamento.getValidade().format(FMT_DATA) : "-";
        String valor   = orcamento.getTotal() != null
                ? "R$ " + orcamento.getTotal().toPlainString().replace(".", ",") : "-";

        Map<String, String> vars = new HashMap<>();
        vars.put("nome",            cliente.getNome());
        vars.put("numeroOrcamento", String.valueOf(orcamento.getId()));
        vars.put("validade",        validade);
        vars.put("valor",           valor);
        vars.put("link",            link);
        return vars;
    }
}
