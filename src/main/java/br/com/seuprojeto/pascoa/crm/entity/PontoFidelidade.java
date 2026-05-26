package br.com.seuprojeto.pascoa.crm.entity;

import br.com.seuprojeto.pascoa.cadastro.entity.Cliente;
import br.com.seuprojeto.pascoa.pedido.entity.Pedido;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "pontos_fidelidade")
@Data
@EqualsAndHashCode(of = "id")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PontoFidelidade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    @ToString.Exclude
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_id")
    @ToString.Exclude
    private Pedido pedido;

    @Column(nullable = false)
    private Integer pontos;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TipoPonto tipo;

    @Column(length = 200)
    private String descricao;

    @Column(name = "data_operacao", nullable = false)
    private LocalDateTime dataOperacao;

    @Column(name = "data_expiracao")
    private LocalDate dataExpiracao;

    @PrePersist
    private void prePersist() {
        if (this.dataOperacao == null) this.dataOperacao = LocalDateTime.now();
    }
}
