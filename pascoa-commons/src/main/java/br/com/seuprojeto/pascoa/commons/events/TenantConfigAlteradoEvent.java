package br.com.seuprojeto.pascoa.commons.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * MS-01 — publicado pelo tenant-service / config-engine-service quando uma
 * configuração de tenant é alterada (white-label, feature flag, etc.).
 *
 * Consumidores: qualquer microsserviço que mantenha cache local de
 * configuração do tenant (notification-service templates, catalog-service
 * atributos, etc.). Ao receber, devem fazer {@code cacheManager.getCache(...)
 * .evict(tenantId)} para que a próxima leitura busque o valor atualizado.
 *
 * Exchange: {@value EXCHANGE}.
 * Routing key: {@value ROUTING_KEY}.
 * Queue sugerida no consumidor: {@code config.tenant.alterado.queue}
 * (durable, bind ao exchange acima).
 */
@Getter
public class TenantConfigAlteradoEvent extends DomainEvent {

    public static final String EXCHANGE    = "foodflow.tenant.config";
    public static final String ROUTING_KEY = "tenant.config.alterado";

    /** Tipo de alteração — usado pelo consumidor para invalidar caches específicos. */
    public enum Tipo { WHITE_LABEL, FEATURE_FLAG, ATRIBUTO, PLANO, OUTROS }

    private String tenantId;
    private Tipo   tipo;

    @JsonCreator
    public TenantConfigAlteradoEvent(@JsonProperty("tenantId") String tenantId,
                                     @JsonProperty("tipo")     Tipo   tipo) {
        super(tenantId);
        this.tenantId = tenantId;
        this.tipo     = tipo;
    }
}
