package br.com.seuprojeto.pascoa.notification.application.port.out;

public interface SmsPort {
    void enviar(String telefone, String mensagem);
}
