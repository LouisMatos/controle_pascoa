package br.com.seuprojeto.pascoa.tenant.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "api_key", schema = "platform")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ApiKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, length = 50)
    private String tenantId;

    @Column(nullable = false, length = 100)
    private String nome;

    /** SHA-256 hex (64 chars) da chave plain-text. O plain só é exibido na criação. */
    @Column(name = "chave_hash", nullable = false, length = 64, unique = true)
    private String chaveHash;

    /** Primeiros 12 chars do plain-text — para identificação visual ("ff_live_a1b2…"). */
    @Column(name = "chave_prefix", nullable = false, length = 12)
    private String chavePrefix;

    /** CSV de escopos: read, write, admin. */
    @Column(length = 500)
    private String escopos;

    @Column(nullable = false)
    private boolean ativa = true;

    @Column(name = "criada_em", nullable = false)
    private LocalDateTime criadaEm;

    @Column(name = "ultima_usada_em")
    private LocalDateTime ultimaUsadaEm;

    @Column(name = "expira_em")
    private LocalDate expiraEm;

    @PrePersist void onCreate() { if (criadaEm == null) criadaEm = LocalDateTime.now(); }
}
