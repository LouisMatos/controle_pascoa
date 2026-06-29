package br.com.seuprojeto.pascoa.tenant.service;

import br.com.seuprojeto.pascoa.tenant.domain.ApiKey;
import br.com.seuprojeto.pascoa.tenant.exception.TenantException;
import br.com.seuprojeto.pascoa.tenant.exception.TenantNotFoundException;
import br.com.seuprojeto.pascoa.tenant.repository.ApiKeyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

/**
 * v6 Etapa 15 — Geração e validação de chaves de API.
 *
 * <p>Segurança:
 * <ul>
 *   <li>Plain-text gerado com {@link SecureRandom} (32 bytes → 64 chars hex)</li>
 *   <li>Apenas o hash SHA-256 é persistido</li>
 *   <li>O plain só é retornado UMA vez na criação — o servidor não consegue
 *       recuperá-lo depois</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ApiKeyService {

    private static final String KEY_PREFIX = "ff_live_";
    private static final SecureRandom RNG = new SecureRandom();

    private final ApiKeyRepository repo;
    private final TenantService tenantService;

    /** Resultado da criação: contém o plain-text que NÃO será exibido novamente. */
    public record ChaveCriada(ApiKey metadata, String plainKey) {}

    public ChaveCriada criar(String tenantId, String nome, String escopos, LocalDate expiraEm) {
        // valida tenant
        if (tenantService.buscarPorId(tenantId) == null) {
            throw new TenantNotFoundException(tenantId);
        }

        String plain = gerarPlain();
        String hash = sha256(plain);
        String prefix = plain.substring(0, Math.min(12, plain.length()));

        ApiKey key = ApiKey.builder()
                .tenantId(tenantId)
                .nome(nome != null ? nome : "default")
                .chaveHash(hash)
                .chavePrefix(prefix)
                .escopos(escopos != null ? escopos : "read")
                .ativa(true)
                .expiraEm(expiraEm)
                .build();
        repo.save(key);
        log.info("ApiKey criada: tenant={} nome={} prefix={}", tenantId, key.getNome(), prefix);
        return new ChaveCriada(key, plain);
    }

    @Transactional(readOnly = true)
    public List<ApiKey> listar(String tenantId) {
        return repo.findByTenantIdOrderByCriadaEmDesc(tenantId);
    }

    public void revogar(Long keyId) {
        ApiKey k = repo.findById(keyId)
                .orElseThrow(() -> new TenantException("ApiKey não encontrada: " + keyId));
        k.setAtiva(false);
        repo.save(k);
    }

    /**
     * Valida uma chave plain-text recebida em request. Retorna a {@link ApiKey}
     * vigente ou {@link Optional#empty()} se inválida/expirada. Atualiza
     * {@code ultima_usada_em} em hit.
     */
    public Optional<ApiKey> validar(String plainKey) {
        if (plainKey == null || plainKey.isBlank()) return Optional.empty();
        Optional<ApiKey> maybe = repo.findByChaveHashAndAtivaTrue(sha256(plainKey));
        if (maybe.isEmpty()) return Optional.empty();
        ApiKey k = maybe.get();
        if (k.getExpiraEm() != null && LocalDate.now().isAfter(k.getExpiraEm())) {
            return Optional.empty();
        }
        k.setUltimaUsadaEm(LocalDateTime.now());
        repo.save(k);
        return Optional.of(k);
    }

    // ── helpers ───────────────────────────────────────────────────────────

    private String gerarPlain() {
        byte[] random = new byte[32];
        RNG.nextBytes(random);
        return KEY_PREFIX + HexFormat.of().formatHex(random);
    }

    private String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível", e);
        }
    }
}
