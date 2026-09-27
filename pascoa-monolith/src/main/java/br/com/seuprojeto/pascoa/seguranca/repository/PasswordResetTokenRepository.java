package br.com.seuprojeto.pascoa.seguranca.repository;

import br.com.seuprojeto.pascoa.seguranca.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByToken(String token);

    /** Remove todos os tokens pendentes de um usuário antes de gerar um novo. */
    @Modifying
    @Query("DELETE FROM PasswordResetToken t WHERE t.usuario.id = :usuarioId")
    void deleteByUsuarioId(@Param("usuarioId") Long usuarioId);
}
