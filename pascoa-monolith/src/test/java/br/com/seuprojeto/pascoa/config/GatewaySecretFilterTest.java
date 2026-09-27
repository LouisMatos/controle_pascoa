package br.com.seuprojeto.pascoa.config;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class GatewaySecretFilterTest {

    private final FilterChain chain = mock(FilterChain.class);

    private MockHttpServletRequest req(String uri, String header) {
        MockHttpServletRequest r = new MockHttpServletRequest("GET", uri);
        if (header != null) {
            r.addHeader("X-Gateway-Secret", header);
        }
        return r;
    }

    @Test
    void semSegredoConfigurado_naoFiltra() {
        assertThat(new GatewaySecretFilter("").shouldNotFilter(req("/dashboard", null))).isTrue();
    }

    @Test
    void healthSempreLiberado() {
        assertThat(new GatewaySecretFilter("s3cr3t").shouldNotFilter(req("/actuator/health", null))).isTrue();
    }

    @Test
    void segredoCorreto_passa() throws Exception {
        MockHttpServletResponse res = new MockHttpServletResponse();
        new GatewaySecretFilter("s3cr3t").doFilterInternal(req("/dashboard", "s3cr3t"), res, chain);
        verify(chain).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        assertThat(res.getStatus()).isEqualTo(200);
    }

    @Test
    void segredoAusenteOuErrado_403() throws Exception {
        for (String header : new String[] {null, "errado"}) {
            MockHttpServletResponse res = new MockHttpServletResponse();
            new GatewaySecretFilter("s3cr3t").doFilterInternal(req("/dashboard", header), res, chain);
            assertThat(res.getStatus()).isEqualTo(403);
            assertThat(res.getContentAsString()).isEqualTo("403 Forbidden");
            assertThat(res.getRedirectedUrl()).isNull();
        }
        verify(chain, never()).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }
}
