package br.com.seuprojeto.pascoa.commons.whitelabel;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class WhiteLabelAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(WhiteLabelClient.class)
    public WhiteLabelClient whiteLabelClient() {
        return new NoOpWhiteLabelClient();
    }

    /** Inicializa o helper estático ao subir o contexto. */
    @Bean
    public WhiteLabelBootstrap whiteLabelBootstrap(WhiteLabelClient client) {
        WhiteLabel.setClient(client);
        return new WhiteLabelBootstrap();
    }

    static class WhiteLabelBootstrap {}
}
