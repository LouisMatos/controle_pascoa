package br.com.seuprojeto.pascoa.common.tenant;

import java.util.function.Supplier;

public final class TenantContext {

    public static final long SEM_TENANT = 0L;

    public static final String LOJA_ATUAL_SPEL =
        ":#{T(br.com.seuprojeto.pascoa.common.tenant.TenantContext).atual()}";

    private static final ThreadLocal<Long> ATUAL = new ThreadLocal<>();

    private TenantContext() {
    }

    public interface Escopo extends AutoCloseable {
        @Override
        void close();
    }

    public static void set(Long lojaId) {
        ATUAL.set(lojaId);
    }

    public static long atual() {
        Long id = ATUAL.get();
        return id == null ? SEM_TENANT : id;
    }

    public static long exigir() {
        Long id = ATUAL.get();
        if (id == null) {
            throw new IllegalStateException("Nenhuma loja no contexto");
        }
        return id;
    }

    public static void limpar() {
        ATUAL.remove();
    }

    public static Escopo abrir(Long lojaId) {
        Long anterior = ATUAL.get();
        ATUAL.set(lojaId);
        return () -> {
            if (anterior == null) {
                ATUAL.remove();
            } else {
                ATUAL.set(anterior);
            }
        };
    }

    public static void executar(Long lojaId, Runnable acao) {
        try (var escopo = abrir(lojaId)) {
            acao.run();
        }
    }

    public static <T> T calcular(Long lojaId, Supplier<T> acao) {
        try (var escopo = abrir(lojaId)) {
            return acao.get();
        }
    }
}
