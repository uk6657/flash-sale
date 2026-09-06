package com.study.flashsale.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.study.flashsale.common.ErrorCode;
import com.study.flashsale.common.RedisKeyConstants;
import com.study.flashsale.dto.request.TestCreateUsersRequest;
import com.study.flashsale.dto.request.TestResetSeckillRequest;
import com.study.flashsale.dto.response.TestResetSeckillResponse;
import com.study.flashsale.dto.response.TestUserResponse;
import com.study.flashsale.entity.FlashSaleActivity;
import com.study.flashsale.entity.FlashSaleResult;
import com.study.flashsale.entity.Order;
import com.study.flashsale.entity.Product;
import com.study.flashsale.entity.User;
import com.study.flashsale.enums.CommonStatus;
import com.study.flashsale.exception.BusinessException;
import com.study.flashsale.mapper.FlashSaleActivityMapper;
import com.study.flashsale.mapper.FlashSaleResultMapper;
import com.study.flashsale.mapper.OrderMapper;
import com.study.flashsale.mapper.ProductMapper;
import com.study.flashsale.mapper.UserMapper;
import com.study.flashsale.infrastructure.redis.RedisService;
import com.study.flashsale.service.TestSupportService;
import com.study.flashsale.util.IdUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Profile({"default", "local", "dev"})
@Service
@RequiredArgsConstructor
public class TestSupportServiceImpl implements TestSupportService {

    private final FlashSaleActivityMapper activityMapper;
    private final FlashSaleResultMapper resultMapper;
    private final OrderMapper orderMapper;
    private final ProductMapper productMapper;
    private final UserMapper userMapper;
    private final RedisService redisService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TestResetSeckillResponse resetSeckill(Long activityId, TestResetSeckillRequest request) {
        FlashSaleActivity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "秒杀活动不存在");
        }

        Product product = productMapper.selectById(activity.getProductId());
        if (product == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "商品不存在");
        }

        int otherActivityStock = sumOtherEnabledActivityStock(activityId, activity.getProductId());
        if (otherActivityStock + request.getSaleStock() > product.getStock()) {
            throw new BusinessException("重置后的活动库存超过商品总库存");
        }

        LambdaQueryWrapper<Order> orderWrapper = new LambdaQueryWrapper<>();
        orderWrapper.eq(Order::getActivityId, activityId);
        int deletedOrders = orderMapper.delete(orderWrapper);

        LambdaQueryWrapper<FlashSaleResult> resultWrapper = new LambdaQueryWrapper<>();
        resultWrapper.eq(FlashSaleResult::getActivityId, activityId);
        int deletedResults = resultMapper.delete(resultWrapper);

        FlashSaleActivity updateActivity = new FlashSaleActivity();
        updateActivity.setId(activityId);
        updateActivity.setSaleStock(request.getSaleStock());
        activityMapper.updateById(updateActivity);

        Product updateProduct = new Product();
        updateProduct.setId(activity.getProductId());
        updateProduct.setLockStock(otherActivityStock + request.getSaleStock());
        productMapper.updateById(updateProduct);

        Long deletedRedisKeys = clearSeckillRedisKeys(activityId);
        redisService.set(
                RedisKeyConstants.flashSaleStock(activityId),
                String.valueOf(request.getSaleStock())
        );

        return new TestResetSeckillResponse(
                IdUtil.toString(activityId),
                request.getSaleStock(),
                deletedOrders,
                deletedResults,
                deletedRedisKeys
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<TestUserResponse> createUsers(TestCreateUsersRequest request) {
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        String encodedPassword = passwordEncoder.encode(request.getPassword());
        List<TestUserResponse> users = new ArrayList<>();

        for (int i = 1; i <= request.getCount(); i++) {
            String username = request.getUsernamePrefix() + i;
            User user = findUserByUsername(username);

            if (user == null) {
                user = new User();
                user.setUsername(username);
                user.setPassword(encodedPassword);
                user.setNickname(request.getNicknamePrefix() + i);
                user.setStatus(CommonStatus.ENABLED.getCode());
                userMapper.insert(user);
            }

            users.add(new TestUserResponse(
                    IdUtil.toString(user.getId()),
                    user.getUsername(),
                    request.getPassword()
            ));
        }

        return users;
    }

    private int sumOtherEnabledActivityStock(Long activityId, Long productId) {
        LambdaQueryWrapper<FlashSaleActivity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(FlashSaleActivity::getProductId, productId);
        queryWrapper.eq(FlashSaleActivity::getStatus, CommonStatus.ENABLED.getCode());
        queryWrapper.ne(FlashSaleActivity::getId, activityId);

        return activityMapper.selectList(queryWrapper).stream()
                .map(FlashSaleActivity::getSaleStock)
                .reduce(0, Integer::sum);
    }

    private Long clearSeckillRedisKeys(Long activityId) {
        long deletedCount = 0;

        deletedCount += deleteKeys(RedisKeyConstants.flashSaleStock(activityId));
        deletedCount += deleteKeys(RedisKeyConstants.flashSaleUserPattern(activityId));
        deletedCount += deleteKeys(RedisKeyConstants.rateLimitSeckillPattern(activityId));

        return deletedCount;
    }

    private long deleteKeys(String pattern) {
        Set<String> keys = redisService.keys(pattern);
        if (keys == null || keys.isEmpty()) {
            return 0;
        }

        Long deletedCount = redisService.delete(keys);
        return deletedCount == null ? 0 : deletedCount;
    }

    private User findUserByUsername(String username) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, username);
        return userMapper.selectOne(queryWrapper);
    }
}

