package br.com.seuprojeto.pascoa.configengine.dto;

import br.com.seuprojeto.pascoa.configengine.domain.ModeloPreco;
import br.com.seuprojeto.pascoa.configengine.domain.TipoNegocio;
import br.com.seuprojeto.pascoa.configengine.template.TemplateNegocio.AtributoProduto;

import java.util.List;

public record ConfigEfetivaResponse(
        String tenantId,
        TipoNegocio tipoNegocio,
        List<String> categorias,
        List<String> unidades,
        List<String> fasesProducao,
        List<AtributoProduto> atributosProduto,
        ModeloPreco modeloPreco,
        int antecedenciaMinimaDias
) {}
