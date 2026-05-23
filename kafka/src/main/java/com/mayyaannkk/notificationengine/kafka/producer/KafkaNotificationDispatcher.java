package com.mayyaannkk.notificationengine.kafka.producer;

import com.mayyaannkk.notificationengine.core.port.NotificationDispatcher;
import com.mayyaannkk.notificationengine.persistence.entity.Channel;
import com.mayyaannkk.notificationengine.persistence.entity.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaNotificationDispatcher implements NotificationDispatcher {

    private final KafkaTemplate<String, Notification> kafkaTemplate;

    @Override
    public boolean dispatchNotification(Notification notification) {
        try {
            switch (notification.getChannel()) {
                case EMAIL -> {
                    kafkaTemplate.send("notifications.email", notification.getId(), notification);
                    log.info("Notification {} dispatched to notifications.email", notification.getId());
                    return true;
                }
                default -> {
                    log.warn("{} not implemented yet", notification.getChannel());
                    return false;
                }
            }
        } catch (Exception e) {
            log.error("Exception while dispatching notification to Kafka: {}", e.getMessage());
            return false;
        }
    }
}
