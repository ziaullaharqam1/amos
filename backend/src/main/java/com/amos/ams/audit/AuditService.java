package com.amos.ams.audit;

import com.amos.ams.domain.AuditLog;
import com.amos.ams.domain.User;
import com.amos.ams.kafka.AuditEvent;
import com.amos.ams.kafka.EventPublisher;
import com.amos.ams.repository.AuditLogRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

@Service
public class AuditService {
    private final AuditLogRepository repository;
    private final EventPublisher publisher;
    private final ObjectMapper mapper;

    public AuditService(AuditLogRepository repository, EventPublisher publisher, ObjectMapper mapper) {
        this.repository = repository;
        this.publisher = publisher;
        this.mapper = mapper;
    }

    public void record(User actor, String action, String entityType, Object entityId, Object before, Object after) {
        AuditLog log = new AuditLog();
        if (actor != null) {
            log.setActorId(actor.getId());
            log.setActorUsername(actor.getUsername());
        }
        log.setAction(action);
        log.setEntityType(entityType);
        log.setEntityId(entityId == null ? null : String.valueOf(entityId));
        log.setBeforeValue(json(before));
        log.setAfterValue(json(after));
        repository.save(log);

        AuditEvent event = new AuditEvent();
        event.setActorId(log.getActorId());
        event.setActorUsername(log.getActorUsername());
        event.setAction(action);
        event.setEntityType(entityType);
        event.setEntityId(log.getEntityId());
        event.setBeforeValue(log.getBeforeValue());
        event.setAfterValue(log.getAfterValue());
        publisher.publishAudit(event);
    }

    private String json(Object value) {
        if (value == null) return null;
        if (value instanceof String s) return s;
        try {
            return mapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return String.valueOf(value);
        }
    }
}
