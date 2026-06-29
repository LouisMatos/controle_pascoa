package br.com.seuprojeto.pascoa.tenantAdmin.util;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Validador de contraste WCAG 2.1 para a paleta white-label do tenant
 * (FASE 7.6 — design v7).
 *
 * Calcula a relação de contraste entre duas cores (hex) seguindo o
 * algoritmo oficial:
 *   contrast = (L1 + 0.05) / (L2 + 0.05)
 * onde L1 = luminância relativa da cor mais clara e L2 da mais escura.
 *
 * Critérios WCAG:
 *   AA  texto normal: >= 4.5
 *   AA  texto grande: >= 3.0
 *   AAA texto normal: >= 7.0
 *   AAA texto grande: >= 4.5
 *
 * Retorna {@link ContrastResult} para que o frontend exiba aviso
 * (badge "⚠️ AA"/"❌ Falha"/"✅ AAA").
 */
@Component
public class TenantColorValidator {

    // W-01 — bloqueia CSS injection via corPrimaria. Aceita APENAS #RRGGBB.
    private static final Pattern HEX_COLOR = Pattern.compile("^#[0-9A-Fa-f]{6}$");

    public static class CorInvalidaException extends RuntimeException {
        public CorInvalidaException(String msg) { super(msg); }
    }

    /**
     * Valida que a cor está no formato #RRGGBB. Lança {@link CorInvalidaException}
     * caso contrário. Deve ser chamado SEMPRE antes de persistir uma cor do tenant
     * — caso contrário um valor como "red}body{display:none;/*" pode corromper
     * toda a folha de estilos quando injetado via th:inline="text".
     */
    public void validarFormato(String cor) {
        if (cor == null || !HEX_COLOR.matcher(cor).matches()) {
            throw new CorInvalidaException("Cor deve estar no formato #RRGGBB: " + cor);
        }
    }

    public enum Nivel { AAA, AA, AA_LARGE, FAIL }

    public record ContrastResult(
            double ratio,
            Nivel nivel,
            boolean atendeAA,
            boolean atendeAAA,
            String mensagem
    ) {}

    /**
     * Valida o contraste entre brand-primary e o background sobre o qual
     * o texto branco aparecerá (texto da sidebar, do botão primary, etc.).
     *
     * @param hexForeground hex do texto (ex.: "#ffffff")
     * @param hexBackground hex do fundo (ex.: "#e91e63")
     */
    public ContrastResult validar(String hexForeground, String hexBackground) {
        double lumFg = luminanciaRelativa(hexToRgb(hexForeground));
        double lumBg = luminanciaRelativa(hexToRgb(hexBackground));

        double lighter = Math.max(lumFg, lumBg);
        double darker  = Math.min(lumFg, lumBg);
        double ratio = (lighter + 0.05) / (darker + 0.05);

        Nivel nivel;
        boolean atendeAA, atendeAAA;
        String msg;

        if (ratio >= 7.0)      { nivel = Nivel.AAA;      atendeAA = true;  atendeAAA = true;
                                  msg = "Excelente — atende WCAG AAA."; }
        else if (ratio >= 4.5) { nivel = Nivel.AA;       atendeAA = true;  atendeAAA = false;
                                  msg = "Bom — atende WCAG AA para texto normal."; }
        else if (ratio >= 3.0) { nivel = Nivel.AA_LARGE; atendeAA = false; atendeAAA = false;
                                  msg = "Aceitável apenas para texto grande (>=18pt). Considere uma cor com mais contraste."; }
        else                   { nivel = Nivel.FAIL;     atendeAA = false; atendeAAA = false;
                                  msg = "Contraste insuficiente. Texto pode ficar ilegível — escolha uma cor mais escura."; }

        return new ContrastResult(Math.round(ratio * 100) / 100.0, nivel, atendeAA, atendeAAA, msg);
    }

    // ── helpers ──────────────────────────────────────────────────────

    private int[] hexToRgb(String hex) {
        if (hex == null) throw new IllegalArgumentException("hex nulo");
        String h = hex.startsWith("#") ? hex.substring(1) : hex;
        if (h.length() == 3) {
            h = "" + h.charAt(0) + h.charAt(0) + h.charAt(1) + h.charAt(1) + h.charAt(2) + h.charAt(2);
        }
        if (h.length() != 6) throw new IllegalArgumentException("hex inválido: " + hex);
        return new int[] {
                Integer.parseInt(h.substring(0, 2), 16),
                Integer.parseInt(h.substring(2, 4), 16),
                Integer.parseInt(h.substring(4, 6), 16)
        };
    }

    private double luminanciaRelativa(int[] rgb) {
        double r = canal(rgb[0]);
        double g = canal(rgb[1]);
        double b = canal(rgb[2]);
        return 0.2126 * r + 0.7152 * g + 0.0722 * b;
    }

    private double canal(int v) {
        double s = v / 255.0;
        return (s <= 0.03928) ? (s / 12.92) : Math.pow((s + 0.055) / 1.055, 2.4);
    }
}
