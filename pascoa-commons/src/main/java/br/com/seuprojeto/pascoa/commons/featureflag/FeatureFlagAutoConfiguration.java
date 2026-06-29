package br.com.seuprojeto.pascoa.commons.featureflag;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class FeatureFlagAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(FeatureFlagClient.class)
    public FeatureFlagClient featureFlagClient() {
        return new NoOpFeatureFlagClient();
    }

    /** Inicializa o helper estático {@link FeatureFlags} ao subir o contexto Spring. */
    @Bean
    public FeatureFlagBootstrap featureFlagBootstrap(FeatureFlagClient client) {
        FeatureFlags.setClient(client);
        return new FeatureFlagBootstrap();
    }

    static class FeatureFlagBootstrap {}
}
