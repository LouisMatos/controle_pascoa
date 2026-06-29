package br.com.seuprojeto.pascoa.product.adapter.out.persistence;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "produto_ficha_nutricional")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProdutoFichaNutricionalJpaEntity {

    @Id
    @Column(name = "produto_id")
    private Long produtoId;

    @Column(name = "porcao_g",            precision = 8, scale = 2) private BigDecimal porcaoG;
    @Column(name = "calorias",            precision = 8, scale = 2) private BigDecimal calorias;
    @Column(name = "proteinas_g",         precision = 8, scale = 2) private BigDecimal proteinasG;
    @Column(name = "carboidratos_g",      precision = 8, scale = 2) private BigDecimal carboidratosG;
    @Column(name = "gorduras_g",          precision = 8, scale = 2) private BigDecimal gordurasG;
    @Column(name = "gorduras_saturadas_g",precision = 8, scale = 2) private BigDecimal gordurasSaturadasG;
    @Column(name = "fibras_g",            precision = 8, scale = 2) private BigDecimal fibrasG;
    @Column(name = "sodio_mg",            precision = 8, scale = 2) private BigDecimal sodioMg;
    @Column(name = "acucares_g",          precision = 8, scale = 2) private BigDecimal acucaresG;

    @Column(name = "sem_gluten",  nullable = false) private boolean semGluten;
    @Column(name = "sem_lactose", nullable = false) private boolean semLactose;
    @Column(nullable = false)                       private boolean vegano;
    @Column(nullable = false)                       private boolean vegetariano;

    @Column(length = 500)
    private String alergenos;

    @UpdateTimestamp
    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;
}
