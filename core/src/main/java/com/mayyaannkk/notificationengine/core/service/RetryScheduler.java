package com.mayyaannkk.notificationengine.core.service;

import com.mayyaannkk.notificationengine.core.port.EmailSender;
import com.mayyaannkk.notificationengine.persistence.entity.Notification;
import com.mayyaannkk.notificationengine.persistence.entity.NotificationStatus;
import com.mayyaannkk.notificationengine.persistence.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class RetryScheduler {

    private final NotificationRepository notificationRepository;

    private final EmailSender emailSender;

    @Value("${app.retry.max-attempts:3}")
    private int maxAttempts;

    @Scheduled(fixedDelay = 60000)
    public void retryFailedNotifications() {
        Instant cutoff = Instant.now().minus(1, ChronoUnit.MINUTES);
        List<Notification> failed = notificationRepository.findByStatusAndCreatedAtBefore(NotificationStatus.FAILED, cutoff);

        for(Notification notification : failed) {
            if(notification.getRetryCount() >= maxAttempts) {
                notification.setStatus(NotificationStatus.DEAD);
                log.warn("Notification {} marked dead after {} attempts", notification.getId(), notification.getRetryCount());
                notificationRepository.save(notification);
                continue;
            }
            boolean isSent = emailSender.send(notification);
            if(isSent) {
                notification.setStatus(NotificationStatus.SENT);
                log.info("Notification sent successfully: {}", notification.getId());
            } else {
                notification.setRetryCount(notification.getRetryCount() + 1);
                log.warn("Notification: {} delivery failed", notification.getId());
            }
            notificationRepository.save(notification);
        }
    }
}
