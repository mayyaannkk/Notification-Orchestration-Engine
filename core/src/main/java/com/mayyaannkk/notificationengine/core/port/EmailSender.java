package com.mayyaannkk.notificationengine.core.port;

import com.mayyaannkk.notificationengine.persistence.entity.Notification;

public interface EmailSender {
    boolean send(Notification notification);
}
