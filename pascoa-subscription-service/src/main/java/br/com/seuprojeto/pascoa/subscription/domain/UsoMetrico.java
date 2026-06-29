package br.com.seuprojeto.pascoa.subscription.domain;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Objects;

@Entity
@Table(name = "uso_metrico", schema = "subscription")
@IdClass(UsoMetrico.PK.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UsoMetrico {

    @Id
    @Column(name = "tenant_id", length = 50)
    private String tenantId;

    /** Formato 'YYYY-MM' — ex: "2026-05". */
    @Id
    @Column(name = "competencia", length = 7)
    private String competencia;

    @Column(name = "pedidos_total",       nullable = false) private int pedidosTotal;
    @Column(name = "usuarios_ativos",     nullable = false) private int usuariosAtivos;
    @Column(name = "notificacoes_total",  nullable = false) private int notificacoesTotal;
    @Column(name = "armazenamento_mb",    nullable = false) private long armazenamentoMb;

    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;

    @PrePersist @PreUpdate
    void touch() { atualizadoEm = LocalDateTime.now(); }

    public static String competenciaAtual() {
        return YearMonth.now().toString();
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class PK implements Serializable {
        private String tenantId;
        private String competencia;
        @Override public boolean equals(Object o) {
            if (!(o instanceof PK pk)) return false;
            return Objects.equals(tenantId, pk.tenantId) && Objects.equals(competencia, pk.competencia);
        }
        @Override public int hashCode() { return Objects.hash(tenantId, competencia); }
    }
}
