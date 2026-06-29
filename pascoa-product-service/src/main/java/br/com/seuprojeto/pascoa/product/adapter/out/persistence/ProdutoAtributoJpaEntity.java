package br.com.seuprojeto.pascoa.product.adapter.out.persistence;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "produto_atributo",
       uniqueConstraints = @UniqueConstraint(name = "uq_produto_atributo",
                                             columnNames = {"produto_id","atributo_key"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProdutoAtributoJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "produto_id", nullable = false)
    private Long produtoId;

    @Column(name = "atributo_key", nullable = false, length = 80)
    private String atributoKey;

    @Column(name = "atributo_value", length = 500)
    private String atributoValue;
}
