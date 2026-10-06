package br.com.seuprojeto.pascoa.common.tenant;

import org.springframework.core.task.TaskDecorator;
import org.springframework.stereotype.Component;

@Component
public class TenantTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable tarefa) {
        long lojaId = TenantContext.atual();
        if (lojaId == TenantContext.SEM_TENANT) {
            return tarefa;
        }
        return () -> TenantContext.executar(lojaId, tarefa);
    }
}
