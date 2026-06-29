package br.com.seuprojeto.pascoa.configengine.template;

import br.com.seuprojeto.pascoa.configengine.domain.ModeloPreco;

import java.util.List;

/**
 * Pré-configuração imutável de um tipo de negócio. Os 6 templates do design v6 §4
 * são definidos como código em {@link TemplateCatalog} (mais simples e auditável
 * que seeds SQL para JSONB).
 */
public record TemplateNegocio(
        List<String> categorias,
        List<String> unidades,
        List<String> fasesProducao,
        List<AtributoProduto> atributosProduto,
        ModeloPreco modeloPreco,
        int antecedenciaMinimaDias
) {
    public record AtributoProduto(String chave, String label, String tipo, List<String> opcoes) {
        public static AtributoProduto texto(String chave, String label)     { return new AtributoProduto(chave, label, "TEXTO",   null); }
        public static AtributoProduto numero(String chave, String label)    { return new AtributoProduto(chave, label, "NUMERO",  null); }
        public static AtributoProduto bool(String chave, String label)      { return new AtributoProduto(chave, label, "BOOLEAN", null); }
        public static AtributoProduto select(String chave, String label, List<String> opcoes) { return new AtributoProduto(chave, label, "SELECT", opcoes); }
    }
}
