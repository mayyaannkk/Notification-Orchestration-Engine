package com.mayyaannkk.notificationengine.channels.email;

import com.mayyaannkk.notificationengine.core.port.EmailSender;
import com.mayyaannkk.notificationengine.persistence.entity.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class GmailEmailSender implements EmailSender {

    private final JavaMailSender javaMailSender;

    @Value("${app.mail.from}")
    private String fromAddress;

    @Override
    public boolean send(Notification notification) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(notification.getRecipient());
            message.setSubject(notification.getSubject());
            message.setText(notification.getBody());

            javaMailSender.send(message);
            log.info("Email sent successfully to {}", notification.getRecipient());
            return true;
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}",
                    notification.getRecipient(), e.getMessage());
            return false;
        }
    }
}