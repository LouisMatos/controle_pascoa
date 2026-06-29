package br.com.seuprojeto.pascoa.tenant.service;

import br.com.seuprojeto.pascoa.tenant.domain.Tenant;
import br.com.seuprojeto.pascoa.tenant.dto.OnboardingRequest;
import br.com.seuprojeto.pascoa.tenant.dto.OnboardingResponse;
import br.com.seuprojeto.pascoa.tenant.dto.OnboardingResponse.EtapaResultado;
import br.com.seuprojeto.pascoa.tenant.dto.RegistrarTenantRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * v6 Etapa 14 — Wizard de onboarding self-service.
 *
 * <p>Orquestra registro + provisionamento + template + assinatura trial em uma
 * única transação lógica. Cada etapa downstream é best-effort com diagnóstico
 * detalhado na resposta — falhas em config-engine/subscription NÃO desfazem
 * o tenant criado (apenas marcam a etapa como falha para retry posterior pelo painel).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OnboardingService {

    private final TenantService tenantService;

    @Value("${foodflow.services.config-engine-url:http://localhost:8091}")
    private String configEngineUrl;

    @Value("${foodflow.services.subscription-url:http://localhost:8093}")
    private String subscriptionUrl;

    public OnboardingResponse executar(OnboardingRequest req) {
        log.info("Iniciando onboarding: tenant={} tipo={}", req.tenantId(), req.tipoNegocio());

        // 1) Registro + provisionamento (mesmo TX local)
        Tenant tenant;
        EtapaResultado registro;
        EtapaResultado schema;
        try {
            tenant = tenantService.registrar(new RegistrarTenantRequest(
                    req.tenantId(), req.razaoSocial(), req.cnpjCpf(), req.email(),
                    req.tipoNegocio(), null));
            registro = EtapaResultado.ok("Tenant " + req.tenantId() + " criado");
            schema   = EtapaResultado.ok("Schema PostgreSQL provisionado + Liquibase aplicado");
        } catch (RuntimeException e) {
            log.error("Onboarding falhou no registro: {}", e.getMessage());
            EtapaResultado fail = EtapaResultado.fail(e.getMessage());
            return new OnboardingResponse(null, fail, fail,
                    EtapaResultado.fail("não executado"),
                    EtapaResultado.fail("não executado"),
                    EtapaResultado.fail("não executado"), false);
        }

        // 2) Aplica template no config-engine
        EtapaResultado template = chamarHttp(
                "config-engine /aplicar-template",
                () -> RestClient.create().post()
                        .uri(configEngineUrl + "/config/" + req.tenantId() + "/aplicar-template/" + req.tipoNegocio())
                        .retrieve().toBodilessEntity());

        // 3) Cria assinatura TRIAL no subscription-service
        EtapaResultado trial = chamarHttp(
                "subscription /iniciar-trial",
                () -> RestClient.create().post()
                        .uri(subscriptionUrl + "/subscriptions/iniciar-trial")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .body(Map.of("tenantId", req.tenantId(), "email", req.email()))
                        .retrieve().toBodilessEntity());

        // 4) White-label (local — mesmo banco)
        EtapaResultado whiteLabel;
        if (req.whiteLabel() != null) {
            try {
                tenantService.atualizarWhiteLabel(req.tenantId(), req.whiteLabel());
                whiteLabel = EtapaResultado.ok("White-label gravado");
            } catch (RuntimeException e) {
                whiteLabel = EtapaResultado.fail(e.getMessage());
            }
        } else {
            whiteLabel = EtapaResultado.ok("não fornecido (usando defaults)");
        }

        log.info("Onboarding concluído: tenant={} template={} trial={} white-label={}",
                req.tenantId(), template.ok(), trial.ok(), whiteLabel.ok());
        return OnboardingResponse.from(tenant, registro, schema, template, trial, whiteLabel);
    }

    private EtapaResultado chamarHttp(String descricao, Runnable acao) {
        try {
            acao.run();
            return EtapaResultado.ok(descricao + " OK");
        } catch (Exception e) {
            log.warn("Etapa best-effort falhou ({}): {}", descricao, e.getMessage());
            return EtapaResultado.fail(descricao + " falhou: " + e.getMessage());
        }
    }
}
