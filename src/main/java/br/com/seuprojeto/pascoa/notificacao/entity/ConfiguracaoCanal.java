package br.com.seuprojeto.pascoa.notificacao.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "configuracao_canal")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfiguracaoCanal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, unique = true, length = 10)
    private CanalNotificacao tipo;

    /**
     * URL base da API (Evolution API para WhatsApp, ou SMTP host para e-mail).
     */
    @Column(name = "api_url", length = 300)
    private String apiUrl;

    /**
     * Chave de autenticação da API / instância Evolution API.
     */
    @Column(name = "api_key", length = 300)
    private String apiKey;

    /**
     * Nome da instância Evolution API (WhatsApp) ou endereço remetente (e-mail).
     */
    @Column(name = "remetente", length = 200)
    private String remetente;

    @Column(name = "ativo", nullable = false)
    @Builder.Default
    private Boolean ativo = false;

    /**
     * Quando true, não envia de verdade — registra o disparo apenas no log.
     */
    @Column(name = "test_mode", nullable = false)
    @Builder.Default
    private Boolean testMode = true;
}
