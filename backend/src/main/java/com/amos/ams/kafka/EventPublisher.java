package com.amos.ams.kafka;

import com.amos.ams.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class EventPublisher {
    private static final Logger log = LoggerFactory.getLogger(EventPublisher.class);
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final AppProperties properties;

    public EventPublisher(ObjectProvider<KafkaTemplate<String, Object>> kafkaTemplate, AppProperties properties) {
        this.kafkaTemplate = kafkaTemplate.getIfAvailable();
        this.properties = properties;
    }

    public void publishNotification(NotificationEvent event) {
        send(properties.getKafka().getTopics().getNotifications(), event.getEntityId(), event);
    }

    public void publishAudit(AuditEvent event) {
        send(properties.getKafka().getTopics().getAudit(), event.getEntityId(), event);
    }

    private void send(String topic, String key, Object payload) {
        if (kafkaTemplate == null || topic == null) {
            log.debug("Kafka unavailable; dropping event on {}", topic);
            return;
        }
        kafkaTemplate.send(topic, key, payload).whenComplete((r, ex) -> {
            if (ex != null) {
                log.warn("Failed to publish to {}: {}", topic, ex.getMessage());
            }
        });
    }
}
