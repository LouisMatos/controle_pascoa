package br.com.seuprojeto.pascoa.common;

import br.com.seuprojeto.pascoa.common.tenant.TenantContext;
import br.com.seuprojeto.pascoa.seguranca.entity.Loja;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;

public class TenantTestExecutionListener extends AbstractTestExecutionListener {

    @Override
    public int getOrder() {
        return 3000;
    }

    @Override
    public void beforeTestMethod(TestContext testContext) {
        TenantContext.set(Loja.PLATAFORMA_ID);
    }

    @Override
    public void afterTestMethod(TestContext testContext) {
        TenantContext.limpar();
    }
}
