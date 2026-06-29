package br.com.seuprojeto.pascoa.tenant.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "feature_flag", schema = "platform")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeatureFlag {

    @Id
    @Column(length = 80)
    private String chave;

    @Column(nullable = false, length = 500)
    private String descricao;

    @Column(name = "default_trial",      nullable = false) private boolean defaultTrial;
    @Column(name = "default_starter",    nullable = false) private boolean defaultStarter;
    @Column(name = "default_pro",        nullable = false) private boolean defaultPro;
    @Column(name = "default_enterprise", nullable = false) private boolean defaultEnterprise;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @PrePersist
    void onCreate() { if (criadoEm == null) criadoEm = LocalDateTime.now(); }

    public boolean defaultParaPlano(PlanoAssinatura plano) {
        return switch (plano) {
            case TRIAL      -> defaultTrial;
            case STARTER    -> defaultStarter;
            case PRO        -> defaultPro;
            case ENTERPRISE -> defaultEnterprise;
        };
    }
}
