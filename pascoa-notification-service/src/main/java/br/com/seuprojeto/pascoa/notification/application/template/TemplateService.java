package br.com.seuprojeto.pascoa.notification.application.template;

import br.com.seuprojeto.pascoa.notification.adapter.in.rest.dto.RenderResponse;
import br.com.seuprojeto.pascoa.notification.adapter.in.rest.dto.TemplateRequest;
import br.com.seuprojeto.pascoa.notification.adapter.out.persistence.NotifTemplateJpaEntity;
import br.com.seuprojeto.pascoa.notification.adapter.out.persistence.NotifTemplateJpaRepository;
import br.com.seuprojeto.pascoa.notification.domain.service.TemplateEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * v6 Etapa 10 — Resolução white-label de templates.
 *
 * <p>Resolução por precedência:
 * <ol>
 *   <li>Template ativo com {@code (tenantId, evento, canal)}</li>
 *   <li>Fallback global: {@code (NULL, evento, canal)}</li>
 *   <li>Caso nenhum exista → {@link TemplateNotFoundException}</li>
 * </ol>
 *
 * <p>O texto sempre passa pelo {@link TemplateEngine} para substituir
 * {@code {variavel}} pelos valores do mapa.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class TemplateService {

    private final NotifTemplateJpaRepository repo;

    public NotifTemplateJpaEntity criar(TemplateRequest req) {
        // Se existir ativo no mesmo slot, desativa antes (único ativo por slot via index parcial)
        Optional<NotifTemplateJpaEntity> existente =
                repo.findByTenantIdAndEventoAndCanalAndAtivoTrue(req.tenantId(), req.evento(), req.canal());
        existente.ifPresent(t -> { t.setAtivo(false); repo.save(t); });

        return repo.save(NotifTemplateJpaEntity.builder()
                .tenantId(req.tenantId())
                .evento(req.evento())
                .canal(req.canal())
                .assunto(req.assunto())
                .conteudo(req.conteudo())
                .ativo(true)
                .build());
    }

    @Transactional(readOnly = true)
    public List<NotifTemplateJpaEntity> listarDoTenant(String tenantId) {
        return repo.findByTenantIdAndAtivoTrue(tenantId);
    }

    public void desativar(Long id) {
        repo.findById(id).ifPresent(t -> { t.setAtivo(false); repo.save(t); });
    }

    @Transactional(readOnly = true)
    public RenderResponse renderizar(String tenantId, String evento, String canal,
                                     Map<String, String> variaveis) {
        var lista = repo.resolverComFallback(tenantId, evento, canal);
        if (lista.isEmpty()) {
            throw new TemplateNotFoundException(tenantId, evento, canal);
        }
        NotifTemplateJpaEntity t = lista.get(0); // ORDER BY já prioriza tenant
        boolean fallback = t.getTenantId() == null;
        String assunto = TemplateEngine.render(t.getAssunto(), variaveis);
        String conteudo = TemplateEngine.render(t.getConteudo(), variaveis);
        return new RenderResponse(tenantId, evento, canal, assunto, conteudo, fallback);
    }

    public static class TemplateNotFoundException extends RuntimeException {
        public TemplateNotFoundException(String tenantId, String evento, String canal) {
            super("Nenhum template ativo (incluindo global) para tenant=" + tenantId
                    + " evento=" + evento + " canal=" + canal);
        }
    }
}
