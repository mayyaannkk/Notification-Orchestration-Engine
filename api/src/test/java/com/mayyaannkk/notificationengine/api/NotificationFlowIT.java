package com.mayyaannkk.notificationengine.api;

import com.mayyaannkk.notificationengine.api.dto.LoginRequest;
import com.mayyaannkk.notificationengine.api.dto.LoginResponse;
import com.mayyaannkk.notificationengine.core.dto.NotificationRequest;
import com.mayyaannkk.notificationengine.core.dto.NotificationResponse;
import com.mayyaannkk.notificationengine.core.port.EmailSender;
import com.mayyaannkk.notificationengine.core.port.NotificationDispatcher;
import com.mayyaannkk.notificationengine.persistence.entity.Channel;
import com.mayyaannkk.notificationengine.persistence.entity.Notification;
import com.mayyaannkk.notificationengine.persistence.entity.NotificationStatus;
import com.mayyaannkk.notificationengine.persistence.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
public class NotificationFlowIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16");

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private NotificationRepository repository;

    @MockitoBean
    private NotificationDispatcher notificationDispatcher;

    @MockitoBean
    private EmailSender emailSender;

    // helper methods
    private String obtainToken() {
        LoginRequest request = LoginRequest.builder()
                .username("testuser")
                .password("testpass123")
                .build();

        ResponseEntity<LoginResponse> response = restTemplate.postForEntity("/api/v1/auth/login", request, LoginResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getBody().getToken();
    }

    private HttpHeaders bearerHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    @Test
    void fullFlow_loginThenSendNotification_saveAsSent() {
        when(notificationDispatcher.dispatchNotification(any())).thenReturn(true);

        String token = obtainToken();

        NotificationRequest request = NotificationRequest.builder()
                .channel(Channel.EMAIL)
                .recipient("test@example.com")
                .subject("Integration testing notification engine")
                .body("Integration Test Body")
                .build();

        ResponseEntity<NotificationResponse> response = restTemplate.exchange(
                "/api/v1/notifications",
                HttpMethod.POST,
                new HttpEntity<>(request, bearerHeaders(token)),
                NotificationResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(response.getBody().getStatus()).isEqualTo(NotificationStatus.QUEUED);

        Optional<Notification> savedNotification = repository.findById(response.getBody().getId());
        assertThat(savedNotification).isPresent();
        assertThat(savedNotification.get().getStatus()).isEqualTo(NotificationStatus.QUEUED);
        assertThat(savedNotification.get().getRecipient()).isEqualTo("test@example.com");
    }

    @Test
    void fullFlow_unauthenticatedUser_returnForbidden() {
        NotificationRequest request = NotificationRequest.builder()
                .channel(Channel.EMAIL)
                .recipient("test@example.com")
                .subject("Integration testing notification engine")
                .body("Integration Test Body")
                .build();

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/notifications",
                HttpMethod.POST,
                new HttpEntity<>(request),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void fullFlow_validationRejected_returnError() {
        String token = obtainToken();

        NotificationRequest request = NotificationRequest.builder()
                .channel(Channel.EMAIL)
                .subject("Validation Failed")
                .build();

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/notifications",
                HttpMethod.POST,
                new HttpEntity<>(request, bearerHeaders(token)),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

}
