package br.com.seuprojeto.pascoa.seguranca.service;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Serviço de autorização no nível de método — usado via @PreAuthorize("@authService.owns(...)").
 *
 * Sistema single-tenant: owns() confirma que o usuário está autenticado.
 * Em uma expansão multi-tenant, este método receberia tenantId e verificaria a posse do recurso.
 */
@Component("authService")
public class AuthorizationService {

    public boolean owns(Long resourceId, Authentication auth) {
        return auth != null
            && auth.isAuthenticated()
            && !"anonymousUser".equals(auth.getPrincipal());
    }
}
