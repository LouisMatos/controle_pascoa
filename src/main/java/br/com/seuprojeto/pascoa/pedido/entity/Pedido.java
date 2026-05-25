package br.com.seuprojeto.pascoa.pedido.entity;

import br.com.seuprojeto.pascoa.cadastro.entity.Cliente;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "pedidos")
@Data
@EqualsAndHashCode(of = "id")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    @ToString.Exclude
    private Cliente cliente;

    @Column(name = "data_pedido", nullable = false, updatable = false)
    private LocalDateTime dataPedido;

    @Column(name = "data_entrega")
    private LocalDate dataEntrega;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private StatusPedido status = StatusPedido.NOVO;

    @Column(length = 500)
    private String observacoes;

    @Column(name = "total_pedido", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal totalPedido = BigDecimal.ZERO;

    @Column(name = "token_acompanhamento", unique = true, length = 36)
    private String tokenAcompanhamento;

    @Column(name = "slot_entrega")
    private LocalTime slotEntrega;

    @Column(name = "custo_real_calculado", precision = 10, scale = 2)
    private BigDecimal custoRealCalculado;

    @Column(name = "margem_real_calculada", precision = 5, scale = 2)
    private BigDecimal margemRealCalculada;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @ToString.Exclude
    private List<ItemPedido> itens = new ArrayList<>();

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @ToString.Exclude
    private List<Pagamento> pagamentos = new ArrayList<>();

    @PrePersist
    private void prePersist() {
        this.dataPedido = LocalDateTime.now();
        if (this.status == null) this.status = StatusPedido.NOVO;
        if (this.totalPedido == null) this.totalPedido = BigDecimal.ZERO;
        if (this.tokenAcompanhamento == null) this.tokenAcompanhamento = UUID.randomUUID().toString();
    }
}
