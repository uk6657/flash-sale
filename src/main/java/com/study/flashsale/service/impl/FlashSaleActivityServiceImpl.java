package com.study.flashsale.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.study.flashsale.common.ErrorCode;
import com.study.flashsale.common.PageResponse;
import com.study.flashsale.common.RedisKeyConstants;
import com.study.flashsale.config.FlashSaleProperties;
import com.study.flashsale.context.UserContext;
import com.study.flashsale.dto.message.SeckillOrderMessage;
import com.study.flashsale.dto.request.ActivityCreateRequest;
import com.study.flashsale.dto.request.ActivityUpdateRequest;
import com.study.flashsale.dto.request.PageRequest;
import com.study.flashsale.dto.response.ActivitySummaryResponse;
import com.study.flashsale.dto.response.SeckillResponse;
import com.study.flashsale.dto.response.SeckillResultResponse;
import com.study.flashsale.entity.FlashSaleActivity;
import com.study.flashsale.entity.FlashSaleResult;
import com.study.flashsale.entity.MqMessage;
import com.study.flashsale.entity.Order;
import com.study.flashsale.entity.Product;
import com.study.flashsale.enums.ActivityTimeStatus;
import com.study.flashsale.enums.CommonStatus;
import com.study.flashsale.enums.OrderStatus;
import com.study.flashsale.enums.SeckillResultStatus;
import com.study.flashsale.exception.BusinessException;
import com.study.flashsale.mapper.FlashSaleActivityMapper;
import com.study.flashsale.mapper.OrderMapper;
import com.study.flashsale.mapper.ProductMapper;
import com.study.flashsale.infrastructure.cache.CacheService;
import com.study.flashsale.mq.producer.SeckillOrderProducer;
import com.study.flashsale.service.FlashSaleActivityService;
import com.study.flashsale.service.FlashSaleResultService;
import com.study.flashsale.service.MqMessageService;
import com.study.flashsale.infrastructure.redis.RedisService;
import com.study.flashsale.util.IdUtil;
import com.study.flashsale.vo.ActivityVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class FlashSaleActivityServiceImpl extends ServiceImpl<FlashSaleActivityMapper, FlashSaleActivity> implements FlashSaleActivityService {

    private final RedisService redisService;
    private final FlashSaleResultService flashSaleResultService;
    private final SeckillOrderProducer seckillOrderProducer;
    private final ProductMapper productMapper;
    private final OrderMapper orderMapper;
    private final FlashSaleProperties flashSaleProperties;
    private final CacheService cacheService;
    private final MqMessageService mqMessageService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ActivityVO createActivity(ActivityCreateRequest request) {
        if (request.getStatus() != null && CommonStatus.isInvalid(request.getStatus())) {
            throw new BusinessException("活动状态只能是0或1");
        }

        if (!request.getEndTime().isAfter(request.getStartTime())) {
            throw new BusinessException("活动结束时间必须晚于开始时间");
        }

        if (!request.getEndTime().isAfter(LocalDateTime.now())) {
            throw new BusinessException("活动结束时间不能早于当前时间");
        }

        Long productId = IdUtil.parseId(request.getProductId());
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "商品不存在");
        }
        if (CommonStatus.DISABLED.getCode().equals(product.getStatus())) {
            throw new BusinessException("商品已下架，不能创建秒杀活动");
        }
        if (request.getSalePrice().compareTo(product.getPrice()) > 0) {
            throw new BusinessException("秒杀价格不能高于商品原价");
        }

        freezeProductStock(productId, request.getSaleStock());

        FlashSaleActivity activity = new FlashSaleActivity();
        activity.setProductId(productId);
        activity.setSalePrice(request.getSalePrice());
        activity.setSaleStock(request.getSaleStock());
        activity.setStartTime(request.getStartTime());
        activity.setEndTime(request.getEndTime());
        activity.setStatus(request.getStatus() == null ? CommonStatus.ENABLED.getCode() : request.getStatus());

        save(activity);
        return toActivityVO(activity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ActivityVO updateActivity(Long id, ActivityUpdateRequest request) {
        FlashSaleActivity oldActivity = getById(id);
        if (oldActivity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "秒杀活动不存在");
        }

        if (!ActivityTimeStatus.NOT_STARTED.getCode().equals(getActivityTimeStatus(oldActivity))) {
            throw new BusinessException("活动已开始，不能修改活动配置");
        }

        if (CommonStatus.isInvalid(request.getStatus())) {
            throw new BusinessException("活动状态只能是0或1");
        }

        if (!request.getEndTime().isAfter(request.getStartTime())) {
            throw new BusinessException("活动结束时间必须晚于开始时间");
        }

        if (!request.getEndTime().isAfter(LocalDateTime.now())) {
            throw new BusinessException("活动结束时间不能早于当前时间");
        }

        Product product = productMapper.selectById(oldActivity.getProductId());
        if (product == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "商品不存在");
        }
        if (CommonStatus.DISABLED.getCode().equals(product.getStatus())) {
            throw new BusinessException("商品已下架，不能修改秒杀活动");
        }
        if (request.getSalePrice().compareTo(product.getPrice()) > 0) {
            throw new BusinessException("秒杀价格不能高于商品原价");
        }

        int stockDiff = request.getSaleStock() - oldActivity.getSaleStock();
        if (stockDiff > 0) {
            freezeProductStock(oldActivity.getProductId(), stockDiff);
        }
        if (stockDiff < 0) {
            releaseProductStock(oldActivity.getProductId(), -stockDiff);
        }

        FlashSaleActivity activity = new FlashSaleActivity();
        activity.setId(id);
        activity.setProductId(oldActivity.getProductId());
        activity.setSalePrice(request.getSalePrice());
        activity.setSaleStock(request.getSaleStock());
        activity.setStartTime(request.getStartTime());
        activity.setEndTime(request.getEndTime());
        activity.setStatus(request.getStatus());

        updateById(activity);
        redisService.delete(RedisKeyConstants.flashSaleStock(id));
        redisService.delete(RedisKeyConstants.activityDetail(id));

        return toActivityVO(getById(id));
    }

    private void freezeProductStock(Long productId, Integer freezeStock) {
        LambdaUpdateWrapper<Product> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Product::getId, productId);
        updateWrapper.eq(Product::getStatus, 1);
        updateWrapper.apply("stock - lock_stock >= {0}", freezeStock);
        updateWrapper.setSql("lock_stock = lock_stock + " + freezeStock);

        if (productMapper.update(null, updateWrapper) == 0) {
            throw new BusinessException("商品可用库存不足");
        }
    }

    private void releaseProductStock(Long productId, Integer releaseStock) {
        LambdaUpdateWrapper<Product> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Product::getId, productId);
        updateWrapper.ge(Product::getLockStock, releaseStock);
        updateWrapper.setSql("lock_stock = lock_stock - " + releaseStock);

        int updateRows = productMapper.update(null, updateWrapper);
        if (updateRows == 0) {
            throw new BusinessException("释放商品冻结库存失败");
        }
    }

    @Override
    public PageResponse<ActivityVO> listActivities(PageRequest request) {
        LambdaQueryWrapper<FlashSaleActivity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(FlashSaleActivity::getStatus, CommonStatus.ENABLED.getCode());
        queryWrapper.orderByDesc(FlashSaleActivity::getId);

        Page<FlashSaleActivity> page = page(new Page<>(request.getCurrent(), request.getSize()), queryWrapper);

        List<ActivityVO> records = page.getRecords().stream()
                .map(this::toActivityVO)
                .toList();

        return new PageResponse<>(
                page.getTotal(),
                page.getCurrent(),
                page.getSize(),
                records
        );
    }

    @Override
    public ActivityVO getActivityDetail(Long id) {
        return cacheService.queryWithLogicalExpire(
                RedisKeyConstants.activityDetail(id),
                RedisKeyConstants.lockActivityDetail(id),
                ActivityVO.class,
                () -> getActivityDetailFromDb(id),
                Duration.ofMinutes(flashSaleProperties.getCache().getActivityDetailMinutes()),
                "秒杀活动不存在或已禁用"
        );
    }

    private ActivityVO getActivityDetailFromDb(Long id) {
        FlashSaleActivity activity = getById(id);
        if (activity == null || CommonStatus.DISABLED.getCode().equals(activity.getStatus())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "秒杀活动不存在或已禁用");
        }
        return toActivityVO(activity);
    }

    @Override
    public void prepareStock(Long id) {
        FlashSaleActivity activity = getById(id);
        if (activity == null || CommonStatus.DISABLED.getCode().equals(activity.getStatus())) {
            log.warn("秒杀库存预热失败，活动不存在或已禁用，activityId={}", id);
            throw new BusinessException(ErrorCode.NOT_FOUND, "秒杀活动不存在或已禁用");
        }
        if (ActivityTimeStatus.ENDED.getCode().equals(getActivityTimeStatus(activity))) {
            log.warn("秒杀库存预热失败，活动已结束，activityId={}", id);
            throw new BusinessException("秒杀活动已结束，不能预热库存");
        }

        String stockKey = RedisKeyConstants.flashSaleStock(id);
        redisService.set(stockKey, String.valueOf(activity.getSaleStock()));
        log.info("秒杀库存预热成功，activityId={}, saleStock={}", id, activity.getSaleStock());

        try {
            cacheService.setWithLogicalExpire(
                    RedisKeyConstants.activityDetail(id),
                    toActivityVO(activity),
                    Duration.ofMinutes(flashSaleProperties.getCache().getActivityDetailMinutes())
            );
        } catch (BusinessException e) {
            log.warn("预热活动详情缓存失败，activityId={}", id, e);
        }
    }

    @Override
    public SeckillResponse seckill(Long activityId) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        log.info("收到秒杀请求，userId={}, activityId={}", userId, activityId);

        FlashSaleActivity activity = getById(activityId);
        if (activity == null || CommonStatus.DISABLED.getCode().equals(activity.getStatus())) {
            log.warn("秒杀请求被拒绝，活动不存在或已禁用，userId={}, activityId={}", userId, activityId);
            throw new BusinessException(ErrorCode.NOT_FOUND, "秒杀活动不存在或已禁用");
        }

        int timeStatus = getActivityTimeStatus(activity);
        if (ActivityTimeStatus.NOT_STARTED.getCode().equals(timeStatus)) {
            log.warn("秒杀请求被拒绝，活动未开始，userId={}, activityId={}", userId, activityId);
            throw new BusinessException("秒杀活动未开始");
        }
        if (ActivityTimeStatus.ENDED.getCode().equals(timeStatus)) {
            log.warn("秒杀请求被拒绝，活动已结束，userId={}, activityId={}", userId, activityId);
            throw new BusinessException("秒杀活动已结束");
        }
        log.info("秒杀活动校验通过，userId={}, activityId={}", userId, activityId);

        FlashSaleResult existResult = flashSaleResultService.getByUserAndActivity(userId, activityId);
        if (existResult != null && !SeckillResultStatus.FAILED.getCode().equals(existResult.getStatus())) {
            log.info("秒杀请求命中已有结果，userId={}, activityId={}, status={}",
                    userId, activityId, existResult.getStatus());
            return new SeckillResponse(
                    existResult.getStatus(),
                    SeckillResultStatus.getTextByCode(existResult.getStatus()),
                    existResult.getMessage()
            );
        }

        String rateLimitKey = RedisKeyConstants.rateLimitSeckill(activityId, userId);
        Long requestCount;
        try {
            requestCount = redisService.increment(rateLimitKey);
        } catch (BusinessException e) {
            log.warn("秒杀限流Redis不可用，activityId={}, userId={}", activityId, userId, e);
            throw new BusinessException(ErrorCode.SERVICE_DEGRADED, "当前参与人数较多，请稍后再试");
        }

        if (requestCount != null && requestCount == 1) {
            try {
                redisService.expire(
                        rateLimitKey,
                        Duration.ofSeconds(flashSaleProperties.getSeckill().getRateLimitSeconds())
                );
            } catch (BusinessException e) {
                log.warn("秒杀限流过期时间设置失败，activityId={}, userId={}", activityId, userId, e);
                throw new BusinessException(ErrorCode.SERVICE_DEGRADED, "当前参与人数较多，请稍后再试");
            }
        }

        if (requestCount != null && requestCount > flashSaleProperties.getSeckill().getRateLimitCount()) {
            log.warn("秒杀请求被限流，userId={}, activityId={}, requestCount={}", userId, activityId, requestCount);
            throw new BusinessException("请求过于频繁，请稍后再试");
        }

        String userKey = RedisKeyConstants.flashSaleUser(activityId, userId);
        Duration userKeyTtl = Duration.between(LocalDateTime.now(), activity.getEndTime())
                .plusMinutes(flashSaleProperties.getSeckill().getUserMarkerExtraMinutes());

        String stockKey = RedisKeyConstants.flashSaleStock(activityId);
        int seckillResult;
        try {
            seckillResult = redisService.tryAcquireSeckill(userKey, stockKey, userKeyTtl);
        } catch (BusinessException e) {
            log.warn("秒杀库存Redis不可用，activityId={}, userId={}", activityId, userId, e);
            throw new BusinessException(ErrorCode.SERVICE_DEGRADED, "当前参与人数较多，请稍后再试");
        }
        if (seckillResult == RedisService.SECKILL_DUPLICATE) {
            log.warn("秒杀请求被拒绝，重复参与，userId={}, activityId={}", userId, activityId);
            throw new BusinessException("不能重复参与同一场秒杀");
        }
        if (seckillResult == RedisService.SECKILL_STOCK_NOT_PREPARED) {
            log.warn("秒杀请求被拒绝，库存未预热，userId={}, activityId={}", userId, activityId);
            throw new BusinessException("秒杀库存未预热");
        }
        if (seckillResult == RedisService.SECKILL_OUT_OF_STOCK) {
            log.warn("秒杀请求被拒绝，库存不足，userId={}, activityId={}", userId, activityId);
            throw new BusinessException("秒杀库存不足");
        }
        if (seckillResult != RedisService.SECKILL_ACCEPTED) {
            log.warn("秒杀请求处理异常，未知Redis脚本返回值，userId={}, activityId={}, seckillResult={}",
                    userId, activityId, seckillResult);
            throw new BusinessException("当前参与人数较多，请稍后再试");
        }
        log.info("Redis秒杀资格获取成功，userId={}, activityId={}", userId, activityId);

        boolean queuedResultCreated = false;
        try {
            flashSaleResultService.createQueuedResult(userId, activityId);
            queuedResultCreated = true;
            log.info("秒杀排队结果创建成功，userId={}, activityId={}", userId, activityId);
            SeckillOrderMessage message = new SeckillOrderMessage(
                    UUID.randomUUID().toString(),
                    userId,
                    activityId,
                    LocalDateTime.now()
            );

            MqMessage mqMessage = mqMessageService.createPendingOrderMessage(message);
            try {
                seckillOrderProducer.sendCreateOrderMessage(mqMessage);
            } catch (Exception sendException) {
                mqMessageService.markSendFailed(message.getMessageId(), "Send exception: " + sendException.getMessage());
                log.error("Seckill order message send failed, messageId={}, userId={}, activityId={}",
                        message.getMessageId(), userId, activityId, sendException);
            }
            log.info("秒杀下单消息发送成功，messageId={}, userId={}, activityId={}",
                    message.getMessageId(), userId, activityId);
        } catch (Exception e) {
            try {
                rollbackAcceptedSeckill(userKey, stockKey);
            } catch (BusinessException redisException) {
                log.error("回滚已接收的秒杀Redis状态失败，activityId={}, userId={}", activityId, userId, redisException);
            }
            if (queuedResultCreated) {
                flashSaleResultService.markFailed(userId, activityId, "当前参与人数较多，请稍后再试");
            }
            if (e instanceof BusinessException businessException) {
                throw businessException;
            }
            throw new BusinessException("秒杀请求处理失败，请稍后重试");
        }

        log.info("秒杀请求已接收，userId={}, activityId={}", userId, activityId);
        return new SeckillResponse(
                SeckillResultStatus.QUEUED.getCode(),
                SeckillResultStatus.QUEUED.getText(),
                SeckillResultStatus.QUEUED.getText()
        );
    }

    @Override
    public SeckillResultResponse getSeckillResult(Long activityId) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        FlashSaleResult result = flashSaleResultService.getByUserAndActivity(userId, activityId);
        if (result == null) {
            return new SeckillResultResponse(
                    SeckillResultStatus.FAILED.getCode(),
                    SeckillResultStatus.FAILED.getText(),
                    "未参与该秒杀活动",
                    null
            );
        }

        return new SeckillResultResponse(
                result.getStatus(),
                SeckillResultStatus.getTextByCode(result.getStatus()),
                result.getMessage(),
                IdUtil.toString(result.getOrderId())
        );
    }

    @Override
    public ActivitySummaryResponse getActivitySummary(Long activityId) {
        FlashSaleActivity activity = getById(activityId);
        if (activity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "秒杀活动不存在");
        }

        Long redisStock = null;
        try {
            String redisStockValue = redisService.get(RedisKeyConstants.flashSaleStock(activityId));
            if (redisStockValue != null) {
                redisStock = Long.valueOf(redisStockValue);
            }
        } catch (Exception e) {
            log.warn("查询活动汇总时读取Redis库存失败，activityId={}", activityId, e);
        }

        int timeStatus = getActivityTimeStatus(activity);
        return new ActivitySummaryResponse(
                IdUtil.toString(activity.getId()),
                IdUtil.toString(activity.getProductId()),
                activity.getStatus(),
                CommonStatus.getTextByCode(activity.getStatus()),
                timeStatus,
                ActivityTimeStatus.getTextByCode(timeStatus),
                activity.getSaleStock(),
                redisStock,
                countSeckillResult(activityId, SeckillResultStatus.QUEUED.getCode()),
                countSeckillResult(activityId, SeckillResultStatus.SUCCESS.getCode()),
                countSeckillResult(activityId, SeckillResultStatus.FAILED.getCode()),
                countOrder(activityId, OrderStatus.WAIT_PAY.getCode()),
                countOrder(activityId, OrderStatus.PAID.getCode()),
                countOrder(activityId, OrderStatus.CANCELED.getCode()),
                countOrder(activityId, OrderStatus.PAY_FAILED.getCode())
        );
    }

    private Long countSeckillResult(Long activityId, Integer status) {
        LambdaQueryWrapper<FlashSaleResult> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(FlashSaleResult::getActivityId, activityId);
        queryWrapper.eq(FlashSaleResult::getStatus, status);
        return flashSaleResultService.count(queryWrapper);
    }

    private Long countOrder(Long activityId, Integer status) {
        LambdaQueryWrapper<Order> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Order::getActivityId, activityId);
        queryWrapper.eq(Order::getStatus, status);
        return orderMapper.selectCount(queryWrapper);
    }

    private ActivityVO toActivityVO(FlashSaleActivity activity) {
        int timeStatus = getActivityTimeStatus(activity);
        return new ActivityVO(
                IdUtil.toString(activity.getId()),
                IdUtil.toString(activity.getProductId()),
                activity.getSalePrice(),
                activity.getSaleStock(),
                activity.getStartTime(),
                activity.getEndTime(),
                activity.getStatus(),
                CommonStatus.getTextByCode(activity.getStatus()),
                timeStatus,
                ActivityTimeStatus.getTextByCode(timeStatus)
        );
    }

    private int getActivityTimeStatus(FlashSaleActivity activity) {
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(activity.getStartTime())) {
            return ActivityTimeStatus.NOT_STARTED.getCode();
        }
        if (now.isAfter(activity.getEndTime())) {
            return ActivityTimeStatus.ENDED.getCode();
        }
        return ActivityTimeStatus.IN_PROGRESS.getCode();
    }

    private void rollbackAcceptedSeckill(String userKey, String stockKey) {
        redisService.increment(stockKey);
        redisService.delete(userKey);
    }
}
