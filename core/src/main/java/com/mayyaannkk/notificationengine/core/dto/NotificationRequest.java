package com.mayyaannkk.notificationengine.core.dto;

import com.mayyaannkk.notificationengine.persistence.entity.Channel;
import jakarta.validation.constraints.*;
import lombok.*;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationRequest {

    @NotNull(message = "Channel is required")
    private Channel channel;

    @NotBlank(message = "Recipient cannot be empty")
    @Size(max = 512, message = "Recipient cannot exceed 512 characters")
    private String recipient;

    @Size(max = 998, message = "Subject should not exceed 998 characters")
    private String subject;

    @NotBlank(message = "Body cannot be empty")
    private String body;

    private String idempotencyKey;

    private Instant scheduledAt;
}