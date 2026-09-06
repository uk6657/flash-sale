package com.study.flashsale.task;

import com.study.flashsale.common.RedisKeyConstants;
import com.study.flashsale.config.FlashSaleProperties;
import com.study.flashsale.service.OrderService;
import com.study.flashsale.infrastructure.redis.RedisService;
import com.study.flashsale.util.IdUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutTask {

    private final RedisService redisService;
    private final OrderService orderService;
    private final FlashSaleProperties flashSaleProperties;
    private final ScheduledTaskLockExecutor scheduledTaskLockExecutor;

    @Scheduled(fixedDelayString = "${flash-sale.task.order-timeout-fixed-delay-ms}")
    public void handleTimeoutOrders() {
        scheduledTaskLockExecutor.executeWithLock(
                RedisKeyConstants.LOCK_TASK_ORDER_TIMEOUT,
                Duration.ofMillis(flashSaleProperties.getTask().getScheduledLockTtlMs()),
                this::doHandleTimeoutOrders
        );
    }

    private void doHandleTimeoutOrders() {
        long now = System.currentTimeMillis();

        Set<String> orderIds = redisService.zRangeByScore(
                RedisKeyConstants.ORDER_TIMEOUT_QUEUE,
                0,
                now,
                0,
                flashSaleProperties.getTask().getOrderTimeoutBatchSize()
        );

        if (orderIds == null || orderIds.isEmpty()) {
            return;
        }

        log.info("开始处理超时订单，本轮数量={}", orderIds.size());
        for (String orderId : orderIds) {
            try {
                orderService.cancelTimeoutOrder(IdUtil.parseId(orderId));

                redisService.zRemove(
                        RedisKeyConstants.ORDER_TIMEOUT_QUEUE,
                        orderId
                );
                log.info("超时订单处理完成，orderId={}", orderId);
            } catch (Exception e) {
                log.error("处理超时订单失败，orderId={}", orderId, e);
            }
        }
    }
}

