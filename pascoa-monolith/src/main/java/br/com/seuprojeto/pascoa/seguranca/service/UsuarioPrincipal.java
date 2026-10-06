package br.com.seuprojeto.pascoa.seguranca.service;

import br.com.seuprojeto.pascoa.seguranca.entity.Usuario;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.List;

public class UsuarioPrincipal extends User {

    private static final long serialVersionUID = 1L;

    private final Long lojaId;

    public UsuarioPrincipal(Usuario usuario) {
        super(usuario.getLogin(), usuario.getSenha(), true, true, true, true,
            List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getRole().name())));
        this.lojaId = usuario.getLojaId();
    }

    public Long getLojaId() {
        return lojaId;
    }
}
