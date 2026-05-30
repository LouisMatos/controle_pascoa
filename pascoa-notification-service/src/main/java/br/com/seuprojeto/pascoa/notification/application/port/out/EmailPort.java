package br.com.seuprojeto.pascoa.notification.application.port.out;

public interface EmailPort {
    void enviar(String destinatario, String assunto, String conteudo);
}
