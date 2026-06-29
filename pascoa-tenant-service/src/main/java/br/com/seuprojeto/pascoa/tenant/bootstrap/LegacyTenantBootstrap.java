package br.com.seuprojeto.pascoa.tenant.bootstrap;

import br.com.seuprojeto.pascoa.tenant.domain.*;
import br.com.seuprojeto.pascoa.tenant.repository.TenantRepository;
import br.com.seuprojeto.pascoa.tenant.repository.WhiteLabelConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * v6 Etapa 16 — Migra a operação atual (Controle Páscoa v5, single-tenant) para o
 * modelo SaaS multi-tenant da v6 sem migração de dados.
 *
 * <p>Estratégia "tenant zero":
 * <ul>
 *   <li>Registra o tenant <b>pascoa-original</b> na plataforma como ENTERPRISE perpétuo</li>
 *   <li>O banco {@code pascoa_monolith} continua sendo a fonte da verdade — o tenant
 *       apenas <i>representa</i> a operação legada no painel de plataforma</li>
 *   <li>Quando o monólito for ativado em modo multi-tenant ({@code foodflow.tenant.enabled=true}),
 *       requests sem subdomínio fazem fallback para este tenant via header explícito
 *       {@code X-Tenant-Id: pascoa-original}</li>
 * </ul>
 *
 * <p>Idempotente: se o tenant já existe, não faz nada. Controlado por
 * {@code foodflow.bootstrap-legacy=true} (default {@code true}).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LegacyTenantBootstrap implements CommandLineRunner {

    private static final String LEGACY_ID = "pascoa-original";

    @Value("${foodflow.bootstrap-legacy:true}")
    private boolean enabled;

    private final TenantRepository tenantRepo;
    private final WhiteLabelConfigRepository whiteLabelRepo;

    @Override
    @Transactional
    public void run(String... args) {
        if (!enabled) {
            log.info("LegacyTenantBootstrap desabilitado (foodflow.bootstrap-legacy=false)");
            return;
        }
        if (tenantRepo.existsById(LEGACY_ID)) {
            log.info("Tenant legacy '{}' já registrado — bootstrap idempotente, nada a fazer", LEGACY_ID);
            return;
        }

        Tenant tenant = Tenant.builder()
                .id(LEGACY_ID)
                .razaoSocial("Controle Páscoa Original")
                .email("admin@pascoa-original.local")
                .tipoNegocio(TipoNegocio.DOCES)
                .plano(PlanoAssinatura.ENTERPRISE)
                .status(StatusTenant.ATIVO)
                // ENTERPRISE perpétuo — sem expiração
                .dataExpiracao(LocalDate.of(9999, 12, 31))
                .build();
        tenantRepo.save(tenant);

        whiteLabelRepo.save(WhiteLabelConfig.builder()
                .tenantId(LEGACY_ID)
                .nomeApp("Controle Páscoa")
                .corPrimaria("#e91e63")        // rosa Páscoa
                .corSecundaria("#fce4ec")
                .rodape("Sistema legacy — pascoa_monolith")
                .build());

        log.warn("Tenant legacy '{}' registrado como ENTERPRISE ATIVO (operação Controle Páscoa atual)",
                LEGACY_ID);
        log.warn("Importante: schema PostgreSQL deste tenant NÃO foi criado em foodflow_platform — " +
                "o monólito continua usando o banco 'pascoa_monolith' como fonte da verdade.");
    }
}
