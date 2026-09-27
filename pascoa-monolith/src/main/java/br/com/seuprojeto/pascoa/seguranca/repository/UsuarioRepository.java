package br.com.seuprojeto.pascoa.seguranca.repository;

import br.com.seuprojeto.pascoa.seguranca.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByLogin(String login);

    Optional<Usuario> findByEmail(String email);

    List<Usuario> findAllByOrderByNomeAsc();
}
