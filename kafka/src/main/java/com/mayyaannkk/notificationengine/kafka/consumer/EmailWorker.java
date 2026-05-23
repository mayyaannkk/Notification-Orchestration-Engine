package com.mayyaannkk.notificationengine.kafka.consumer;

import com.mayyaannkk.notificationengine.core.port.EmailSender;
import com.mayyaannkk.notificationengine.persistence.entity.Notification;
import com.mayyaannkk.notificationengine.persistence.entity.NotificationStatus;
import com.mayyaannkk.notificationengine.persistence.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@Slf4j
@RequiredArgsConstructor
public class EmailWorker {

    private final EmailSender emailSender;

    private final NotificationRepository notificationRepository;

    @KafkaListener(
            topics = "notifications.email",
            groupId = "email-worker-group"
    )
    public void consume(Notification notification, Acknowledgment ack) {
        log.info("Received notification: {}", notification.getId());
        try {
            boolean sent = emailSender.send(notification);
            if (sent) {
                notification.setStatus(NotificationStatus.SENT);
                notification.setSentAt(Instant.now());
                notificationRepository.save(notification);
                ack.acknowledge();
            } else {
                log.warn("Notification {} delivery failed, will redeliver", notification.getId());
            }
        } catch (Exception e) {
            log.error("Exception while delivering notification {} via EMAIL: {}",
                    notification.getId(), e.getMessage());
        }
    }
}
