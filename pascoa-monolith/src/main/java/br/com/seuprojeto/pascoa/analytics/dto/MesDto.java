package br.com.seuprojeto.pascoa.analytics.dto;

import java.math.BigDecimal;

public record MesDto(int mes, BigDecimal faturamento, long pedidos) {

    public String nomeMes() {
        return switch (mes) {
            case 1  -> "Jan";
            case 2  -> "Fev";
            case 3  -> "Mar";
            case 4  -> "Abr";
            case 5  -> "Mai";
            case 6  -> "Jun";
            case 7  -> "Jul";
            case 8  -> "Ago";
            case 9  -> "Set";
            case 10 -> "Out";
            case 11 -> "Nov";
            case 12 -> "Dez";
            default -> "?";
        };
    }
}
