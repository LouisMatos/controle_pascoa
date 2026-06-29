package br.com.seuprojeto.pascoa.tenant.dto;

import br.com.seuprojeto.pascoa.tenant.domain.Tenant;

public record OnboardingResponse(
        TenantResponse tenant,
        EtapaResultado registroTenant,
        EtapaResultado provisionamentoSchema,
        EtapaResultado aplicacaoTemplate,
        EtapaResultado inicioTrial,
        EtapaResultado whiteLabel,
        boolean sucesso
) {
    public record EtapaResultado(boolean ok, String detalhe) {
        public static EtapaResultado ok(String detalhe) { return new EtapaResultado(true,  detalhe); }
        public static EtapaResultado fail(String erro)  { return new EtapaResultado(false, erro); }
    }

    public static OnboardingResponse from(Tenant t,
                                          EtapaResultado registro,
                                          EtapaResultado schema,
                                          EtapaResultado template,
                                          EtapaResultado trial,
                                          EtapaResultado whiteLabel) {
        boolean all = registro.ok() && schema.ok() && template.ok() && trial.ok() && whiteLabel.ok();
        return new OnboardingResponse(TenantResponse.from(t), registro, schema, template, trial, whiteLabel, all);
    }
}
