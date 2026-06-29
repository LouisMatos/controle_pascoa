package br.com.seuprojeto.pascoa.product.adapter.out.persistence;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "produto_variante")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProdutoVarianteJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "produto_id", nullable = false)
    private Long produtoId;

    @Column(length = 50, unique = true)
    private String sku;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal preco;

    /** JSON serializado de Map&lt;String,String&gt; — atributos específicos da variante. */
    @Column(name = "atributos_json", columnDefinition = "text")
    private String atributosJson;

    @Column(nullable = false)
    private boolean disponivel = true;

    @CreationTimestamp
    @Column(name = "criado_em", updatable = false)
    private LocalDateTime criadoEm;
}
