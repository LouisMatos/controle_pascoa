package br.com.seuprojeto.pascoa.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // Spring Security cuida do POST /login; apenas mapeamos o GET para a view
        registry.addViewController("/login").setViewName("login");
    }
}
