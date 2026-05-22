package com.mayyaannkk.notificationengine.api.controller;

import com.mayyaannkk.notificationengine.api.dto.LoginRequest;
import com.mayyaannkk.notificationengine.api.dto.LoginResponse;
import com.mayyaannkk.notificationengine.auth.service.JwtService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {

    @Value("${app.user.username}")
    private String configuredUsername;

    @Value("${app.user.password}")
    private String configuredPassword;

    @Value("${app.user.tenant-id}")
    private String configuredTenantId;

    @Value("${app.jwt.expiration-ms}")
    private long expirationMs;

    private final JwtService jwtService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        if(request.getUsername().equals(configuredUsername) && request.getPassword().equals(configuredPassword)) {
            String token = jwtService.generateToken(configuredUsername, configuredTenantId);
            return ResponseEntity.ok(LoginResponse.of(token, expirationMs / 1000));
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
}
