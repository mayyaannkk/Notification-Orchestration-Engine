package com.mayyaannkk.notificationengine.cache;

import com.mayyaannkk.notificationengine.core.port.RateLimiter;
import com.mayyaannkk.notificationengine.persistence.entity.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class RedisRateLimiter implements RateLimiter {

    private final StringRedisTemplate redisTemplate;

    @Value("${app.rate-limit.max-per-minute:10}")
    private int maxRequests;

    @Override
    public boolean tryAcquire(String tenantId, Channel channel) {
        String key = String.format("ratelimit:%s:%s", tenantId, channel.name());

        long now = Instant.now().getEpochSecond();
        long windowStart = now - 60;

        redisTemplate.opsForZSet()
                .removeRangeByScore(key, 0, windowStart);

        Long count = redisTemplate.opsForZSet().size(key);

        if(count != null && count >= maxRequests) {
            log.warn("Rate limit exceeded for tenant={} channel={} count={}/{}",
                    tenantId, channel.name(), count, maxRequests);
            return false;
        }

        String requestId = UUID.randomUUID().toString();

        redisTemplate.opsForZSet()
                .add(key, requestId, now);

        redisTemplate.expire(key, Duration.ofSeconds(70));

        log.debug("Rate limit check passed for tenant={} channel={} count={}/{}",
                tenantId, channel.name(), count, maxRequests);
        return true;
    }
}
