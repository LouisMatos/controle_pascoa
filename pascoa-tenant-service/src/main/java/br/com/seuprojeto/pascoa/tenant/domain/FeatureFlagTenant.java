package br.com.seuprojeto.pascoa.tenant.domain;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "feature_flag_tenant", schema = "platform")
@IdClass(FeatureFlagTenant.PK.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeatureFlagTenant {

    @Id
    @Column(name = "tenant_id", length = 50)
    private String tenantId;

    @Id
    @Column(length = 80)
    private String chave;

    @Column(nullable = false)
    private boolean ativo;

    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;

    @PrePersist @PreUpdate
    void touch() { atualizadoEm = LocalDateTime.now(); }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class PK implements Serializable {
        private String tenantId;
        private String chave;
        @Override public boolean equals(Object o) {
            if (!(o instanceof PK pk)) return false;
            return Objects.equals(tenantId, pk.tenantId) && Objects.equals(chave, pk.chave);
        }
        @Override public int hashCode() { return Objects.hash(tenantId, chave); }
    }
}
