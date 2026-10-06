package br.com.seuprojeto.pascoa.seguranca;

import br.com.seuprojeto.pascoa.seguranca.entity.Role;
import br.com.seuprojeto.pascoa.seguranca.entity.Usuario;
import br.com.seuprojeto.pascoa.seguranca.repository.UsuarioRepository;
import br.com.seuprojeto.pascoa.seguranca.service.UsuarioPrincipal;
import br.com.seuprojeto.pascoa.seguranca.service.UsuarioService;
import br.com.seuprojeto.pascoa.shared.exception.RecursoNaoEncontradoException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@WithMockUser(roles = "ADMIN")
class UsuarioTenantTest {

    @Autowired private UsuarioService service;
    @Autowired private UsuarioRepository repository;

    private Usuario salvar(String login, long loja) {
        return repository.save(Usuario.builder().nome(login).login(login).senha("x")
            .role(Role.ATENDENTE).lojaId(loja).build());
    }

    @Test
    void listarTodos_devolveSomenteUsuariosDaLojaAtual() {
        String daLoja1 = "u1-" + UUID.randomUUID();
        String daLoja2 = "u2-" + UUID.randomUUID();
        salvar(daLoja1, 1L);
        salvar(daLoja2, 2L);

        assertThat(service.listarTodos()).extracting(Usuario::getLogin)
            .contains(daLoja1).doesNotContain(daLoja2);
    }

    @Test
    void buscarPorId_deOutraLoja_naoEncontra() {
        Usuario outro = salvar("u2-" + UUID.randomUUID(), 2L);

        assertThatThrownBy(() -> service.buscarPorId(outro.getId()))
            .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void usuarioNovoSemLoja_herdaLojaDoContexto() {
        Usuario novo = repository.save(Usuario.builder().nome("n").login("n-" + UUID.randomUUID())
            .senha("x").role(Role.ATENDENTE).build());

        assertThat(novo.getLojaId()).isEqualTo(1L);
    }

    @Test
    void loadUserByUsername_devolvePrincipalComLoja() {
        String login = "p-" + UUID.randomUUID();
        salvar(login, 2L);

        var principal = (UsuarioPrincipal) service.loadUserByUsername(login);

        assertThat(principal.getLojaId()).isEqualTo(2L);
    }
}
