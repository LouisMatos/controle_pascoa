package br.com.seuprojeto.pascoa.analytics.adapter.out.persistence;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "registros_venda", indexes = {
        @Index(name = "idx_rv_ano",       columnList = "ano"),
        @Index(name = "idx_rv_produto",   columnList = "produto_id, ano"),
        @Index(name = "idx_rv_pedido",    columnList = "pedido_id")
})
@Getter
@Setter
public class RegistroVendaJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pedido_id", nullable = false)
    private Long pedidoId;

    @Column(name = "cliente_id", nullable = false)
    private Long clienteId;

    @Column(name = "produto_id", nullable = false)
    private Long produtoId;

    @Column(name = "nome_produto", nullable = false, length = 150)
    private String nomeProduto;

    @Column(nullable = false)
    private int quantidade;

    @Column(name = "valor_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorTotal;

    @Column(name = "data_venda", nullable = false)
    private LocalDate dataVenda;

    @Column(nullable = false)
    private int ano;

    @Column(nullable = false)
    private int mes;

    @CreationTimestamp
    @Column(name = "criado_em", updatable = false)
    private LocalDateTime criadoEm;
}
