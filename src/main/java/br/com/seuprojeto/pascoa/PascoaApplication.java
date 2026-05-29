package br.com.seuprojeto.pascoa;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PascoaApplication {

    public static void main(String[] args) {
        SpringApplication.run(PascoaApplication.class, args);
    }
}
