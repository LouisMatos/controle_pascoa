package br.com.seuprojeto.pascoa.commons.whitelabel;

public class NoOpWhiteLabelClient implements WhiteLabelClient {
    @Override public WhiteLabelTheme buscar(String tenantId) {
        return WhiteLabelTheme.DEFAULT;
    }
}
