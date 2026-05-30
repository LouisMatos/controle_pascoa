package br.com.seuprojeto.pascoa.notification.domain.service;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TemplateEngine {

    private static final Pattern VAR_PATTERN = Pattern.compile("\\{([^}]+)}");

    private TemplateEngine() {}

    /**
     * Substitui {variavel} no template pelos valores do mapa.
     * Variáveis ausentes no mapa ficam como "{variavel}" sem substituição.
     */
    public static String render(String template, Map<String, String> variaveis) {
        if (template == null || template.isBlank()) return "";
        if (variaveis == null || variaveis.isEmpty()) return template;

        Matcher matcher = VAR_PATTERN.matcher(template);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String chave = matcher.group(1);
            String valor = variaveis.getOrDefault(chave, matcher.group(0));
            matcher.appendReplacement(result, Matcher.quoteReplacement(valor));
        }
        matcher.appendTail(result);
        return result.toString();
    }
}
