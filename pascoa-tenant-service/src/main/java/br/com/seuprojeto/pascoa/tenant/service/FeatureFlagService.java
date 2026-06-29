package br.com.seuprojeto.pascoa.tenant.service;

import br.com.seuprojeto.pascoa.tenant.domain.FeatureFlag;
import br.com.seuprojeto.pascoa.tenant.domain.FeatureFlagTenant;
import br.com.seuprojeto.pascoa.tenant.domain.Tenant;
import br.com.seuprojeto.pascoa.tenant.exception.TenantException;
import br.com.seuprojeto.pascoa.tenant.repository.FeatureFlagRepository;
import br.com.seuprojeto.pascoa.tenant.repository.FeatureFlagTenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class FeatureFlagService {

    private final FeatureFlagRepository flagRepo;
    private final FeatureFlagTenantRepository overrideRepo;
    private final TenantService tenantService;

    /** Resolve {@code (override do tenant) > (default do plano)}. False se a flag não existe. */
    @Transactional(readOnly = true)
    public boolean isAtivo(String tenantId, String chave) {
        var override = overrideRepo.findByTenantIdAndChave(tenantId, chave);
        if (override.isPresent()) return override.get().isAtivo();
        var flag = flagRepo.findById(chave);
        if (flag.isEmpty()) return false;
        Tenant t = tenantService.buscarPorId(tenantId);
        return flag.get().defaultParaPlano(t.getPlano());
    }

    /** Mapa {chave → ativo} de todas as flags conhecidas para o tenant. */
    @Transactional(readOnly = true)
    public Map<String, Boolean> resolverTodas(String tenantId) {
        Tenant tenant = tenantService.buscarPorId(tenantId);
        Map<String, Boolean> overrides = new HashMap<>();
        overrideRepo.findByTenantId(tenantId)
                .forEach(o -> overrides.put(o.getChave(), o.isAtivo()));

        Map<String, Boolean> result = new HashMap<>();
        for (FeatureFlag flag : flagRepo.findAll()) {
            result.put(flag.getChave(),
                    overrides.getOrDefault(flag.getChave(), flag.defaultParaPlano(tenant.getPlano())));
        }
        return result;
    }

    /** Salva ou atualiza o override do tenant. */
    public FeatureFlagTenant definir(String tenantId, String chave, boolean ativo) {
        tenantService.buscarPorId(tenantId); // valida
        if (!flagRepo.existsById(chave)) {
            throw new TenantException("Feature flag desconhecida: " + chave);
        }
        var existing = overrideRepo.findByTenantIdAndChave(tenantId, chave)
                .orElseGet(() -> FeatureFlagTenant.builder()
                        .tenantId(tenantId).chave(chave).build());
        existing.setAtivo(ativo);
        existing.setAtualizadoEm(LocalDateTime.now());
        return overrideRepo.save(existing);
    }

    public void remover(String tenantId, String chave) {
        overrideRepo.findByTenantIdAndChave(tenantId, chave).ifPresent(overrideRepo::delete);
    }
}
