package com.mayyaannkk.notificationengine.kafka.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import com.mayyaannkk.notificationengine.persistence.entity.Notification;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    /*
     * ConsumerFactory creates Kafka consumers.
     * We configure:
     * - bootstrap servers: where Kafka is running
     * - key deserializer: String (notification ID)
     * - value deserializer: JSON → Notification object
     * - trusted packages: tells JsonDeserializer which packages
     *   are safe to deserialize — prevents arbitrary class deserialization
     */
    @Bean
    public ConsumerFactory<String, Notification> consumerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                JsonDeserializer.class);
        config.put(JsonDeserializer.TRUSTED_PACKAGES,
                "com.mayyaannkk.notificationengine.*");
        config.put(JsonDeserializer.VALUE_DEFAULT_TYPE,
                Notification.class.getName());
        return new DefaultKafkaConsumerFactory<>(config);
    }

    /*
     * This factory creates the listener containers for @KafkaListener.
     * The key setting here is AckMode.MANUAL —
     * this tells Spring Kafka: do NOT auto-commit offsets.
     * Only commit when ack.acknowledge() is explicitly called.
     * Without this, offsets are committed automatically after
     * the method returns, even if processing failed.
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Notification>
    kafkaListenerContainerFactory() {

        ConcurrentKafkaListenerContainerFactory<String, Notification> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.getContainerProperties()
                .setAckMode(ContainerProperties.AckMode.MANUAL);
        return factory;
    }
}