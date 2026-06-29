package br.com.seuprojeto.pascoa.configengine.template;

import br.com.seuprojeto.pascoa.configengine.domain.ModeloPreco;
import br.com.seuprojeto.pascoa.configengine.domain.TipoNegocio;
import br.com.seuprojeto.pascoa.configengine.template.TemplateNegocio.AtributoProduto;

import java.util.List;
import java.util.Map;

/**
 * Catálogo dos 6 templates de negócio do design v6 §4. Estrutura imutável e
 * lookup O(1) por {@link TipoNegocio}.
 */
public final class TemplateCatalog {

    private TemplateCatalog() {}

    private static final Map<TipoNegocio, TemplateNegocio> TEMPLATES = Map.of(

        // ── 4.1 CONFEITARIA ────────────────────────────────────────────────
        TipoNegocio.CONFEITARIA, new TemplateNegocio(
            List.of("Bolo Simples", "Bolo Recheado", "Torta Doce", "Torta Salgada",
                    "Bolo de Festa", "Naked Cake", "Drip Cake", "Cupcake"),
            List.of("Unidade", "Fatia", "Porção", "kg"),
            List.of("Pesagem de ingredientes", "Preparo da massa", "Assamento",
                    "Recheio e montagem", "Cobertura", "Decoração", "Acabamento", "Embalagem"),
            List.of(
                AtributoProduto.select("tamanho_forma", "Tamanho da Fôrma", List.of("15cm","20cm","25cm","30cm")),
                AtributoProduto.numero("num_camadas",   "Nº de Camadas"),
                AtributoProduto.texto ("recheio_base",  "Recheio Base"),
                AtributoProduto.texto ("cobertura",     "Cobertura"),
                AtributoProduto.select("massa",         "Massa", List.of("pão de ló","amanteigada","red velvet"))
            ),
            ModeloPreco.POR_UNIDADE,
            3
        ),

        // ── 4.2 MARMITARIA ────────────────────────────────────────────────
        TipoNegocio.MARMITARIA, new TemplateNegocio(
            List.of("Tradicional", "Fitness/Low-Carb", "Vegana", "Vegetariana",
                    "Diet", "Infantil", "Executiva"),
            List.of("Porção P", "Porção M", "Porção G", "Porção GG", "Unidade", "Combo Semana", "Combo Quinzena", "Combo Mês"),
            List.of("Compra/separação de insumos", "Pré-preparo", "Cocção",
                    "Montagem das marmitas", "Selagem/embalagem", "Etiquetagem", "Refrigeração/expedição"),
            List.of(
                AtributoProduto.numero("calorias",     "Calorias (kcal)"),
                AtributoProduto.numero("proteinas_g",  "Proteínas (g)"),
                AtributoProduto.numero("carboidratos_g","Carboidratos (g)"),
                AtributoProduto.numero("gorduras_g",   "Gorduras (g)"),
                AtributoProduto.texto ("ingredientes", "Ingredientes"),
                AtributoProduto.texto ("alergenos",    "Alérgenos"),
                AtributoProduto.bool  ("sem_gluten",   "Sem glúten"),
                AtributoProduto.bool  ("sem_lactose",  "Sem lactose")
            ),
            ModeloPreco.POR_UNIDADE,
            1
        ),

        // ── 4.3 RESTAURANTE / BUFFET ──────────────────────────────────────
        TipoNegocio.RESTAURANTE, new TemplateNegocio(
            List.of("Prato Principal", "Acompanhamento", "Saladas",
                    "Sobremesas", "Bebidas", "Prato do Dia", "Executivo"),
            List.of("kg", "Porção", "Meia Porção", "Prato", "Por Pessoa"),
            List.of("Mise en place", "Preparo de bases e caldos", "Cocção principal",
                    "Finalização e tempero", "Montagem/apresentação", "Serviço/expedição"),
            List.of(
                AtributoProduto.select("tipo",                  "Tipo", List.of("quente","frio","salada")),
                AtributoProduto.numero("tempo_preparo_min",     "Tempo de preparo (min)"),
                AtributoProduto.texto ("ingredientes_principais","Ingredientes principais"),
                AtributoProduto.texto ("alergenos",             "Alérgenos"),
                AtributoProduto.numero("serve_pessoas",         "Serve quantas pessoas")
            ),
            ModeloPreco.POR_PESO,
            0
        ),

        // ── 4.4 SALGADERIA ────────────────────────────────────────────────
        TipoNegocio.SALGADERIA, new TemplateNegocio(
            List.of("Salgados Assados", "Salgados Fritos", "Tortas Salgadas",
                    "Mini-salgados", "Coxinhas Especiais", "Esfihas"),
            List.of("Unidade", "Dúzia", "50 unidades", "Cento", "kg", "Bandeja"),
            List.of("Preparo dos recheios", "Preparo da massa", "Modelagem/montagem",
                    "Descanso", "Fritura ou Assamento", "Controle de qualidade", "Embalagem"),
            List.of(
                AtributoProduto.texto ("tipo_massa", "Tipo de massa"),
                AtributoProduto.texto ("recheio",    "Recheio"),
                AtributoProduto.select("metodo",     "Método", List.of("assado","frito")),
                AtributoProduto.select("tamanho",    "Tamanho", List.of("mini","médio","grande")),
                AtributoProduto.bool  ("congelado_disponivel", "Congelado disponível?")
            ),
            ModeloPreco.FAIXA_VOLUME,
            2
        ),

        // ── 4.5 DOCES GOURMET (herança Páscoa) ────────────────────────────
        TipoNegocio.DOCES, new TemplateNegocio(
            List.of("Ovos de Páscoa", "Trufas", "Bombons", "Brigadeiros Gourmet",
                    "Barras de Chocolate", "Pirulitos", "Kits Presentes"),
            List.of("Unidade", "Caixa", "Bandeja", "kg", "Kit"),
            List.of("Temperagem do chocolate", "Moldagem", "Recheio", "Fechamento",
                    "Acabamento e decoração", "Resfriamento", "Embalagem personalizada"),
            List.of(
                AtributoProduto.select("tipo_chocolate", "Tipo de chocolate", List.of("ao leite","meio amargo","branco","ruby")),
                AtributoProduto.texto ("recheio",       "Recheio"),
                AtributoProduto.numero("peso_g",        "Peso (g)"),
                AtributoProduto.texto ("personalizacao","Personalização (texto, embalagem)")
            ),
            ModeloPreco.POR_UNIDADE,
            5
        ),

        // ── 4.6 CUSTOM ────────────────────────────────────────────────────
        TipoNegocio.CUSTOM, new TemplateNegocio(
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            ModeloPreco.POR_UNIDADE,
            0
        )
    );

    public static TemplateNegocio buscar(TipoNegocio tipo) {
        TemplateNegocio t = TEMPLATES.get(tipo);
        if (t == null) throw new IllegalArgumentException("Template desconhecido: " + tipo);
        return t;
    }
}
