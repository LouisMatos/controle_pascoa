package br.com.seuprojeto.pascoa.commons.events;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Getter
@NoArgsConstructor
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "eventType")
public abstract class DomainEvent {

    private String eventId = UUID.randomUUID().toString();
    private Instant occurredOn = Instant.now();
    private String aggregateId;

    protected DomainEvent(String aggregateId) {
        this.aggregateId = aggregateId;
    }
}
