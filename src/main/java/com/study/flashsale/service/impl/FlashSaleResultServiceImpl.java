package com.study.flashsale.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.study.flashsale.common.PageResponse;
import com.study.flashsale.dto.request.PageRequest;
import com.study.flashsale.entity.FlashSaleResult;
import com.study.flashsale.enums.SeckillResultStatus;
import com.study.flashsale.mapper.FlashSaleResultMapper;
import com.study.flashsale.service.FlashSaleResultService;
import com.study.flashsale.util.IdUtil;
import com.study.flashsale.vo.FlashSaleResultVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class FlashSaleResultServiceImpl extends ServiceImpl<FlashSaleResultMapper, FlashSaleResult> implements FlashSaleResultService {

    @Override
    public void createQueuedResult(Long userId, Long activityId) {
        FlashSaleResult existResult = getByUserAndActivity(userId, activityId);
        if (existResult != null) {
            existResult.setStatus(SeckillResultStatus.QUEUED.getCode());
            existResult.setMessage(SeckillResultStatus.QUEUED.getText());
            existResult.setOrderId(null);
            updateById(existResult);
            log.info("秒杀结果更新为排队中，resultId={}, userId={}, activityId={}",
                    existResult.getId(), userId, activityId);
            return;
        }

        FlashSaleResult result = new FlashSaleResult();
        result.setUserId(userId);
        result.setActivityId(activityId);
        result.setStatus(SeckillResultStatus.QUEUED.getCode());
        result.setMessage(SeckillResultStatus.QUEUED.getText());

        save(result);
        log.info("秒杀结果创建为排队中，resultId={}, userId={}, activityId={}",
                result.getId(), userId, activityId);
    }

    @Override
    public FlashSaleResult getByUserAndActivity(Long userId, Long activityId) {
        LambdaQueryWrapper<FlashSaleResult> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(FlashSaleResult::getUserId, userId);
        queryWrapper.eq(FlashSaleResult::getActivityId, activityId);

        return getOne(queryWrapper);
    }

    @Override
    public PageResponse<FlashSaleResultVO> listAllResults(PageRequest request) {
        LambdaQueryWrapper<FlashSaleResult> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByDesc(FlashSaleResult::getId);

        Page<FlashSaleResult> page = page(new Page<>(request.getCurrent(), request.getSize()), queryWrapper);

        List<FlashSaleResultVO> records = page.getRecords().stream()
                .map(this::toFlashSaleResultVO)
                .toList();

        return new PageResponse<>(
                page.getTotal(),
                page.getCurrent(),
                page.getSize(),
                records
        );
    }

    @Override
    public void markSuccess(Long userId, Long activityId, Long orderId) {
        FlashSaleResult result = getByUserAndActivity(userId, activityId);
        if (result == null) {
            return;
        }

        result.setStatus(SeckillResultStatus.SUCCESS.getCode());
        result.setMessage(SeckillResultStatus.SUCCESS.getText());
        result.setOrderId(orderId);

        updateById(result);
        log.info("秒杀结果标记成功，resultId={}, userId={}, activityId={}, orderId={}",
                result.getId(), userId, activityId, orderId);
    }

    @Override
    public void markFailed(Long userId, Long activityId, String message) {
        FlashSaleResult result = getByUserAndActivity(userId, activityId);
        if (result == null) {
            return;
        }

        result.setStatus(SeckillResultStatus.FAILED.getCode());
        result.setMessage(message);

        updateById(result);
        log.info("秒杀结果标记失败，resultId={}, userId={}, activityId={}, message={}",
                result.getId(), userId, activityId, message);
    }

    private FlashSaleResultVO toFlashSaleResultVO(FlashSaleResult result) {
        return new FlashSaleResultVO(
                IdUtil.toString(result.getId()),
                IdUtil.toString(result.getUserId()),
                IdUtil.toString(result.getActivityId()),
                result.getStatus(),
                SeckillResultStatus.getTextByCode(result.getStatus()),
                result.getMessage(),
                IdUtil.toString(result.getOrderId()),
                result.getCreatedAt(),
                result.getUpdatedAt()
        );
    }
}
