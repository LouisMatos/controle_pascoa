package br.com.seuprojeto.pascoa.auditoria.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marca métodos de service que devem gerar um registro no Audit Log.
 *
 * O aspecto {@code AuditAspect} intercepta esses métodos e persiste
 * um {@code AuditLog} com o usuário autenticado, a ação e o ID da entidade.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Auditavel {

    /** Código legível da ação, ex.: "CONFIRMAR_PEDIDO". */
    String acao();

    /** Tipo da entidade afetada, ex.: "Pedido". Opcional. */
    String entidade() default "";
}
