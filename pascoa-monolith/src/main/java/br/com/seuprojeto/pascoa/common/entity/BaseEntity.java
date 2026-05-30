package br.com.seuprojeto.pascoa.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Superclasse de auditoria — herdada por todas as entidades principais.
 *
 * Campos preenchidos automaticamente pelo Spring Data Auditing:
 *   criadoEm    — data/hora de criação  (mapeável via @AttributeOverride)
 *   atualizadoEm — data/hora da última atualização
 *   criadoPor   — usuário que criou o registro
 *   atualizadoPor — usuário que fez a última alteração
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public abstract class BaseEntity {

    @CreatedDate
    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @LastModifiedDate
    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;

    @CreatedBy
    @Column(name = "criado_por", updatable = false, length = 100)
    private String criadoPor;

    @LastModifiedBy
    @Column(name = "atualizado_por", length = 100)
    private String atualizadoPor;
}
