package br.com.seuprojeto.pascoa.auth.application.port.out;

import br.com.seuprojeto.pascoa.auth.domain.model.Usuario;

import java.util.Optional;

public interface UserRepositoryPort {
    Optional<Usuario> findByLogin(String login);
    Optional<Usuario> findById(Long id);
    Usuario save(Usuario usuario);
}
