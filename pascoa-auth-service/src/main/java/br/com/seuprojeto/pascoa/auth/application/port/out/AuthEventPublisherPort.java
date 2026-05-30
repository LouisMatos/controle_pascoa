package br.com.seuprojeto.pascoa.auth.application.port.out;

public interface AuthEventPublisherPort {
    void publishLoginSuccess(Long userId, String login);
    void publishLoginFailed(String login, String reason);
}
