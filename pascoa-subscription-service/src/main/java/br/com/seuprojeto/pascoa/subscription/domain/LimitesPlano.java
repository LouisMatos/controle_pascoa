package br.com.seuprojeto.pascoa.subscription.domain;

import java.util.Map;

/**
 * Limites por plano alinhados ao design v6 §5.4.
 * {@code Integer.MAX_VALUE} representa "ilimitado".
 */
public record LimitesPlano(
        int pedidosPorMes,
        int usuarios,
        int produtos,
        int notificacoesPorMes,
        long armazenamentoMb,
        boolean dominioProprio,
        boolean apiPublica,
        boolean exportacaoPdfExcel
) {

    public static final LimitesPlano TRIAL      = new LimitesPlano(50,    1, 20,     50,    100, false, false, false);
    public static final LimitesPlano STARTER    = new LimitesPlano(100,   1, 30,      0,    200, false, false, false);
    public static final LimitesPlano PRO        = new LimitesPlano(1_000, 5, 500, 5_000,  5_120, false, false, true);
    public static final LimitesPlano ENTERPRISE = new LimitesPlano(
            Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE,
            50_240, true, true, true);

    private static final Map<Plano, LimitesPlano> POR_PLANO = Map.of(
            Plano.TRIAL,      TRIAL,
            Plano.STARTER,    STARTER,
            Plano.PRO,        PRO,
            Plano.ENTERPRISE, ENTERPRISE
    );

    public static LimitesPlano de(Plano plano) {
        return POR_PLANO.getOrDefault(plano, STARTER);
    }
}
