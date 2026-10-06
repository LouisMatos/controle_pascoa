package br.com.seuprojeto.pascoa.common.entity;

import br.com.seuprojeto.pascoa.common.tenant.TenantContext;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import lombok.Getter;
import org.hibernate.annotations.TenantId;

@MappedSuperclass
@Getter
public abstract class TenantEntity {

    @TenantId
    @Column(name = "loja_id", nullable = false, updatable = false)
    private Long lojaId;

    @PrePersist
    void exigirLojaNoContexto() {
        if (TenantContext.atual() == TenantContext.SEM_TENANT) {
            throw new IllegalStateException("Escrita sem loja no contexto");
        }
    }
}
