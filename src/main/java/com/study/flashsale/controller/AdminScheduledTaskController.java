package com.study.flashsale.controller;

import com.study.flashsale.annotation.LogOperation;
import com.study.flashsale.common.RedisKeyConstants;
import com.study.flashsale.common.Result;
import com.study.flashsale.config.FlashSaleProperties;
import com.study.flashsale.infrastructure.redis.RedisService;
import com.study.flashsale.vo.ScheduledTaskLockStatusVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Tag(name = "管理端-定时任务")
@RestController
@RequestMapping("/api/admin/scheduled-tasks")
@RequiredArgsConstructor
public class AdminScheduledTaskController {

    private final RedisService redisService;

    private final FlashSaleProperties flashSaleProperties;

    @Operation(summary = "查询定时任务分布式锁状态")
    @LogOperation("查询定时任务锁状态")
    @GetMapping("/locks")
    public Result<List<ScheduledTaskLockStatusVO>> listTaskLocks() {
        return Result.success(List.of(
                buildLockStatus(
                        "order-timeout",
                        RedisKeyConstants.LOCK_TASK_ORDER_TIMEOUT,
                        flashSaleProperties.getTask().getOrderTimeoutFixedDelayMs(),
                        "扫描 Redis 延迟队列，取消超时未支付订单"
                ),
                buildLockStatus(
                        "stock-compensation",
                        RedisKeyConstants.LOCK_TASK_STOCK_COMPENSATION,
                        flashSaleProperties.getTask().getStockCompensationFixedDelayMs(),
                        "补偿进行中活动的秒杀库存缓存"
                ),
                buildLockStatus(
                        "mq-message-retry",
                        RedisKeyConstants.LOCK_TASK_MQ_MESSAGE_RETRY,
                        flashSaleProperties.getMq().getSendRetryFixedDelayMs(),
                        "扫描本地消息表，重试发送失败的 MQ 消息"
                )
        ));
    }

    private ScheduledTaskLockStatusVO buildLockStatus(
            String taskName,
            String lockKey,
            Long fixedDelayMs,
            String description
    ) {
        Long ttlMs = redisService.getExpire(lockKey, TimeUnit.MILLISECONDS);
        boolean locked = ttlMs != null && ttlMs != -2;

        return new ScheduledTaskLockStatusVO(
                taskName,
                lockKey,
                locked,
                ttlMs,
                fixedDelayMs,
                description
        );
    }
}
