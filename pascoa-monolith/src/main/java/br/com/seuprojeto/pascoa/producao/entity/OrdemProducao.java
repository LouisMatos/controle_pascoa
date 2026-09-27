package br.com.seuprojeto.pascoa.producao.entity;

import br.com.seuprojeto.pascoa.cadastro.entity.Produto;
import br.com.seuprojeto.pascoa.pedido.entity.Pedido;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

@Entity
@Table(name = "ordens_producao")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class OrdemProducao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_id")
    @ToString.Exclude
    private Pedido pedido;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "produto_id", nullable = false)
    private Produto produto;

    @Column(nullable = false)
    private Integer quantidade;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private StatusOrdem status = StatusOrdem.PENDENTE;

    private LocalDateTime dataAbertura;
    private LocalDateTime dataConclusao;

    @Column(length = 500)
    private String observacoes;

    @PrePersist
    private void prePersist() {
        if (dataAbertura == null) { dataAbertura = LocalDateTime.now(); }
    }
}
