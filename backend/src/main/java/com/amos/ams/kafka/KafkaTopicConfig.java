package com.amos.ams.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;

@Configuration
@EnableKafka
@ConditionalOnBean(KafkaAdmin.class)
public class KafkaTopicConfig {
    @Bean
    public NewTopic notificationsTopic() {
        return TopicBuilder.name("ams.notifications").partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic auditTopic() {
        return TopicBuilder.name("ams.audit").partitions(3).replicas(1).build();
    }
}
