package br.com.seuprojeto.pascoa.notification.application.port.out;

public interface WhatsAppPort {
    void enviar(String telefone, String mensagem);
}
