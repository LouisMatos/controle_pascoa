package br.com.seuprojeto.pascoa.gateway.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/fallback")
public class FallbackController {

    /**
     * Fallback do circuit breaker da rota catch-all do monólito (porta 8080).
     *
     * <p>Quando a UI legada não está no ar, o usuário é silenciosamente levado
     * ao ambiente moderno — sem mensagem de erro nem qualquer menção a "monólito
     * desativado". O formato é decidido pelo {@code Accept}:</p>
     *
     * <ul>
     *   <li>Navegador ({@code text/html}) → {@code 302 Location: /login.html}.</li>
     *   <li>API ({@code application/json} ou outro) → {@code 404 Not Found}.</li>
     * </ul>
     *
     * <p>Aceita qualquer método HTTP porque o forward interno do circuit breaker
     * preserva o verbo original — POSTs e PUTs que falharam não devem cair em
     * "method not allowed".</p>
     */
    @RequestMapping("/monolith")
    public Mono<ResponseEntity<Void>> monolithFallback(
            @RequestHeader(value = HttpHeaders.ACCEPT, required = false) String accept) {

        boolean wantsHtml = accept != null && accept.contains(MediaType.TEXT_HTML_VALUE);

        if (wantsHtml) {
            return Mono.just(ResponseEntity
                    .status(HttpStatus.FOUND)
                    .header(HttpHeaders.LOCATION, "/login.html")
                    .build());
        }

        return Mono.just(ResponseEntity.notFound().build());
    }
}
