package com.mayyaannkk.notificationengine.core.service;

import com.mayyaannkk.notificationengine.core.dto.NotificationRequest;
import com.mayyaannkk.notificationengine.core.dto.NotificationResponse;
import com.mayyaannkk.notificationengine.core.port.EmailSender;
import com.mayyaannkk.notificationengine.core.port.NotificationDispatcher;
import com.mayyaannkk.notificationengine.persistence.entity.Channel;
import com.mayyaannkk.notificationengine.persistence.entity.Notification;
import com.mayyaannkk.notificationengine.persistence.entity.NotificationStatus;
import com.mayyaannkk.notificationengine.persistence.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationOrchestratorTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationDispatcher notificationDispatcher;

    @InjectMocks
    private NotificationOrchestrator notificationOrchestrator;

    private NotificationRequest request;
    private static final String TENANT = "tenant-001";

    @BeforeEach
    void setUp() {
        request = NotificationRequest.builder()
                .channel(Channel.EMAIL)
                .recipient("user@example.com")
                .subject("Hello")
                .body("Test body")
                .build();

        lenient().when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification n = invocation.getArgument(0);
            if(n.getId() == null) n.setId("test-uuid-001");
            if(n.getCreatedAt() == null) n.setCreatedAt(Instant.now());
            return n;
        });
    }

    @Test
    void process_emailSentSuccessfully_returnSentResponse() {
        when(notificationDispatcher.dispatchNotification(any(Notification.class))).thenReturn(true);

        NotificationResponse response = notificationOrchestrator.process(request, TENANT);

        assertThat(response.getStatus()).isEqualTo(NotificationStatus.QUEUED);
        assertThat(response.getId()).isNotNull();
    }

    @Test
    void process_emailFailed_returnFailedResponse() {
        when(notificationDispatcher.dispatchNotification(any(Notification.class))).thenReturn(false);

        NotificationResponse response = notificationOrchestrator.process(request, TENANT);

        assertThat(response.getStatus()).isEqualTo(NotificationStatus.FAILED);
        assertThat(response.getMessage()).contains("retry");
    }

    @Test
    void process_idempotencyKeyMatch_returnSkippedResponse() {
        request.setIdempotencyKey("order-123-confirm");

        when(notificationRepository.findByIdempotencyKey(anyString())).thenReturn(Optional.of(Notification.builder().build()));

        NotificationResponse response = notificationOrchestrator.process(request, TENANT);

        assertThat(response.getStatus()).isEqualTo(NotificationStatus.SKIPPED);
        verify(notificationRepository, never()).save(any(Notification.class));
        verify(notificationDispatcher, never()).dispatchNotification(any(Notification.class));
    }

    @Test
    void process_noIdempotencyKeyFound_returnSentResponse() {
        when(notificationDispatcher.dispatchNotification(any(Notification.class))).thenReturn(true);

        NotificationResponse response = notificationOrchestrator.process(request, TENANT);

        assertThat(response.getStatus()).isEqualTo(NotificationStatus.QUEUED);
        verify(notificationRepository, never()).findByIdempotencyKey(any(String.class));
    }

    @Test
    void process_saveBeforeEmail_returnSentResponse() {
        when(notificationDispatcher.dispatchNotification(any(Notification.class))).thenReturn(true);

        NotificationResponse response = notificationOrchestrator.process(request, TENANT);

        // create an InOrder verifier for the objects you care about
        InOrder order = inOrder(notificationRepository, notificationDispatcher);

        // then verify they were called in this exact sequence
        order.verify(notificationRepository).save(any());   // save first
        order.verify(notificationDispatcher).dispatchNotification(any());              // then send
        assertThat(response.getStatus()).isEqualTo(NotificationStatus.QUEUED);
    }

}