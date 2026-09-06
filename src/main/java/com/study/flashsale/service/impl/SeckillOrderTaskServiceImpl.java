package com.study.flashsale.service.impl;

import com.study.flashsale.common.RedisKeyConstants;
import com.study.flashsale.infrastructure.redis.RedisService;
import com.study.flashsale.service.FlashSaleResultService;
import com.study.flashsale.service.OrderService;
import com.study.flashsale.service.SeckillOrderTaskService;
import com.study.flashsale.util.TraceTaskWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.util.concurrent.RejectedExecutionException;

@Service
@Slf4j
@RequiredArgsConstructor
public class SeckillOrderTaskServiceImpl implements SeckillOrderTaskService {

    @Qualifier("orderTaskExecutor")
    private final ThreadPoolTaskExecutor orderTaskExecutor;

    private final OrderService orderService;

    private final FlashSaleResultService flashSaleResultService;

    private final RedisService redisService;

    @Override
    public void submitOrderTask(Long userId, Long activityId) {
        try {
            orderTaskExecutor.execute(TraceTaskWrapper.wrap(() -> executeOrderTask(userId, activityId)));
        } catch (RejectedExecutionException e) {
            log.error("异步下单任务提交被线程池拒绝，userId={}, activityId={}, activeCount={}, queueSize={}",
                    userId,
                    activityId,
                    orderTaskExecutor.getActiveCount(),
                    orderTaskExecutor.getThreadPoolExecutor().getQueue().size(),
                    e);
            throw e;
        } catch (Exception e) {
            log.error("异步下单任务提交失败，userId={}, activityId={}", userId, activityId, e);
            throw e;
        }
    }

    private void executeOrderTask(Long userId, Long activityId) {
        long startTime = System.currentTimeMillis();
        try {
            log.info("异步下单任务开始处理，userId={}, activityId={}", userId, activityId);
            Long orderId = orderService.createFlashSaleOrder(userId, activityId);
            flashSaleResultService.markSuccess(userId, activityId, orderId);
            log.info("异步下单任务处理成功，userId={}, activityId={}, orderId={}, cost={}ms",
                    userId, activityId, orderId, System.currentTimeMillis() - startTime);
        } catch (Exception e) {
            log.error("异步下单任务处理失败，userId={}, activityId={}, cost={}ms",
                    userId, activityId, System.currentTimeMillis() - startTime, e);
            handleOrderFailed(userId, activityId, "下单失败：" + e.getMessage());
        }
    }

    private void handleOrderFailed(Long userId, Long activityId, String message) {
        log.warn("开始处理异步下单失败补偿，userId={}, activityId={}, message={}", userId, activityId, message);
        flashSaleResultService.markFailed(userId, activityId, message);

        String stockKey = RedisKeyConstants.flashSaleStock(activityId);
        redisService.increment(stockKey);

        String userKey = RedisKeyConstants.flashSaleUser(activityId, userId);
        redisService.delete(userKey);
        log.info("异步下单失败补偿完成，userId={}, activityId={}", userId, activityId);
    }
}
