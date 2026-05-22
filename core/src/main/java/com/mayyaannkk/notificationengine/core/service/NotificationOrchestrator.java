package com.mayyaannkk.notificationengine.core.service;

import com.mayyaannkk.notificationengine.core.dto.NotificationRequest;
import com.mayyaannkk.notificationengine.core.dto.NotificationResponse;
import com.mayyaannkk.notificationengine.core.port.EmailSender;
import com.mayyaannkk.notificationengine.persistence.entity.Notification;
import com.mayyaannkk.notificationengine.persistence.entity.NotificationStatus;
import com.mayyaannkk.notificationengine.persistence.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationOrchestrator {

    private final NotificationRepository repository;
    private final EmailSender emailSender;

    @Transactional
    public NotificationResponse process(NotificationRequest request, String tenantId) {
        log.info("Request received: {}", request);

        if(request.getIdempotencyKey() != null) {
            Optional<Notification> byIdempotencyKey = repository.findByIdempotencyKey(request.getIdempotencyKey());

            if(byIdempotencyKey.isPresent()) {
                log.info("Duplicate detected key: {}", request.getIdempotencyKey());
                return NotificationResponse.duplicate(request.getIdempotencyKey());
            }
        }

        Notification notification = Notification.builder()
                .tenantId(tenantId)
                .idempotencyKey(request.getIdempotencyKey())
                .channel(request.getChannel())
                .status(NotificationStatus.PENDING)
                .recipient(request.getRecipient())
                .subject(request.getSubject())
                .body(request.getBody())
                .scheduledAt(request.getScheduledAt())
                .build();

        repository.save(notification);

        boolean sent = emailSender.send(notification);
        if(sent) {
            log.info("Notification sent: {}", notification);
            notification.setStatus(NotificationStatus.SENT);
        } else {
            notification.setStatus(NotificationStatus.FAILED);
        }

        Notification notificationResponse = repository.save(notification);
        log.info("Notification save in db!");

        return sent
                ? NotificationResponse.sent(notificationResponse.getId(), notificationResponse.getCreatedAt())
                : NotificationResponse.failed(notificationResponse.getId());
    }
}
