package com.logistica.demo.platform.infrastructure.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.logistica.demo.sharedkernel.event.DomainEvent;
import com.logistica.demo.sharedkernel.event.DomainEventPublisher;
import com.logistica.demo.sharedkernel.event.OutboxPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class JdbcOutboxAdapter implements OutboxPort, DomainEventPublisher {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final JdbcJsonSupport jsonSupport;

    public JdbcOutboxAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = JsonMapper.builder().findAndAddModules().build();
        this.jsonSupport = new JdbcJsonSupport(jdbcTemplate);
    }

    @Override
    @Transactional
    public void append(DomainEvent event) {
        jdbcTemplate.update(
                """
                INSERT INTO platform.outbox_events (
                    id, aggregate_type, aggregate_id, event_type, payload, occurred_at
                ) VALUES (?, ?, ?, ?, %s, ?)
                """.formatted(jsonSupport.jsonPlaceholder("JSONB")),
                event.eventId(),
                event.aggregateType(),
                event.aggregateId(),
                event.eventType(),
                toJson(event),
                event.occurredAt());
    }

    @Override
    @Transactional
    public void publish(DomainEvent event) {
        append(event);
    }

    private String toJson(DomainEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("No se pudo serializar el evento de dominio", ex);
        }
    }
}
