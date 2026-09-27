package br.com.seuprojeto.pascoa.auditoria.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_log")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String usuario;

    /** Código da ação — ex.: CONFIRMAR_PEDIDO, SALVAR_USUARIO. */
    @Column(nullable = false, length = 100)
    private String acao;

    /** Nome simples da entidade afetada — ex.: Pedido, Usuario. */
    @Column(name = "entidade_tipo", length = 100)
    private String entidadeTipo;

    /** ID da entidade afetada (quando disponível). */
    @Column(name = "entidade_id")
    private Long entidadeId;

    /** Detalhes legíveis sobre a ação. */
    @Column(length = 1000)
    private String detalhes;

    @Column(name = "criado_em", nullable = false)
    @Builder.Default
    private LocalDateTime criadoEm = LocalDateTime.now();
}
