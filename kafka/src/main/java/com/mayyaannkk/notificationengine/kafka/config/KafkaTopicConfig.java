package com.mayyaannkk.notificationengine.kafka.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic email() {
        return TopicBuilder.name("notifications.email")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic dlq() {
        return TopicBuilder.name("notifications.dlq")
                .partitions(1)
                .replicas(1)
                .build();
    }
}
