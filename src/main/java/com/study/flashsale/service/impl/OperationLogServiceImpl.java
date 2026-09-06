package com.study.flashsale.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.study.flashsale.common.PageResponse;
import com.study.flashsale.dto.request.PageRequest;
import com.study.flashsale.entity.OperationLog;
import com.study.flashsale.mapper.OperationLogMapper;
import com.study.flashsale.service.OperationLogService;
import com.study.flashsale.util.IdUtil;
import com.study.flashsale.vo.OperationLogVO;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OperationLogServiceImpl extends ServiceImpl<OperationLogMapper, OperationLog> implements OperationLogService {

    @Override
    public void record(OperationLog operationLog) {
        save(operationLog);
    }

    @Override
    public PageResponse<OperationLogVO> listLogs(PageRequest request) {
        LambdaQueryWrapper<OperationLog> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByDesc(OperationLog::getId);

        Page<OperationLog> page = page(new Page<>(request.getCurrent(), request.getSize()), queryWrapper);

        List<OperationLogVO> records = page.getRecords().stream()
                .map(this::toOperationLogVO)
                .toList();

        return new PageResponse<>(
                page.getTotal(),
                page.getCurrent(),
                page.getSize(),
                records
        );
    }

    private OperationLogVO toOperationLogVO(OperationLog operationLog) {
        return new OperationLogVO(
                IdUtil.toString(operationLog.getId()),
                IdUtil.toString(operationLog.getUserId()),
                operationLog.getOperation(),
                operationLog.getRequestUri(),
                operationLog.getRequestMethod(),
                operationLog.getIp(),
                operationLog.getTraceId(),
                operationLog.getCreatedAt()
        );
    }
}
