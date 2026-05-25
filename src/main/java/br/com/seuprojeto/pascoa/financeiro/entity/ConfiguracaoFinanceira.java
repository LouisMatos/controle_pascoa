package br.com.seuprojeto.pascoa.financeiro.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "configuracao_financeira")
@Data
@EqualsAndHashCode(of = "id")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfiguracaoFinanceira {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Margem de lucro desejada padrão (%) — usada como sugestão nas fichas técnicas
    @NotNull(message = "Margem desejada padrão é obrigatória")
    @DecimalMin(value = "0.0", message = "Margem não pode ser negativa")
    @Column(name = "margem_desejada_padrao", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal margemDesejadaPadrao = new BigDecimal("30.00");

    // Alíquota do Simples Nacional (%) — deduzida do faturamento
    @NotNull(message = "Alíquota é obrigatória")
    @DecimalMin(value = "0.0", message = "Alíquota não pode ser negativa")
    @Column(name = "aliquota_simples", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal aliquotaSimples = new BigDecimal("6.00");

    @NotNull(message = "Meta de faturamento é obrigatória")
    @DecimalMin(value = "0", inclusive = true, message = "Meta não pode ser negativa")
    @Column(name = "meta_faturamento_mensal", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal metaFaturamentoMensal = BigDecimal.ZERO;

    @NotBlank(message = "Moeda é obrigatória")
    @Size(max = 5)
    @Column(nullable = false, length = 5)
    @Builder.Default
    private String moeda = "BRL";
}
