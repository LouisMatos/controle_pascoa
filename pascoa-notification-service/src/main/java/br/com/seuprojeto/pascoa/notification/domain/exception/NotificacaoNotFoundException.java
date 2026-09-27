package br.com.seuprojeto.pascoa.notification.domain.exception;

public class NotificacaoNotFoundException extends RuntimeException {
    public NotificacaoNotFoundException(Long id) {
        super("Notificação não encontrada: " + id);
    }
}
