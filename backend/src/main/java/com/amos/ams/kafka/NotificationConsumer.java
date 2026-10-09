package com.amos.ams.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnBean(ConcurrentKafkaListenerContainerFactory.class)
public class NotificationConsumer {
    private static final Logger log = LoggerFactory.getLogger(NotificationConsumer.class);

    @KafkaListener(topics = "${app.kafka.topics.notifications}", groupId = "ams-notification-fanout")
    public void onNotification(NotificationEvent event) {
        log.info("Kafka notification {} for users {}", event.getType(), event.getUserIds());
    }
}
