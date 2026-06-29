package br.com.seuprojeto.pascoa.commons.featureflag;

/** Implementação default: nada de v6 ainda ativo. */
public class NoOpFeatureFlagClient implements FeatureFlagClient {
    @Override
    public boolean isAtivo(String tenantId, String chave) {
        return false;
    }
}
