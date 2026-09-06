package com.study.flashsale.task;

import com.study.flashsale.infrastructure.redis.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduledTaskLockExecutor {

    private final RedisService redisService;

    public void executeWithLock(String lockKey, Duration ttl, Runnable task) {
        String lockToken;
        try {
            lockToken = redisService.tryLock(lockKey, ttl);
        } catch (Exception e) {
            log.warn("Scheduled task skipped because lock service is unavailable, lockKey={}", lockKey, e);
            return;
        }

        if (lockToken == null) {
            log.debug("Scheduled task skipped because lock is held by another instance, lockKey={}", lockKey);
            return;
        }

        try {
            task.run();
        } finally {
            try {
                redisService.unlock(lockKey, lockToken);
            } catch (Exception e) {
                log.error("Scheduled task lock release failed, lockKey={}", lockKey, e);
            }
        }
    }
}
