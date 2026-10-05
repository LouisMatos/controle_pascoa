package br.com.seuprojeto.pascoa.auth.adapter.out.persistence;

import br.com.seuprojeto.pascoa.auth.application.port.out.UserRepositoryPort;
import br.com.seuprojeto.pascoa.auth.domain.model.Usuario;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class UserRepositoryAdapter implements UserRepositoryPort {

    private final UsuarioJpaRepository jpaRepository;
    private final UsuarioMapper mapper;

    @Override
    public Optional<Usuario> findByLogin(String login) {
        return jpaRepository.findByLogin(login).map(mapper::toDomain);
    }

    @Override
    public Optional<Usuario> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Usuario save(Usuario usuario) {
        UsuarioJpaEntity entity = mapper.toEntity(usuario);
        return mapper.toDomain(jpaRepository.save(entity));
    }
}
