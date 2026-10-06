package br.com.seuprojeto.pascoa.config;

import br.com.seuprojeto.pascoa.common.tenant.TenantContext;
import br.com.seuprojeto.pascoa.seguranca.entity.Role;
import br.com.seuprojeto.pascoa.seguranca.entity.Usuario;
import br.com.seuprojeto.pascoa.seguranca.service.UsuarioPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TenantFilterTest {

    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final TenantFilter filter = new TenantFilter(jdbc);

    @AfterEach
    void limpar() {
        SecurityContextHolder.clearContext();
        TenantContext.limpar();
    }

    private long tenantDuranteRequisicao(String caminho) throws Exception {
        var request = new MockHttpServletRequest("GET", caminho);
        request.setServletPath(caminho);
        AtomicLong visto = new AtomicLong(-1);
        filter.doFilter(request, new MockHttpServletResponse(), (rq, rs) -> visto.set(TenantContext.atual()));
        return visto.get();
    }

    @Test
    void usuarioLogado_usaLojaDoPrincipal() throws Exception {
        var usuario = Usuario.builder().nome("A").login("a").senha("x").role(Role.ADMIN).lojaId(9L).build();
        var principal = new UsuarioPrincipal(usuario);
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));

        assertThat(tenantDuranteRequisicao("/clientes")).isEqualTo(9L);
        assertThat(TenantContext.atual()).isEqualTo(TenantContext.SEM_TENANT);
    }

    @Test
    void tokenDeAcompanhamento_resolveLojaDoPedido() throws Exception {
        when(jdbc.queryForList(anyString(), eq(Long.class), eq("tok-1"))).thenReturn(List.of(7L));

        assertThat(tenantDuranteRequisicao("/acompanhamento/tok-1")).isEqualTo(7L);
    }

    @Test
    void tokenDeOrcamento_resolveLojaDoOrcamento_tambemNasAcoes() throws Exception {
        when(jdbc.queryForList(anyString(), eq(Long.class), eq("tok-2"))).thenReturn(List.of(8L));

        assertThat(tenantDuranteRequisicao("/orcamento-publico/tok-2/aprovar")).isEqualTo(8L);
    }

    @Test
    void tokenDesconhecido_ficaSemTenant() throws Exception {
        when(jdbc.queryForList(anyString(), eq(Long.class), eq("nada"))).thenReturn(List.of());

        assertThat(tenantDuranteRequisicao("/acompanhamento/nada")).isEqualTo(TenantContext.SEM_TENANT);
    }

    @Test
    void catalogo_usaLojaDaPlataforma() throws Exception {
        assertThat(tenantDuranteRequisicao("/catalogo")).isEqualTo(1L);
    }

    @Test
    void rotaSemTenant_naoDefineContexto() throws Exception {
        assertThat(tenantDuranteRequisicao("/login")).isEqualTo(TenantContext.SEM_TENANT);
    }
}
