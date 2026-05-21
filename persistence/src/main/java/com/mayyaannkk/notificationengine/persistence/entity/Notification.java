package com.mayyaannkk.notificationengine.persistence.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.Instant;


@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private String id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "idempotency_key", unique = true)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 20)
    private Channel channel;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private NotificationStatus status = NotificationStatus.PENDING;



    @Column(name = "recipient", nullable = false)
    private String recipient;

    @Column(name = "subject")
    private String subject; // Only used for EMAIL. Nullable.


    @Column(name = "body", nullable = false, columnDefinition = "TEXT")
    private String body;

    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private Integer retryCount = 0;


    @Column(name = "scheduled_at")
    private Instant scheduledAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason; // Why it failed, if it did.

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;


    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        if (this.status == null) {
            this.status = NotificationStatus.PENDING;
        }
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }
}