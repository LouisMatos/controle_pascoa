package br.com.seuprojeto.pascoa.config;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Configuração global do sistema — singleton (sempre id = 1).
 * Gerenciada exclusivamente por administradores via /admin/sistema.
 */
@Entity
@Table(name = "configuracao_sistema")
@Data
@NoArgsConstructor
public class ConfiguracaoSistema {

    @Id
    private Long id = 1L;

    /** Quando true, o MaintenanceFilter bloqueia todos os usuários não-ADMIN. */
    @Column(name = "modo_manutencao", nullable = false)
    private Boolean modoManutencao = false;

    /** Mensagem exibida na página de manutenção. */
    @Size(max = 500)
    @Column(name = "mensagem_manutencao", length = 500)
    private String mensagemManutencao = "O sistema está temporariamente indisponível para manutenção.";

    /** Texto livre com previsão de retorno (ex.: "16h00", "amanhã às 9h"). */
    @Size(max = 100)
    @Column(name = "previsao_retorno", length = 100)
    private String previsaoRetorno;

    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm = LocalDateTime.now();

    @PrePersist
    @PreUpdate
    private void preUpdate() {
        this.atualizadoEm = LocalDateTime.now();
    }
}
