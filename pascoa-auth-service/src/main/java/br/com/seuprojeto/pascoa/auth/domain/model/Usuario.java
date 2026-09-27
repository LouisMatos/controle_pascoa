package br.com.seuprojeto.pascoa.auth.domain.model;

import lombok.Builder;
import lombok.Getter;
import lombok.With;

import java.util.Set;

@Getter
@Builder
@With
public class Usuario {

    private final Long id;
    private final String login;
    private final String passwordHash;
    private final Set<Role> roles;
    private final boolean ativo;
    private final boolean totpAtivado;
    private final String totpSecret;
    private final int tentativasFalhas;

    private static final int MAX_TENTATIVAS = 5;

    public boolean estaBloqueado() {
        return tentativasFalhas >= MAX_TENTATIVAS;
    }

    public boolean hasRole(Role role) {
        return roles != null && roles.contains(role);
    }

    public Usuario incrementarFalhas() {
        return this.withTentativasFalhas(this.tentativasFalhas + 1);
    }

    public Usuario resetarFalhas() {
        return this.withTentativasFalhas(0);
    }
}
