package br.com.seuprojeto.pascoa.tenant.service;

import br.com.seuprojeto.pascoa.commons.events.TenantConfigAlteradoEvent;
import br.com.seuprojeto.pascoa.commons.events.TenantConfigEventPublisher;
import br.com.seuprojeto.pascoa.tenant.domain.*;
import br.com.seuprojeto.pascoa.tenant.dto.RegistrarTenantRequest;
import br.com.seuprojeto.pascoa.tenant.dto.WhiteLabelRequest;
import br.com.seuprojeto.pascoa.tenant.exception.TenantException;
import br.com.seuprojeto.pascoa.tenant.exception.TenantNotFoundException;
import br.com.seuprojeto.pascoa.tenant.repository.TenantRepository;
import br.com.seuprojeto.pascoa.tenant.repository.WhiteLabelConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class TenantService {

    private static final int TRIAL_DAYS = 14;

    private final TenantRepository tenantRepository;
    private final WhiteLabelConfigRepository whiteLabelRepository;
    private final TenantProvisioningService provisioningService;
    private final TenantConfigEventPublisher.Publisher tenantConfigEventPublisher;

    public Tenant registrar(RegistrarTenantRequest req) {
        if (tenantRepository.existsById(req.id())) {
            throw new TenantException("Tenant já registrado: " + req.id());
        }
        if (tenantRepository.existsByEmail(req.email())) {
            throw new TenantException("Email já vinculado a outro tenant: " + req.email());
        }

        Tenant tenant = Tenant.builder()
                .id(req.id())
                .razaoSocial(req.razaoSocial())
                .cnpjCpf(req.cnpjCpf())
                .email(req.email())
                .tipoNegocio(req.tipoNegocio())
                .plano(req.plano() != null ? req.plano() : PlanoAssinatura.TRIAL)
                .status(StatusTenant.TRIAL)
                .dataExpiracao(LocalDate.now().plusDays(TRIAL_DAYS))
                .build();

        tenantRepository.save(tenant);

        // Provisiona schema dedicado no banco da plataforma.
        // Etapa 4 expandirá para todos os bancos por bounded context (Liquibase multi-schema).
        provisioningService.provisionarSchema(tenant.getId());

        // Inicializa white-label vazio para o tenant
        whiteLabelRepository.save(WhiteLabelConfig.builder()
                .tenantId(tenant.getId())
                .nomeApp(tenant.getRazaoSocial())
                .build());

        log.info("Tenant registrado: id={}, tipo={}, status={}, expira={}",
                tenant.getId(), tenant.getTipoNegocio(), tenant.getStatus(), tenant.getDataExpiracao());
        return tenant;
    }

    @Transactional(readOnly = true)
    public Tenant buscarPorId(String tenantId) {
        return tenantRepository.findById(tenantId)
                .orElseThrow(() -> new TenantNotFoundException(tenantId));
    }

    public Tenant suspender(String tenantId) {
        Tenant t = buscarPorId(tenantId);
        t.setStatus(StatusTenant.SUSPENSO);
        log.warn("Tenant suspenso: {}", tenantId);
        return t;
    }

    public Tenant reativar(String tenantId) {
        Tenant t = buscarPorId(tenantId);
        if (t.getStatus() == StatusTenant.CANCELADO) {
            throw new TenantException("Tenant CANCELADO não pode ser reativado: " + tenantId);
        }
        t.setStatus(StatusTenant.ATIVO);
        log.info("Tenant reativado: {}", tenantId);
        return t;
    }

    public void cancelar(String tenantId) {
        Tenant t = buscarPorId(tenantId);
        t.setStatus(StatusTenant.CANCELADO);
        // Anonimização LGPD — schema removido fisicamente
        provisioningService.removerSchema(tenantId);
        log.warn("Tenant cancelado e schema removido: {}", tenantId);
    }

    public WhiteLabelConfig atualizarWhiteLabel(String tenantId, WhiteLabelRequest req) {
        buscarPorId(tenantId); // valida existência
        WhiteLabelConfig cfg = whiteLabelRepository.findById(tenantId)
                .orElse(WhiteLabelConfig.builder().tenantId(tenantId).build());
        if (req.nomeApp()       != null) cfg.setNomeApp(req.nomeApp());
        if (req.logoUrl()       != null) cfg.setLogoUrl(req.logoUrl());
        if (req.corPrimaria()   != null) cfg.setCorPrimaria(req.corPrimaria());
        if (req.corSecundaria() != null) cfg.setCorSecundaria(req.corSecundaria());
        if (req.favicon()       != null) cfg.setFavicon(req.favicon());
        if (req.rodape()        != null) cfg.setRodape(req.rodape());
        WhiteLabelConfig saved = whiteLabelRepository.save(cfg);
        // MS-01 — propaga a alteração via RabbitMQ; consumers (config-engine,
        // notification, catalog) invalidam o cache local imediatamente em vez
        // de esperar o TTL (até 5min) expirar.
        tenantConfigEventPublisher.publicar(tenantId, TenantConfigAlteradoEvent.Tipo.WHITE_LABEL);
        return saved;
    }

    public Tenant atualizarDominio(String tenantId, String dominio) {
        Tenant t = buscarPorId(tenantId);
        if (t.getPlano() != PlanoAssinatura.ENTERPRISE) {
            throw new TenantException("Domínio próprio é exclusivo do plano ENTERPRISE");
        }
        t.setDominioCustom(dominio);
        return t;
    }
}
