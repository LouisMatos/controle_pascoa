package br.com.seuprojeto.pascoa.orcamento.entity;

import br.com.seuprojeto.pascoa.cadastro.entity.Cliente;
import br.com.seuprojeto.pascoa.pedido.entity.Pedido;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orcamentos")
@Data
@EqualsAndHashCode(of = "id")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Orcamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    @ToString.Exclude
    private Cliente cliente;

    @Column(name = "data_criacao", nullable = false)
    private LocalDateTime dataCriacao;

    @Column(nullable = false)
    private LocalDate validade;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    @Builder.Default
    private StatusOrcamento status = StatusOrcamento.PENDENTE;

    @Column(nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal total = BigDecimal.ZERO;

    @Column(length = 1000)
    private String observacoes;

    @Column(name = "token_aprovacao", unique = true, length = 36)
    private String tokenAprovacao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_id")
    @ToString.Exclude
    private Pedido pedido;

    @Column(name = "criado_por", length = 100)
    private String criadoPor;

    @OneToMany(mappedBy = "orcamento", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @ToString.Exclude
    private List<OrcamentoItem> itens = new ArrayList<>();

    @PrePersist
    private void prePersist() {
        if (dataCriacao == null) dataCriacao = LocalDateTime.now();
        if (status == null) status = StatusOrcamento.PENDENTE;
        if (total == null) total = BigDecimal.ZERO;
        if (tokenAprovacao == null) tokenAprovacao = UUID.randomUUID().toString();
    }

    public boolean isPendente() {
        return status == StatusOrcamento.PENDENTE;
    }

    public boolean isAprovado() {
        return status == StatusOrcamento.APROVADO;
    }

    public boolean isExpirado() {
        return status == StatusOrcamento.PENDENTE
                && validade != null
                && LocalDate.now().isAfter(validade);
    }
}
