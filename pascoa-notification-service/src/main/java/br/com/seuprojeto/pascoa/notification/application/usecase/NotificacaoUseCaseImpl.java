package br.com.seuprojeto.pascoa.notification.application.usecase;

import br.com.seuprojeto.pascoa.notification.application.port.in.NotificacaoUseCase;
import br.com.seuprojeto.pascoa.notification.application.port.out.*;
import br.com.seuprojeto.pascoa.notification.domain.exception.NotificacaoNotFoundException;
import br.com.seuprojeto.pascoa.notification.domain.model.Canal;
import br.com.seuprojeto.pascoa.notification.domain.model.Notificacao;
import br.com.seuprojeto.pascoa.notification.domain.model.StatusNotificacao;
import br.com.seuprojeto.pascoa.notification.domain.service.TemplateEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class NotificacaoUseCaseImpl implements NotificacaoUseCase {

    private final NotificacaoRepositoryPort repository;
    private final EmailPort emailPort;
    private final WhatsAppPort whatsAppPort;
    private final SmsPort smsPort;

    @Override
    public Notificacao enviar(EnviarCommand cmd) {
        String conteudo = TemplateEngine.render(cmd.templateConteudo(), cmd.variaveis());

        Notificacao notificacao = Notificacao.builder()
                .destinatario(cmd.destinatario())
                .canal(cmd.canal())
                .assunto(cmd.assunto())
                .conteudo(conteudo)
                .status(StatusNotificacao.PENDENTE)
                .evento(cmd.evento())
                .referenciaId(cmd.referenciaId())
                .criadoEm(LocalDateTime.now())
                .build();

        Notificacao salva = repository.save(notificacao);
        return despachar(salva);
    }

    @Override
    public Notificacao reenviar(Long id) {
        Notificacao notificacao = repository.findById(id)
                .orElseThrow(() -> new NotificacaoNotFoundException(id));
        return despachar(notificacao);
    }

    @Override
    @Transactional(readOnly = true)
    public Notificacao buscarPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotificacaoNotFoundException(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Notificacao> listarPorReferencia(String referenciaId) {
        return repository.findByReferenciaId(referenciaId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Notificacao> listarRecentes(int limite) {
        return repository.findRecentes(limite);
    }

    // ── Canal dispatch com fallback WhatsApp → SMS ────────────────────────────

    private Notificacao despachar(Notificacao n) {
        try {
            return switch (n.getCanal()) {
                case EMAIL     -> enviarEmail(n);
                case WHATSAPP  -> enviarWhatsAppComFallback(n);
                case SMS       -> enviarSms(n);
            };
        } catch (Exception e) {
            log.error("Falha ao enviar notificação {}: {}", n.getId(), e.getMessage());
            return repository.save(n.marcarFalhou(e.getMessage()));
        }
    }

    private Notificacao enviarEmail(Notificacao n) {
        emailPort.enviar(n.getDestinatario(), n.getAssunto(), n.getConteudo());
        log.info("Email enviado para {}", n.getDestinatario());
        return repository.save(n.marcarEnviada());
    }

    private Notificacao enviarWhatsAppComFallback(Notificacao n) {
        try {
            whatsAppPort.enviar(n.getDestinatario(), n.getConteudo());
            log.info("WhatsApp enviado para {}", n.getDestinatario());
            return repository.save(n.marcarEnviada());
        } catch (Exception e) {
            log.warn("WhatsApp falhou para {}, tentando SMS como fallback: {}", n.getDestinatario(), e.getMessage());
            smsPort.enviar(n.getDestinatario(), n.getConteudo());
            log.info("SMS fallback enviado para {}", n.getDestinatario());
            return repository.save(n.marcarEnviada());
        }
    }

    private Notificacao enviarSms(Notificacao n) {
        smsPort.enviar(n.getDestinatario(), n.getConteudo());
        log.info("SMS enviado para {}", n.getDestinatario());
        return repository.save(n.marcarEnviada());
    }
}
