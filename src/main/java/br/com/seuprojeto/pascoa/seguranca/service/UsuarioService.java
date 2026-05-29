package br.com.seuprojeto.pascoa.seguranca.service;

import br.com.seuprojeto.pascoa.seguranca.dto.UsuarioForm;
import br.com.seuprojeto.pascoa.seguranca.entity.Usuario;
import br.com.seuprojeto.pascoa.seguranca.repository.UsuarioRepository;
import br.com.seuprojeto.pascoa.shared.exception.RecursoNaoEncontradoException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import br.com.seuprojeto.pascoa.auditoria.annotation.Auditavel;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UsuarioService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    // -----------------------------------------------------------------------
    // UserDetailsService — usado pelo Spring Security
    // -----------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String login) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByLogin(login)
            .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado: " + login));

        if (!usuario.isAtivo()) {
            throw new UsernameNotFoundException("Usuário inativo: " + login);
        }

        return User.builder()
            .username(usuario.getLogin())
            .password(usuario.getSenha())
            .authorities(new SimpleGrantedAuthority("ROLE_" + usuario.getRole().name()))
            .build();
    }

    // -----------------------------------------------------------------------
    // CRUD de usuários (apenas ADMIN)
    // -----------------------------------------------------------------------

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public List<Usuario> listarTodos() {
        return usuarioRepository.findAllByOrderByNomeAsc();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public Usuario buscarPorId(Long id) {
        return usuarioRepository.findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado: " + id));
    }

    @Auditavel(acao = "SALVAR_USUARIO", entidade = "Usuario")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void salvar(UsuarioForm form) {
        if (form.getId() != null) {
            // Edição — carrega entidade existente para preservar campos não editáveis
            Usuario usuario = buscarPorId(form.getId());
            usuario.setNome(form.getNome());
            usuario.setLogin(form.getLogin());
            usuario.setEmail(form.getEmail());
            usuario.setRole(form.getRole());
            usuario.setAtivo(form.isAtivo());
            if (form.getSenha() != null && !form.getSenha().isBlank()) {
                usuario.setSenha(passwordEncoder.encode(form.getSenha()));
            }
            usuarioRepository.save(usuario);
        } else {
            // Criação
            usuarioRepository.save(Usuario.builder()
                .nome(form.getNome())
                .login(form.getLogin())
                .email(form.getEmail())
                .senha(passwordEncoder.encode(form.getSenha()))
                .role(form.getRole())
                .ativo(true)
                .build());
        }
    }

    @Auditavel(acao = "TOGGLE_USUARIO", entidade = "Usuario")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void alternarAtivo(Long id) {
        Usuario usuario = buscarPorId(id);
        usuario.setAtivo(!usuario.isAtivo());
        usuarioRepository.save(usuario);
    }
}
