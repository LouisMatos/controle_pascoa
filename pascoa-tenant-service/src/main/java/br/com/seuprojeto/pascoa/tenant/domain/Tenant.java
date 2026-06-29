package br.com.seuprojeto.pascoa.tenant.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "tenant", schema = "platform")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tenant {

    /** Slug usado também como nome do schema PostgreSQL do tenant. Validação: [a-z0-9_-]+ */
    @Id
    @Column(length = 50, nullable = false)
    private String id;

    @Column(name = "razao_social", nullable = false, length = 200)
    private String razaoSocial;

    @Column(name = "cnpj_cpf", length = 20)
    private String cnpjCpf;

    @Column(nullable = false, length = 200)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PlanoAssinatura plano;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_negocio", nullable = false, length = 20)
    private TipoNegocio tipoNegocio;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusTenant status;

    @Column(name = "data_expiracao")
    private LocalDate dataExpiracao;

    @Column(name = "dominio_custom", length = 200)
    private String dominioCustom;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;

    @PrePersist
    void onCreate() {
        if (criadoEm == null) criadoEm = LocalDateTime.now();
        if (status == null)   status   = StatusTenant.TRIAL;
        if (plano == null)    plano    = PlanoAssinatura.TRIAL;
    }

    @PreUpdate
    void onUpdate() { atualizadoEm = LocalDateTime.now(); }
}
