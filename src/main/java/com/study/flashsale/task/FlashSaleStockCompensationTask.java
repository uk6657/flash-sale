package com.study.flashsale.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.study.flashsale.common.RedisKeyConstants;
import com.study.flashsale.config.FlashSaleProperties;
import com.study.flashsale.entity.FlashSaleActivity;
import com.study.flashsale.entity.FlashSaleResult;
import com.study.flashsale.enums.CommonStatus;
import com.study.flashsale.enums.SeckillResultStatus;
import com.study.flashsale.mapper.FlashSaleActivityMapper;
import com.study.flashsale.mapper.FlashSaleResultMapper;
import com.study.flashsale.infrastructure.redis.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class FlashSaleStockCompensationTask {

    private final FlashSaleActivityMapper activityMapper;
    private final FlashSaleResultMapper resultMapper;
    private final RedisService redisService;
    private final FlashSaleProperties flashSaleProperties;
    private final ScheduledTaskLockExecutor scheduledTaskLockExecutor;

    @Scheduled(
            initialDelayString = "${flash-sale.task.stock-compensation-initial-delay-ms}",
            fixedDelayString = "${flash-sale.task.stock-compensation-fixed-delay-ms}"
    )
    public void compensateFlashSaleStock() {
        scheduledTaskLockExecutor.executeWithLock(
                RedisKeyConstants.LOCK_TASK_STOCK_COMPENSATION,
                Duration.ofMillis(flashSaleProperties.getTask().getScheduledLockTtlMs()),
                this::doCompensateFlashSaleStock
        );
    }

    private void doCompensateFlashSaleStock() {
        LocalDateTime now = LocalDateTime.now();

        LambdaQueryWrapper<FlashSaleActivity> activityQueryWrapper = new LambdaQueryWrapper<>();
        activityQueryWrapper.eq(FlashSaleActivity::getStatus, CommonStatus.ENABLED.getCode());
        activityQueryWrapper.le(FlashSaleActivity::getStartTime, now);
        activityQueryWrapper.gt(FlashSaleActivity::getEndTime, now);

        List<FlashSaleActivity> activities = activityMapper.selectList(activityQueryWrapper);
        if (activities.isEmpty()) {
            return;
        }

        for (FlashSaleActivity activity : activities) {
            try {
                if (hasQueuedSeckillResult(activity.getId())) {
                    continue;
                }

                String stockKey = RedisKeyConstants.flashSaleStock(activity.getId());
                String oldStock = redisService.get(stockKey);
                String latestStock = String.valueOf(activity.getSaleStock());

                redisService.set(stockKey, latestStock);

                if (!latestStock.equals(oldStock)) {
                    log.info("补偿秒杀库存成功，activityId={}, oldStock={}, latestStock={}",
                            activity.getId(), oldStock, latestStock);
                }
            } catch (Exception e) {
                log.error("补偿秒杀库存失败，activityId={}", activity.getId(), e);
            }
        }
    }

    private boolean hasQueuedSeckillResult(Long activityId) {
        LambdaQueryWrapper<FlashSaleResult> resultQueryWrapper = new LambdaQueryWrapper<>();
        resultQueryWrapper.eq(FlashSaleResult::getActivityId, activityId);
        resultQueryWrapper.eq(FlashSaleResult::getStatus, SeckillResultStatus.QUEUED.getCode());

        return resultMapper.selectCount(resultQueryWrapper) > 0;
    }
}

