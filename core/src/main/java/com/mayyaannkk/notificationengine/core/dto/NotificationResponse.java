package com.mayyaannkk.notificationengine.core.dto;

import com.mayyaannkk.notificationengine.persistence.entity.NotificationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {

    private String id;
    private NotificationStatus status;
    private String message;
    private Instant createdAt;

    public static NotificationResponse sent(String id, Instant createdAt) {
        return NotificationResponse.builder()
                .id(id)
                .status(NotificationStatus.SENT)
                .message("Notification sent successfully")
                .createdAt(createdAt)
                .build();
    }

    public static NotificationResponse failed(String id) {
        return NotificationResponse.builder()
                .id(id)
                .status(NotificationStatus.FAILED)
                .message("Delivery failed — will retry automatically")
                .build();
    }

    public static NotificationResponse duplicate(String idempotencyKey) {
        return NotificationResponse.builder()
                .status(NotificationStatus.SKIPPED)
                .message("Already processed for key: " + idempotencyKey)
                .build();
    }

    public static NotificationResponse queued(String id, Instant createdAt) {
        return NotificationResponse.builder()
                .id(id)
                .status(NotificationStatus.QUEUED)
                .message("Notification accepted and queued for delivery")
                .createdAt(createdAt)
                .build();
    }
}