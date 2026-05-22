package com.mayyaannkk.notificationengine.api.controller;

import com.mayyaannkk.notificationengine.auth.model.AuthenticatedUser;
import com.mayyaannkk.notificationengine.core.dto.NotificationRequest;
import com.mayyaannkk.notificationengine.core.dto.NotificationResponse;
import com.mayyaannkk.notificationengine.core.service.NotificationOrchestrator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
@Slf4j
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationOrchestrator orchestrator;

    @PostMapping
    public ResponseEntity<NotificationResponse> send(
            @Valid
            @RequestBody NotificationRequest request, Authentication authentication
    ) {
        AuthenticatedUser user = (AuthenticatedUser) authentication.getPrincipal();

        String userId = user.userId();
        String tenantId = user.tenantId();

        log.info("Processing notification for user: {}, with tenantId: {}", userId, tenantId);
        NotificationResponse processed = orchestrator.process(request, tenantId);

        return ResponseEntity.accepted().body(processed);
    }
}
