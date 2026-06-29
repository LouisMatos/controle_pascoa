package br.com.seuprojeto.pascoa.tenant.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * S-03 — rate limiter para o endpoint público {@code POST /tenants/registrar}.
 *
 * Implementação in-memory com janela deslizante: cada IP pode fazer no máximo
 * {@code max} requisições dentro de {@code janela}. O custo de criar um tenant
 * é alto (provisionar schema + Liquibase + popular templates), então um
 * atacante poderia exaurir o banco enviando POSTs em loop.
 *
 * Para implantações multi-instância, a chave deveria viver no Redis — esta
 * versão atende o caso single-node atual e fail-open em caso de bug
 * (segurança não pode quebrar o endpoint legítimo).
 */
@Component
@Slf4j
public class RegistrarRateLimiter {

    private final int max;
    private final Duration janela;

    /** IP → timestamps das requisições recentes (ordenados do mais antigo para o mais novo). */
    private final Map<String, Deque<Instant>> historico = new ConcurrentHashMap<>();

    public RegistrarRateLimiter(@Value("${foodflow.tenant.register.max:3}") int max,
                                @Value("${foodflow.tenant.register.window:PT1H}") Duration janela) {
        this.max = max;
        this.janela = janela;
    }

    /**
     * @return {@code true} se a requisição é permitida; {@code false} se o IP
     *         excedeu a quota dentro da janela.
     */
    public boolean permitir(String ip) {
        if (ip == null || ip.isBlank()) ip = "unknown";
        Instant agora = Instant.now();
        Instant corte = agora.minus(janela);

        Deque<Instant> fila = historico.computeIfAbsent(ip, k -> new ArrayDeque<>());
        synchronized (fila) {
            // Expira entradas antigas.
            Iterator<Instant> it = fila.iterator();
            while (it.hasNext() && it.next().isBefore(corte)) it.remove();

            if (fila.size() >= max) {
                log.warn("Rate limit excedido em /tenants/registrar para IP {} ({} reqs em {}).",
                        ip, fila.size(), janela);
                return false;
            }
            fila.addLast(agora);
            return true;
        }
    }
}
