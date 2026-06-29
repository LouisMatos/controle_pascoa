package br.com.seuprojeto.pascoa.production.adapter.out.persistence;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "historico_fase")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class HistoricoFaseJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ordem_id", nullable = false)
    private Long ordemId;

    @Column(name = "fase_de", length = 80)
    private String faseDe;

    @Column(name = "fase_para", nullable = false, length = 80)
    private String fasePara;

    @Column(name = "ordem_de")
    private Integer ordemDe;

    @Column(name = "ordem_para", nullable = false)
    private Integer ordemPara;

    @CreationTimestamp
    @Column(name = "mudado_em", nullable = false, updatable = false)
    private LocalDateTime mudadoEm;

    @Column(name = "mudado_por", length = 80)
    private String mudadoPor;

    @Column(length = 500)
    private String observacao;
}
