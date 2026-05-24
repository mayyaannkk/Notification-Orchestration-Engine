package com.mayyaannkk.notificationengine.core.service;

import com.mayyaannkk.notificationengine.core.dto.NotificationRequest;
import com.mayyaannkk.notificationengine.core.dto.NotificationResponse;
import com.mayyaannkk.notificationengine.core.exception.RateLimitExceededException;
import com.mayyaannkk.notificationengine.core.port.EmailSender;
import com.mayyaannkk.notificationengine.core.port.NotificationDispatcher;
import com.mayyaannkk.notificationengine.core.port.RateLimiter;
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
    private final NotificationDispatcher notificationDispatcher;
    private final RateLimiter rateLimiter;

    @Transactional
    public NotificationResponse process(NotificationRequest request, String tenantId) {
        log.info("Request received for tenantId={} channel={}", tenantId, request.getChannel());

        if(!rateLimiter.tryAcquire(tenantId, request.getChannel())) {
            throw new RateLimitExceededException(tenantId);
        }

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

        boolean dispatched = notificationDispatcher.dispatchNotification(notification);
        if(dispatched) {
            log.info("Notification queued id={}", notification.getId());
            notification.setStatus(NotificationStatus.QUEUED);
        } else {
            notification.setStatus(NotificationStatus.FAILED);
        }

        repository.save(notification);
        log.info("Notification save in db!");

        return dispatched
                ? NotificationResponse.queued(notification.getId(), notification.getCreatedAt())
                : NotificationResponse.failed(notification.getId());
    }
}
