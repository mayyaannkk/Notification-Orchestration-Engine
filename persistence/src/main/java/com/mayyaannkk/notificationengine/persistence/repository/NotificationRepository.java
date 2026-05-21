package com.mayyaannkk.notificationengine.persistence.repository;


import com.mayyaannkk.notificationengine.persistence.entity.Notification;
import com.mayyaannkk.notificationengine.persistence.entity.NotificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;


@Repository
public interface NotificationRepository
        extends JpaRepository<Notification, String> {

    // Used for deduplication check — does this key already exist?
    Optional<Notification> findByIdempotencyKey(String idempotencyKey);

    // Used for dashboards and status queries
    List<Notification> findByTenantIdAndStatus(
            String tenantId, NotificationStatus status
    );

    // Used by the retry job — find stuck PENDING notifications
    List<Notification> findByStatusAndCreatedAtBefore(
            NotificationStatus status, Instant cutoff
    );


    @Query("""
        SELECT n FROM Notification n
        WHERE n.tenantId = :tenantId
        AND n.createdAt >= :from
        ORDER BY n.createdAt DESC
        """)
    List<Notification> findRecentByTenant(
            @Param("tenantId") String tenantId,
            @Param("from") Instant from
    );
}
