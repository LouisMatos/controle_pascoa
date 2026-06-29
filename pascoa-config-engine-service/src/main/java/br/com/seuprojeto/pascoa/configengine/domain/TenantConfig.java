package br.com.seuprojeto.pascoa.configengine.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "tenant_config", schema = "config")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TenantConfig {

    @Id
    @Column(name = "tenant_id", length = 50)
    private String tenantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_negocio", nullable = false, length = 20)
    private TipoNegocio tipoNegocio;

    /** JSON: List&lt;String&gt; */
    @Column(name = "categorias_json",      columnDefinition = "text") private String categoriasJson;
    /** JSON: List&lt;String&gt; */
    @Column(name = "unidades_json",        columnDefinition = "text") private String unidadesJson;
    /** JSON: List&lt;String&gt; */
    @Column(name = "fases_producao_json",  columnDefinition = "text") private String fasesProducaoJson;
    /** JSON: List&lt;AtributoProduto&gt; */
    @Column(name = "atributos_produto_json", columnDefinition = "text") private String atributosProdutoJson;

    @Enumerated(EnumType.STRING)
    @Column(name = "modelo_preco", nullable = false, length = 20)
    private ModeloPreco modeloPreco;

    @Column(name = "antecedencia_min_dias", nullable = false)
    private int antecedenciaMinimaDias;

    @Column(name = "criado_em",     nullable = false) private LocalDateTime criadoEm;
    @Column(name = "atualizado_em") private LocalDateTime atualizadoEm;

    @PrePersist void onCreate() { if (criadoEm == null) criadoEm = LocalDateTime.now(); }
    @PreUpdate  void onUpdate() { atualizadoEm = LocalDateTime.now(); }
}
