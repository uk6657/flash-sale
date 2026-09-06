package com.study.flashsale.infrastructure.redis;

import com.study.flashsale.common.CacheData;
import com.study.flashsale.common.ErrorCode;
import com.study.flashsale.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.UUID;
import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisService {

    public static final int SECKILL_ACCEPTED = 0;
    public static final int SECKILL_DUPLICATE = 1;
    public static final int SECKILL_STOCK_NOT_PREPARED = 2;
    public static final int SECKILL_OUT_OF_STOCK = 3;

    private static final DefaultRedisScript<Long> SECKILL_SCRIPT = new DefaultRedisScript<>("""
            if redis.call('EXISTS', KEYS[1]) == 1 then
                return 1
            end

            local stock = redis.call('GET', KEYS[2])
            if not stock then
                return 2
            end

            if tonumber(stock) <= 0 then
                return 3
            end

            redis.call('DECR', KEYS[2])
            redis.call('SET', KEYS[1], '1', 'EX', ARGV[1])
            return 0
            """, Long.class);

    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT = new DefaultRedisScript<>("""
            if redis.call('GET', KEYS[1]) == ARGV[1] then
                return redis.call('DEL', KEYS[1])
            end

            return 0
            """, Long.class);

    private final RedisTemplate<String, String> redisTemplate;

    private final ObjectMapper objectMapper;

    public String get(String key) {
        return runWithRetry("读取Redis数据", () -> redisTemplate.opsForValue().get(key));
    }

    public void set(String key, String value) {
        runWithRetry("写入Redis数据", () -> {
            redisTemplate.opsForValue().set(key, value);
            return null;
        });
    }

    public void set(String key, String value, Duration timeout) {
        runWithRetry("写入Redis数据", () -> {
            redisTemplate.opsForValue().set(key, value, timeout);
            return null;
        });
    }

    public <T> void setWithLogicalExpire(String key, T data, Duration logicalTimeout) {
        try {
            CacheData<T> cacheData = new CacheData<>(data, LocalDateTime.now().plus(logicalTimeout));
            set(key, objectMapper.writeValueAsString(cacheData));
        } catch (JacksonException e) {
            throw new BusinessException("写入逻辑过期缓存失败");
        }
    }

    public Boolean setIfAbsent(String key, String value, Duration timeout) {
        return runWithRetry("写入Redis防重复数据", () ->
                redisTemplate.opsForValue().setIfAbsent(key, value, timeout)
        );
    }

    public Boolean hasKey(String key) {
        return runWithRetry("判断Redis Key是否存在", () -> redisTemplate.hasKey(key));
    }

    public Long getExpire(String key, TimeUnit timeUnit) {
        return runWithRetry("读取Redis Key过期时间", () -> redisTemplate.getExpire(key, timeUnit));
    }

    public Long increment(String key) {
        return runWithRetry("递增Redis数据", () -> redisTemplate.opsForValue().increment(key));
    }

    public Long decrement(String key) {
        return runWithRetry("递减Redis数据", () -> redisTemplate.opsForValue().decrement(key));
    }

    public Boolean expire(String key, Duration timeout) {
        return runWithRetry("设置Redis过期时间", () -> redisTemplate.expire(key, timeout));
    }

    public Boolean delete(String key) {
        return runWithRetry("删除Redis数据", () -> redisTemplate.delete(key));
    }

    public Long delete(Collection<String> keys) {
        return runWithRetry("批量删除Redis数据", () -> redisTemplate.delete(keys));
    }

    public Set<String> keys(String pattern) {
        return runWithRetry("扫描Redis Key", () -> redisTemplate.keys(pattern));
    }

    public Boolean zAdd(String key, String value, double score) {
        return runWithRetry("写入Redis ZSet", () -> redisTemplate.opsForZSet().add(key, value, score));
    }

    public Long zRemove(String key, String value) {
        return runWithRetry("删除Redis ZSet数据", () -> redisTemplate.opsForZSet().remove(key, value));
    }

    public Set<String> zRangeByScore(String key, double min, double max, long offset, long count) {
        return runWithRetry("读取Redis ZSet范围数据",
                () -> redisTemplate.opsForZSet().rangeByScore(key, min, max, offset, count));
    }

    public int tryAcquireSeckill(String userKey, String stockKey, Duration userKeyTtl) {
        long ttlSeconds = Math.max(1, userKeyTtl.getSeconds());
        Long result = runWithRetry("执行Redis秒杀Lua脚本",
                () -> redisTemplate.execute(SECKILL_SCRIPT, List.of(userKey, stockKey), String.valueOf(ttlSeconds)));
        return result == null ? SECKILL_STOCK_NOT_PREPARED : result.intValue();
    }

    private <T> T runWithRetry(String operationName, Supplier<T> action) {
        int maxRetries = 3;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                return action.get();
            } catch (Exception e) {
                if (attempt == maxRetries) {
                    log.error("{}失败", operationName, e);
                    throw new BusinessException(ErrorCode.REDIS_UNAVAILABLE, operationName + "失败");
                }

                try {
                    Thread.sleep(attempt * 100L);
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                    log.error("{}重试被中断", operationName, interruptedException);
                    throw new BusinessException(ErrorCode.REDIS_UNAVAILABLE, operationName + "被中断");
                }
            }
        }

        throw new BusinessException(ErrorCode.REDIS_UNAVAILABLE, operationName + "失败");
    }

    public String tryLock(String key, Duration ttl) {
        String token = UUID.randomUUID().toString();

        Boolean locked = runWithRetry("获取Redis锁", () ->
                redisTemplate.opsForValue().setIfAbsent(key, token, ttl)
        );

        return Boolean.TRUE.equals(locked) ? token : null;
    }

    public void unlock(String key, String token) {
        if (token == null) {
            return;
        }

        runWithRetry("释放Redis锁", () ->
                redisTemplate.execute(UNLOCK_SCRIPT, List.of(key), token)
        );
    }
}

