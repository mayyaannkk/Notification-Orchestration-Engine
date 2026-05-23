package com.mayyaannkk.notificationengine.core.port;

import com.mayyaannkk.notificationengine.persistence.entity.Notification;

public interface NotificationDispatcher {
    boolean dispatchNotification(Notification notification);
}
