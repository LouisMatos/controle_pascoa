package br.com.seuprojeto.pascoa.auditoria.service;

import br.com.seuprojeto.pascoa.auditoria.entity.AuditLog;
import br.com.seuprojeto.pascoa.auditoria.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogService {

    private final AuditLogRepository repository;

    /**
     * Persiste o registro de auditoria numa transação própria (REQUIRES_NEW)
     * para garantir que seja salvo mesmo que a transação principal faça rollback.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrar(String usuario, String acao,
                          String entidadeTipo, Long entidadeId, String detalhes) {
        try {
            repository.save(AuditLog.builder()
                .usuario(usuario)
                .acao(acao)
                .entidadeTipo(entidadeTipo)
                .entidadeId(entidadeId)
                .detalhes(detalhes)
                .build());
        } catch (Exception e) {
            log.error("[AUDIT] Falha ao persistir log: acao={} usuario={} — {}", acao, usuario, e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> listar(String usuario, String acao, int pagina) {
        PageRequest pageRequest = PageRequest.of(pagina, 50);
        boolean semFiltro = (usuario == null || usuario.isBlank())
                         && (acao    == null || acao.isBlank());
        return semFiltro
            ? repository.findAllByOrderByCriadoEmDesc(pageRequest)
            : repository.filtrar(
                (usuario != null && !usuario.isBlank()) ? usuario : null,
                (acao    != null && !acao.isBlank())    ? acao    : null,
                pageRequest);
    }

    /** Usuário autenticado atual — fallback "sistema". */
    public static String usuarioAtual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()
                && !"anonymousUser".equals(auth.getName())) {
            return auth.getName();
        }
        return "sistema";
    }
}
