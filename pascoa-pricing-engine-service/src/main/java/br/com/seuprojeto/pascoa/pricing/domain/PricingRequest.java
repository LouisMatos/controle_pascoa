package br.com.seuprojeto.pascoa.pricing.domain;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Entrada do cálculo. Cada modelo lê os campos que lhe interessam:
 *
 * <ul>
 *   <li>POR_UNIDADE     → precoBase × quantidade</li>
 *   <li>POR_PESO        → precoBase × pesoKg</li>
 *   <li>POR_PORCAO      → precoBase × qtdPorcoes</li>
 *   <li>POR_COMBO       → soma de itens − descontoCombo</li>
 *   <li>FAIXA_VOLUME    → tabela faixas + quantidade</li>
 *   <li>FORMULA_CUSTOM  → expressão SpEL avaliada com {@code variaveis}</li>
 *   <li>ASSINATURA      → precoBase × periodos × (1 − descontoFidelidade)</li>
 * </ul>
 */
public record PricingRequest(
        ModeloPreco modelo,
        BigDecimal precoBase,
        BigDecimal quantidade,
        BigDecimal pesoKg,
        Integer qtdPorcoes,
        List<ItemCombo> itensCombo,
        BigDecimal descontoCombo,
        List<FaixaVolume> faixas,
        String formula,
        Map<String, Object> variaveis,
        Integer periodos,
        BigDecimal descontoFidelidade
) {
    public record ItemCombo(BigDecimal precoItem, BigDecimal qtd) {}
    public record FaixaVolume(int minQtd, int maxQtd, BigDecimal precoUnitario) {}
}
