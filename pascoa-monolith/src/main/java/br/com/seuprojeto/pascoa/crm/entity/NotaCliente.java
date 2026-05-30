package br.com.seuprojeto.pascoa.crm.entity;

import br.com.seuprojeto.pascoa.cadastro.entity.Cliente;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "notas_cliente")
@Data
@EqualsAndHashCode(of = "id")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotaCliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    @ToString.Exclude
    private Cliente cliente;

    @Column(nullable = false, length = 2000)
    private String texto;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @Column(name = "criado_por", length = 100)
    private String criadoPor;

    @PrePersist
    private void prePersist() {
        if (this.criadoEm == null) this.criadoEm = LocalDateTime.now();
    }
}
