package com.mayyaannkk.notificationengine.persistence.repository;


import com.mayyaannkk.notificationengine.persistence.entity.Channel;
import com.mayyaannkk.notificationengine.persistence.entity.Notification;
import com.mayyaannkk.notificationengine.persistence.entity.NotificationStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
@Testcontainers
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class NotificationRepositoryTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16");

    @Autowired
    private NotificationRepository repository;

    @Test
    @DisplayName("should save a notification and find it by id")
    void saveAndFindById() {
        Notification notification = Notification.builder()
                .tenantId("tenant-001")
                .channel(Channel.EMAIL)
                .recipient("test@example.com")
                .subject("Welcome!")
                .body("Hello from the test")
                .build();

        Notification saved = repository.save(notification);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getStatus()).isEqualTo(NotificationStatus.PENDING);
        assertThat(saved.getCreatedAt()).isNotNull();

        var found = repository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getRecipient()).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("should find notification by idempotency key")
    void findByIdempotencyKey() {
        Notification notification = Notification.builder()
                .tenantId("tenant-001")
                .channel(Channel.SMS)
                .recipient("+919876543210")
                .body("Your OTP is 1234")
                .idempotencyKey("order-456-sms-confirm")
                .build();

        repository.save(notification);

        var found = repository.findByIdempotencyKey("order-456-sms-confirm");
        assertThat(found).isPresent();
        assertThat(found.get().getChannel()).isEqualTo(Channel.SMS);
    }

    @Test
    @DisplayName("duplicate idempotency key should throw DataIntegrityViolationException")
    void duplicateIdempotencyKeyShouldFail() {
        repository.save(Notification.builder()
                .tenantId("tenant-001")
                .channel(Channel.EMAIL)
                .recipient("a@b.com")
                .body("first")
                .idempotencyKey("unique-key-123")
                .build());

        assertThrows(
                org.springframework.dao.DataIntegrityViolationException.class,
                () -> repository.saveAndFlush(Notification.builder()
                        .tenantId("tenant-002")
                        .channel(Channel.EMAIL)
                        .recipient("c@d.com")
                        .body("second")
                        .idempotencyKey("unique-key-123")
                        .build())
        );
    }
}