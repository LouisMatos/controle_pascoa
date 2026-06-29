package br.com.seuprojeto.pascoa.production.adapter.out.persistence;

import br.com.seuprojeto.pascoa.production.domain.model.StatusOrdem;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ordens_producao")
@Getter
@Setter
public class OrdemProducaoJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pedido_id", nullable = false, unique = true)
    private Long pedidoId;

    @Column(name = "nome_cliente", nullable = false, length = 150)
    private String nomeCliente;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusOrdem status;

    @Column(name = "data_previsao")
    private LocalDate dataPrevisao;

    @Column(length = 500)
    private String observacoes;

    // ── v6 Etapa 9: fases configuráveis por tenant ──
    @Column(name = "tenant_id", length = 50)
    private String tenantId;

    /** Nome da fase atual conforme {@code config-engine}. Coexiste com {@link StatusOrdem}. */
    @Column(name = "fase_atual", length = 80)
    private String faseAtual;

    @Column(name = "fase_ordem", nullable = false)
    private int faseOrdem = 0;

    @Column(name = "iniciada_em")
    private LocalDateTime iniciadaEm;

    @Column(name = "concluida_em")
    private LocalDateTime concluidaEm;

    @OneToMany(mappedBy = "ordem", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<ItemOrdemJpaEntity> itens = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "criado_em", updatable = false)
    private LocalDateTime criadoEm;

    @UpdateTimestamp
    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;
}
