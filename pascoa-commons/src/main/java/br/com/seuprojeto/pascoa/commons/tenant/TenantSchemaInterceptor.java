package br.com.seuprojeto.pascoa.commons.tenant;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Lê o header {@link TenantContext#HEADER_NAME} (injetado pelo API Gateway via TenantResolutionFilter)
 * e armazena no {@link TenantContext} pela duração do request.
 */
public class TenantSchemaInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String tenantId = request.getHeader(TenantContext.HEADER_NAME);
        if (tenantId != null && !tenantId.isBlank()) {
            TenantContext.set(tenantId.trim());
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        TenantContext.clear();
    }
}
