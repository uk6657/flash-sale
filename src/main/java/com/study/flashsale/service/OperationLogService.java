package com.study.flashsale.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.study.flashsale.common.PageResponse;
import com.study.flashsale.dto.request.PageRequest;
import com.study.flashsale.entity.OperationLog;
import com.study.flashsale.vo.OperationLogVO;

public interface OperationLogService extends IService<OperationLog> {

    void record(OperationLog operationLog);

    PageResponse<OperationLogVO> listLogs(PageRequest request);
}
