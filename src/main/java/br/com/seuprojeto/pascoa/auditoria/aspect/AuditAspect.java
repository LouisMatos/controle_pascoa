package br.com.seuprojeto.pascoa.auditoria.aspect;

import br.com.seuprojeto.pascoa.auditoria.annotation.Auditavel;
import br.com.seuprojeto.pascoa.auditoria.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditAspect {

    private final AuditLogService auditLogService;

    @Around("@annotation(auditavel)")
    public Object registrar(ProceedingJoinPoint pjp, Auditavel auditavel) throws Throwable {
        Object resultado = pjp.proceed();

        try {
            String usuario      = AuditLogService.usuarioAtual();
            String entidadeTipo = auditavel.entidade().isBlank() ? null : auditavel.entidade();
            Long   entidadeId   = extrairId(pjp.getArgs(), resultado);
            String detalhes     = null;

            auditLogService.registrar(usuario, auditavel.acao(), entidadeTipo, entidadeId, detalhes);
        } catch (Exception e) {
            // Nunca deixar o log derrubar a requisição principal
            log.warn("[AUDIT] Falha no aspecto: {}", e.getMessage());
        }

        return resultado;
    }

    /**
     * Tenta extrair o ID da entidade da lista de argumentos ou do valor de retorno.
     * Estratégia: primeiro Long nos args → getId() no retorno → null.
     */
    private Long extrairId(Object[] args, Object resultado) {
        // Primeiro argumento Long (ex.: confirmar(Long id))
        if (args != null) {
            for (Object arg : args) {
                if (arg instanceof Long l) return l;
            }
        }
        // Valor de retorno com getId()
        if (resultado != null) {
            try {
                var method = resultado.getClass().getMethod("getId");
                Object id = method.invoke(resultado);
                if (id instanceof Long l) return l;
            } catch (Exception ignored) {}
        }
        return null;
    }
}
