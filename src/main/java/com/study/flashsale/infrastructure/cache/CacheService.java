package com.study.flashsale.infrastructure.cache;

import com.study.flashsale.common.CacheData;
import com.study.flashsale.common.ErrorCode;
import com.study.flashsale.common.RedisKeyConstants;
import com.study.flashsale.config.FlashSaleProperties;
import com.study.flashsale.exception.BusinessException;
import com.study.flashsale.infrastructure.redis.RedisService;
import com.study.flashsale.util.TraceTaskWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
public class CacheService {

    private final RedisService redisService;
    private final ObjectMapper objectMapper;
    private final FlashSaleProperties flashSaleProperties;

    private final AtomicInteger dbFallbackConcurrency = new AtomicInteger();

    public <T> T queryWithPassThroughAndMutex(String cacheKey,
                                              String lockKey,
                                              Class<T> type,
                                              Supplier<T> dbQuery,
                                              Duration cacheTtl,
                                              String emptyMessage) {
        String cachedValue;
        try {
            cachedValue = redisService.get(cacheKey);
        } catch (BusinessException e) {
            log.warn("读取Redis缓存失败，降级查询数据库，cacheKey={}", cacheKey, e);
            return queryDbWithFallbackLimit(dbQuery);
        }

        if (RedisKeyConstants.EMPTY_VALUE.equals(cachedValue)) {
            throw new BusinessException(emptyMessage);
        }

        if (cachedValue != null) {
            return parseCacheValue(cachedValue, type);
        }

        String lockToken;
        try {
            lockToken = redisService.tryLock(
                    lockKey,
                    Duration.ofSeconds(flashSaleProperties.getCache().getRebuildLockSeconds())
            );
        } catch (BusinessException e) {
            log.warn("获取缓存重建锁失败，降级查询数据库，cacheKey={}", cacheKey, e);
            return queryDbWithFallbackLimit(dbQuery);
        }

        if (lockToken == null) {
            return retryGetNormalCache(cacheKey, type, emptyMessage);
        }

        try {
            return rebuildNormalCache(cacheKey, dbQuery, cacheTtl, emptyMessage);
        } finally {
            unlockQuietly(lockKey, lockToken);
        }
    }

    public <T> T queryWithLogicalExpire(String cacheKey,
                                        String lockKey,
                                        Class<T> type,
                                        Supplier<T> dbQuery,
                                        Duration logicalTtl,
                                        String emptyMessage) {
        String cachedValue;
        try {
            cachedValue = redisService.get(cacheKey);
        } catch (BusinessException e) {
            log.warn("读取逻辑过期缓存失败，降级查询数据库，cacheKey={}", cacheKey, e);
            return queryDbWithFallbackLimit(dbQuery);
        }

        if (RedisKeyConstants.EMPTY_VALUE.equals(cachedValue)) {
            throw new BusinessException(emptyMessage);
        }

        if (cachedValue == null) {
            return rebuildLogicalCacheWithMutex(cacheKey, lockKey, type, dbQuery, logicalTtl, emptyMessage);
        }

        LogicalCacheValue<T> logicalCacheValue = parseLogicalCacheValue(cachedValue, type, cacheKey);
        if (logicalCacheValue.data() == null) {
            return rebuildLogicalCacheWithMutex(cacheKey, lockKey, type, dbQuery, logicalTtl, emptyMessage);
        }

        if (logicalCacheValue.expireTime() != null && logicalCacheValue.expireTime().isAfter(LocalDateTime.now())) {
            return logicalCacheValue.data();
        }

        refreshLogicalCacheAsync(cacheKey, lockKey, dbQuery, logicalTtl);
        return logicalCacheValue.data();
    }

    public <T> void setWithLogicalExpire(String cacheKey, T data, Duration logicalTtl) {
        try {
            redisService.setWithLogicalExpire(cacheKey, data, logicalTtl);
        } catch (BusinessException e) {
            log.warn("写入逻辑过期缓存失败，cacheKey={}", cacheKey, e);
        }
    }

    private <T> T rebuildNormalCache(String cacheKey,
                                     Supplier<T> dbQuery,
                                     Duration cacheTtl,
                                     String emptyMessage) {
        T data;
        try {
            data = dbQuery.get();
        } catch (BusinessException e) {
            writeEmptyValue(cacheKey);
            throw new BusinessException(emptyMessage);
        }

        try {
            redisService.set(cacheKey, objectMapper.writeValueAsString(data), cacheTtl);
        } catch (JacksonException e) {
            throw new BusinessException("缓存数据写入失败");
        } catch (BusinessException e) {
            log.warn("写入Redis缓存失败，直接返回数据库数据，cacheKey={}", cacheKey, e);
        }

        return data;
    }

    private <T> T rebuildLogicalCacheWithMutex(String cacheKey,
                                               String lockKey,
                                               Class<T> type,
                                               Supplier<T> dbQuery,
                                               Duration logicalTtl,
                                               String emptyMessage) {
        String lockToken;
        try {
            lockToken = redisService.tryLock(
                    lockKey,
                    Duration.ofSeconds(flashSaleProperties.getCache().getRebuildLockSeconds())
            );
        } catch (BusinessException e) {
            log.warn("获取逻辑过期缓存重建锁失败，降级查询数据库，cacheKey={}", cacheKey, e);
            return queryDbWithFallbackLimit(dbQuery);
        }

        if (lockToken == null) {
            return retryGetLogicalCache(cacheKey, type, emptyMessage);
        }

        try {
            return rebuildLogicalCache(cacheKey, dbQuery, logicalTtl, emptyMessage);
        } finally {
            unlockQuietly(lockKey, lockToken);
        }
    }

    private <T> T rebuildLogicalCache(String cacheKey,
                                      Supplier<T> dbQuery,
                                      Duration logicalTtl,
                                      String emptyMessage) {
        T data;
        try {
            data = dbQuery.get();
        } catch (BusinessException e) {
            writeEmptyValue(cacheKey);
            throw new BusinessException(emptyMessage);
        }

        setWithLogicalExpire(cacheKey, data, logicalTtl);
        return data;
    }

    private <T> void refreshLogicalCacheAsync(String cacheKey,
                                              String lockKey,
                                              Supplier<T> dbQuery,
                                              Duration logicalTtl) {
        String lockToken;
        try {
            lockToken = redisService.tryLock(
                    lockKey,
                    Duration.ofSeconds(flashSaleProperties.getCache().getRebuildLockSeconds())
            );
        } catch (BusinessException e) {
            log.warn("获取逻辑过期缓存异步刷新锁失败，cacheKey={}", cacheKey, e);
            return;
        }

        if (lockToken == null) {
            return;
        }

        CompletableFuture.runAsync(TraceTaskWrapper.wrap(() -> {
            try {
                rebuildLogicalCache(cacheKey, dbQuery, logicalTtl, "数据不存在");
                log.info("逻辑过期缓存异步刷新成功，cacheKey={}", cacheKey);
            } catch (Exception e) {
                log.error("逻辑过期缓存异步刷新失败，cacheKey={}", cacheKey, e);
            } finally {
                unlockQuietly(lockKey, lockToken);
            }
        }));
    }

    private <T> T retryGetNormalCache(String cacheKey, Class<T> type, String emptyMessage) {
        int maxRetries = flashSaleProperties.getCache().getRebuildRetryTimes();

        for (int i = 0; i < maxRetries; i++) {
            sleepBeforeRetry();

            String cachedValue;
            try {
                cachedValue = redisService.get(cacheKey);
            } catch (BusinessException e) {
                throw new BusinessException("缓存重建中，请稍后重试");
            }

            if (RedisKeyConstants.EMPTY_VALUE.equals(cachedValue)) {
                throw new BusinessException(emptyMessage);
            }

            if (cachedValue != null) {
                return parseCacheValue(cachedValue, type);
            }
        }

        throw new BusinessException("缓存重建中，请稍后重试");
    }

    private <T> T retryGetLogicalCache(String cacheKey, Class<T> type, String emptyMessage) {
        int maxRetries = flashSaleProperties.getCache().getRebuildRetryTimes();

        for (int i = 0; i < maxRetries; i++) {
            sleepBeforeRetry();

            String cachedValue;
            try {
                cachedValue = redisService.get(cacheKey);
            } catch (BusinessException e) {
                throw new BusinessException("缓存重建中，请稍后重试");
            }

            if (RedisKeyConstants.EMPTY_VALUE.equals(cachedValue)) {
                throw new BusinessException(emptyMessage);
            }

            if (cachedValue != null) {
                LogicalCacheValue<T> logicalCacheValue = parseLogicalCacheValue(cachedValue, type, cacheKey);
                if (logicalCacheValue.data() != null) {
                    return logicalCacheValue.data();
                }
            }
        }

        throw new BusinessException("缓存重建中，请稍后重试");
    }

    private <T> T queryDbWithFallbackLimit(Supplier<T> dbQuery) {
        int current = dbFallbackConcurrency.incrementAndGet();
        try {
            if (current > flashSaleProperties.getCache().getDbFallbackMaxConcurrency()) {
                throw new BusinessException(ErrorCode.SYSTEM_BUSY);
            }
            return dbQuery.get();
        } finally {
            dbFallbackConcurrency.decrementAndGet();
        }
    }

    private void writeEmptyValue(String cacheKey) {
        try {
            redisService.set(
                    cacheKey,
                    RedisKeyConstants.EMPTY_VALUE,
                    Duration.ofMinutes(flashSaleProperties.getCache().getEmptyValueMinutes())
            );
        } catch (BusinessException e) {
            log.warn("写入空值缓存失败，cacheKey={}", cacheKey, e);
        }
    }

    private <T> T parseCacheValue(String cachedValue, Class<T> type) {
        try {
            return objectMapper.readValue(cachedValue, type);
        } catch (JacksonException e) {
            throw new BusinessException("缓存数据解析失败");
        }
    }

    private <T> LogicalCacheValue<T> parseLogicalCacheValue(String cachedValue, Class<T> type, String cacheKey) {
        try {
            CacheData<?> cacheData = objectMapper.readValue(cachedValue, CacheData.class);
            T data = null;
            if (cacheData.getData() != null) {
                String dataJson = objectMapper.writeValueAsString(cacheData.getData());
                data = objectMapper.readValue(dataJson, type);
            }
            return new LogicalCacheValue<>(data, cacheData.getExpireTime());
        } catch (JacksonException e) {
            log.warn("逻辑过期缓存解析失败，删除旧缓存后重建，cacheKey={}", cacheKey, e);
            try {
                redisService.delete(cacheKey);
            } catch (BusinessException redisException) {
                log.warn("删除异常缓存失败，cacheKey={}", cacheKey, redisException);
            }
            return new LogicalCacheValue<>(null, null);
        }
    }

    private void sleepBeforeRetry() {
        try {
            Thread.sleep(flashSaleProperties.getCache().getRebuildRetryIntervalMs());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException("读取缓存被中断");
        }
    }

    private void unlockQuietly(String lockKey, String lockToken) {
        try {
            redisService.unlock(lockKey, lockToken);
        } catch (BusinessException e) {
            log.warn("释放缓存重建锁失败，lockKey={}", lockKey, e);
        }
    }

    private record LogicalCacheValue<T>(T data, LocalDateTime expireTime) {
    }
}

