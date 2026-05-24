package com.mayyaannkk.notificationengine.core.port;

import com.mayyaannkk.notificationengine.persistence.entity.Channel;

public interface RateLimiter {
    boolean tryAcquire(String tenantId, Channel channel);
}
