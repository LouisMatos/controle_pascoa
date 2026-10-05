package br.com.seuprojeto.pascoa.notification.adapter.out.channel;

import br.com.seuprojeto.pascoa.notification.application.port.out.EmailPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailAdapter implements EmailPort {

    private final JavaMailSender mailSender;

    @Value("${notification.email.from:noreply@pascoa.com.br}")
    private String from;

    @Override
    public void enviar(String destinatario, String assunto, String conteudo) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(from);
        msg.setTo(destinatario);
        msg.setSubject(assunto != null ? assunto : "Notificação Páscoa");
        msg.setText(conteudo);
        mailSender.send(msg);
        log.info("Email enviado → {}", destinatario);
    }
}
