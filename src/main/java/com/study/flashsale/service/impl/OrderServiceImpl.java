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
import com.study.flashsale.dto.request.PageRequest;
import com.study.flashsale.entity.FlashSaleActivity;
import com.study.flashsale.entity.Order;
import com.study.flashsale.enums.CommonStatus;
import com.study.flashsale.enums.OrderStatus;
import com.study.flashsale.exception.BusinessException;
import com.study.flashsale.mapper.FlashSaleActivityMapper;
import com.study.flashsale.mapper.OrderMapper;
import com.study.flashsale.service.FlashSaleResultService;
import com.study.flashsale.service.OrderService;
import com.study.flashsale.infrastructure.redis.RedisService;
import com.study.flashsale.util.IdUtil;
import com.study.flashsale.util.OrderNoGenerator;
import com.study.flashsale.vo.OrderVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements OrderService {

    private final OrderNoGenerator orderNoGenerator;
    private final FlashSaleActivityMapper activityMapper;
    private final RedisService redisService;
    private final FlashSaleProperties flashSaleProperties;
    private final FlashSaleResultService flashSaleResultService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createFlashSaleOrder(Long userId, Long activityId) {
        log.info("开始创建秒杀订单，userId={}, activityId={}", userId, activityId);

        LambdaQueryWrapper<Order> orderQueryWrapper = new LambdaQueryWrapper<>();
        orderQueryWrapper.eq(Order::getUserId, userId);
        orderQueryWrapper.eq(Order::getActivityId, activityId);
        Order existOrder = getOne(orderQueryWrapper);

        if (existOrder != null
                && !OrderStatus.isCanceled(existOrder.getStatus())
                && !OrderStatus.isPayFailed(existOrder.getStatus())) {
            log.info("秒杀订单已存在，复用已有订单，orderId={}, status={}, userId={}, activityId={}",
                    existOrder.getId(), existOrder.getStatus(), userId, activityId);
            return existOrder.getId();
        }

        FlashSaleActivity activity = activityMapper.selectById(activityId);
        if (activity == null || CommonStatus.DISABLED.getCode().equals(activity.getStatus())) {
            log.warn("创建秒杀订单失败，活动不存在或已禁用，userId={}, activityId={}", userId, activityId);
            throw new BusinessException(ErrorCode.NOT_FOUND, "秒杀活动不存在或已禁用");
        }

        LambdaUpdateWrapper<FlashSaleActivity> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(FlashSaleActivity::getId, activityId);
        updateWrapper.gt(FlashSaleActivity::getSaleStock, 0);
        updateWrapper.setSql("sale_stock = sale_stock - 1");

        int updateRows = activityMapper.update(null, updateWrapper);
        if (updateRows == 0) {
            log.warn("创建秒杀订单失败，数据库秒杀库存不足，userId={}, activityId={}", userId, activityId);
            throw new BusinessException("秒杀库存不足");
        }
        log.info("数据库秒杀库存扣减成功，userId={}, activityId={}", userId, activityId);

        Order order = existOrder == null ? new Order() : existOrder;
        order.setOrderNo(orderNoGenerator.generate());
        order.setUserId(userId);
        order.setActivityId(activityId);
        order.setProductId(activity.getProductId());
        order.setPrice(activity.getSalePrice());
        order.setStatus(OrderStatus.WAIT_PAY.getCode());

        try {
            if (existOrder == null) {
                save(order);
                log.info("秒杀订单写入成功，orderId={}, orderNo={}, userId={}, activityId={}",
                        order.getId(), order.getOrderNo(), userId, activityId);
            } else {
                updateById(order);
                log.info("秒杀订单重新创建成功，orderId={}, orderNo={}, userId={}, activityId={}",
                        order.getId(), order.getOrderNo(), userId, activityId);
            }
            long timeoutAt = System.currentTimeMillis()
                    + Duration.ofMinutes(flashSaleProperties.getOrder().getPayTimeoutMinutes()).toMillis();

            redisService.zAdd(
                    RedisKeyConstants.ORDER_TIMEOUT_QUEUE,
                    IdUtil.toString(order.getId()),
                    timeoutAt
            );
            log.info("订单超时队列写入成功，orderId={}, timeoutAt={}", order.getId(), timeoutAt);
        } catch (Exception e) {
            Order latestOrder = getOne(orderQueryWrapper);
            if (latestOrder != null) {
                log.warn("创建秒杀订单发生冲突，复用最新订单，orderId={}, userId={}, activityId={}",
                        latestOrder.getId(), userId, activityId, e);
                return latestOrder.getId();
            }

            throw e;
        }

        log.info("秒杀订单创建成功，orderId={}, orderNo={}, userId={}, activityId={}",
                order.getId(), order.getOrderNo(), userId, activityId);
        return order.getId();
    }

    @Override
    public PageResponse<OrderVO> listMyOrders(PageRequest request) {
        Long userId = getCurrentUserId();

        LambdaQueryWrapper<Order> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Order::getUserId, userId);
        queryWrapper.orderByDesc(Order::getId);

        Page<Order> page = page(new Page<>(request.getCurrent(), request.getSize()), queryWrapper);

        List<OrderVO> records = page.getRecords().stream()
                .map(this::toOrderVO)
                .toList();

        return new PageResponse<>(
                page.getTotal(),
                page.getCurrent(),
                page.getSize(),
                records
        );
    }

    @Override
    public PageResponse<OrderVO> listAllOrders(PageRequest request) {
        LambdaQueryWrapper<Order> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByDesc(Order::getId);

        Page<Order> page = page(new Page<>(request.getCurrent(), request.getSize()), queryWrapper);

        List<OrderVO> records = page.getRecords().stream()
                .map(this::toOrderVO)
                .toList();

        return new PageResponse<>(
                page.getTotal(),
                page.getCurrent(),
                page.getSize(),
                records
        );
    }

    @Override
    public OrderVO getOrderDetail(Long id) {
        Long userId = getCurrentUserId();
        return toOrderVO(getOwnedOrder(id, userId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVO payOrder(Long id) {
        Long userId = getCurrentUserId();
        Order order = getOwnedOrder(id, userId);

        if (OrderStatus.isPaid(order.getStatus())) {
            log.info("订单已支付，重复支付请求直接返回，orderId={}, userId={}", id, userId);
            return toOrderVO(order);
        }
        if (OrderStatus.isCanceled(order.getStatus())) {
            throw new BusinessException("订单已取消，不能支付");
        }
        if (OrderStatus.isPayFailed(order.getStatus())) {
            throw new BusinessException("订单支付失败，不能重复支付");
        }
        if (!OrderStatus.canPay(order.getStatus())) {
            throw new BusinessException("订单状态不允许支付");
        }

        LambdaUpdateWrapper<Order> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Order::getId, id);
        updateWrapper.eq(Order::getUserId, userId);
        updateWrapper.eq(Order::getStatus, OrderStatus.WAIT_PAY.getCode());
        updateWrapper.set(Order::getStatus, OrderStatus.PAID.getCode());

        int updateRows = baseMapper.update(null, updateWrapper);
        if (updateRows == 0) {
            Order latestOrder = getOwnedOrder(id, userId);
            if (OrderStatus.isPaid(latestOrder.getStatus())) {
                log.info("订单并发支付已完成，orderId={}, userId={}", id, userId);
                return toOrderVO(latestOrder);
            }
            log.warn("订单支付失败，订单状态已变化，orderId={}, userId={}, latestStatus={}",
                    id, userId, latestOrder.getStatus());
            throw new BusinessException("订单状态已变化，请刷新后重试");
        }
        removeOrderTimeoutQueueAfterCommit(id);
        log.info("订单支付成功，orderId={}, userId={}", id, userId);
        return toOrderVO(getById(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVO cancelOrder(Long id) {
        Long userId = getCurrentUserId();
        Order order = getOwnedOrder(id, userId);

        if (OrderStatus.isCanceled(order.getStatus())) {
            log.info("订单已取消，重复取消请求直接返回，orderId={}, userId={}", id, userId);
            return toOrderVO(order);
        }
        if (OrderStatus.isPaid(order.getStatus())) {
            throw new BusinessException("订单已支付，不能取消");
        }
        if (OrderStatus.isPayFailed(order.getStatus())) {
            throw new BusinessException("订单支付失败，不能取消");
        }
        if (!OrderStatus.canCancel(order.getStatus())) {
            throw new BusinessException("订单状态不允许取消");
        }

        LambdaUpdateWrapper<Order> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Order::getId, id);
        updateWrapper.eq(Order::getUserId, userId);
        updateWrapper.eq(Order::getStatus, OrderStatus.WAIT_PAY.getCode());
        updateWrapper.set(Order::getStatus, OrderStatus.CANCELED.getCode());

        int updateRows = baseMapper.update(null, updateWrapper);
        if (updateRows == 0) {
            Order latestOrder = getOwnedOrder(id, userId);
            if (OrderStatus.isCanceled(latestOrder.getStatus())) {
                log.info("订单并发取消已完成，orderId={}, userId={}", id, userId);
                return toOrderVO(latestOrder);
            }
            log.warn("订单取消失败，订单状态已变化，orderId={}, userId={}, latestStatus={}",
                    id, userId, latestOrder.getStatus());
            throw new BusinessException("订单状态已变化，请刷新后重试");
        }
        removeOrderTimeoutQueueAfterCommit(id);
        releaseActivityStock(order);
        deleteSeckillUserMarkerAfterCommit(order);
        flashSaleResultService.markFailed(userId, order.getActivityId(), "订单已取消");
        log.info("订单取消成功，orderId={}, userId={}", id, userId);
        return toOrderVO(getById(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVO failPayOrder(Long id) {
        Long userId = getCurrentUserId();
        Order order = getOwnedOrder(id, userId);

        if (OrderStatus.isPayFailed(order.getStatus())) {
            log.info("订单已标记支付失败，重复请求直接返回，orderId={}, userId={}", id, userId);
            return toOrderVO(order);
        }
        if (OrderStatus.isPaid(order.getStatus())) {
            throw new BusinessException("订单已支付，不能标记支付失败");
        }
        if (OrderStatus.isCanceled(order.getStatus())) {
            throw new BusinessException("订单已取消，不能标记支付失败");
        }
        if (!OrderStatus.canFailPay(order.getStatus())) {
            throw new BusinessException("订单状态不允许标记支付失败");
        }

        LambdaUpdateWrapper<Order> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Order::getId, id);
        updateWrapper.eq(Order::getUserId, userId);
        updateWrapper.eq(Order::getStatus, OrderStatus.WAIT_PAY.getCode());
        updateWrapper.set(Order::getStatus, OrderStatus.PAY_FAILED.getCode());

        int updateRows = baseMapper.update(null, updateWrapper);
        if (updateRows == 0) {
            Order latestOrder = getOwnedOrder(id, userId);
            if (OrderStatus.isPayFailed(latestOrder.getStatus())) {
                log.info("订单并发标记支付失败已完成，orderId={}, userId={}", id, userId);
                return toOrderVO(latestOrder);
            }
            log.warn("订单标记支付失败失败，订单状态已变化，orderId={}, userId={}, latestStatus={}",
                    id, userId, latestOrder.getStatus());
            throw new BusinessException("订单状态已变化，请刷新后重试");
        }

        removeOrderTimeoutQueueAfterCommit(id);
        releaseActivityStock(order);
        deleteSeckillUserMarkerAfterCommit(order);
        flashSaleResultService.markFailed(userId, order.getActivityId(), "支付失败");
        log.info("订单支付失败，orderId={}, userId={}", id, userId);
        return toOrderVO(getById(id));
    }

    private Long getCurrentUserId() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return userId;
    }

    private Order getOwnedOrder(Long id, Long userId) {
        Order order = getById(id);
        if (order == null || !userId.equals(order.getUserId())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "订单不存在");
        }
        return order;
    }

    private OrderVO toOrderVO(Order order) {
        return new OrderVO(
                IdUtil.toString(order.getId()),
                order.getOrderNo(),
                IdUtil.toString(order.getActivityId()),
                IdUtil.toString(order.getProductId()),
                order.getPrice(),
                order.getStatus(),
                OrderStatus.getTextByCode(order.getStatus()),
                order.getCreatedAt()
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelTimeoutOrder(Long id) {
        Order order = getById(id);
        if (order == null) {
            return;
        }

        if (!OrderStatus.canCancel(order.getStatus())) {
            return;
        }

        LambdaUpdateWrapper<Order> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Order::getId, id);
        updateWrapper.eq(Order::getStatus, OrderStatus.WAIT_PAY.getCode());
        updateWrapper.set(Order::getStatus, OrderStatus.CANCELED.getCode());

        int updateRows = baseMapper.update(null, updateWrapper);
        if (updateRows > 0) {
            removeOrderTimeoutQueueAfterCommit(id);
            releaseActivityStock(order);
            deleteSeckillUserMarkerAfterCommit(order);
            flashSaleResultService.markFailed(order.getUserId(), order.getActivityId(), "订单超时未支付，已自动取消");
            log.info("超时订单自动取消成功，orderId={}", id);
        }
    }

    private void releaseActivityStock(Order order) {
        LambdaUpdateWrapper<FlashSaleActivity> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(FlashSaleActivity::getId, order.getActivityId());
        updateWrapper.setSql("sale_stock = sale_stock + 1");
        int updateRows = activityMapper.update(null, updateWrapper);
        log.info("释放数据库秒杀库存，orderId={}, activityId={}, updateRows={}",
                order.getId(), order.getActivityId(), updateRows);

        runAfterCommit(() -> {
            redisService.increment(RedisKeyConstants.flashSaleStock(order.getActivityId()));
            log.info("释放Redis秒杀库存成功，orderId={}, activityId={}", order.getId(), order.getActivityId());
        });
    }

    private void removeOrderTimeoutQueueAfterCommit(Long orderId) {
        runAfterCommit(() -> {
            redisService.zRemove(
                    RedisKeyConstants.ORDER_TIMEOUT_QUEUE,
                    IdUtil.toString(orderId)
            );
            log.info("订单超时队列删除成功，orderId={}", orderId);
        });
    }

    private void deleteSeckillUserMarkerAfterCommit(Order order) {
        runAfterCommit(() -> {
            redisService.delete(
                    RedisKeyConstants.flashSaleUser(order.getActivityId(), order.getUserId())
            );
            log.info("秒杀用户参与标记删除成功，orderId={}, userId={}, activityId={}",
                    order.getId(), order.getUserId(), order.getActivityId());
        });
    }

    private void runAfterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    action.run();
                } catch (Exception e) {
                    log.error("事务提交后执行Redis操作失败", e);
                }
            }
        });
    }
}

