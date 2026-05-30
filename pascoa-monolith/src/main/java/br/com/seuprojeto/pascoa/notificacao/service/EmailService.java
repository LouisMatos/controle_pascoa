package br.com.seuprojeto.pascoa.notificacao.service;

import br.com.seuprojeto.pascoa.notificacao.entity.ConfiguracaoCanal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * Envio de e-mails transacionais via Spring Mail.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    /**
     * Envia e-mail HTML.
     *
     * @param config   configuração do canal (remetente = from address)
     * @param para     endereço de destino
     * @param assunto  assunto do e-mail
     * @param corpo    corpo em HTML (já renderizado com os dados do pedido)
     */
    public void enviar(ConfiguracaoCanal config, String para, String assunto, String corpo) {
        if (Boolean.TRUE.equals(config.getTestMode())) {
            log.info("[EMAIL TEST MODE] Para: {} | Assunto: {} | Corpo: {}", para, assunto, corpo);
            return;
        }

        try {
            var mensagem = mailSender.createMimeMessage();
            var helper = new MimeMessageHelper(mensagem, true, "UTF-8");
            helper.setFrom(config.getRemetente());
            helper.setTo(para);
            helper.setSubject(assunto);
            helper.setText(corpo, true); // HTML
            mailSender.send(mensagem);
            log.info("[EMAIL] Mensagem enviada para {} — assunto: {}", para, assunto);
        } catch (Exception e) {
            log.error("[EMAIL] Falha ao enviar para {}: {}", para, e.getMessage());
            throw new RuntimeException("Falha ao enviar e-mail para " + para + ": " + e.getMessage(), e);
        }
    }
}
