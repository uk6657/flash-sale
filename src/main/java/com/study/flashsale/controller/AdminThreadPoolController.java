package com.study.flashsale.controller;

import com.study.flashsale.annotation.LogOperation;
import com.study.flashsale.common.Result;
import com.study.flashsale.dto.response.ThreadPoolStatusResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.ThreadPoolExecutor;

@Tag(name = "管理端-线程池")
@RestController
@RequestMapping("/api/admin/thread-pools")
public class AdminThreadPoolController {

    private final ThreadPoolTaskExecutor orderTaskExecutor;

    public AdminThreadPoolController(
            @Qualifier("orderTaskExecutor") ThreadPoolTaskExecutor orderTaskExecutor
    ) {
        this.orderTaskExecutor = orderTaskExecutor;
    }

    @Operation(summary = "查询订单线程池状态")
    @LogOperation("查询订单线程池状态")
    @GetMapping("/order")
    public Result<ThreadPoolStatusResponse> getOrderThreadPoolStatus() {
        ThreadPoolExecutor executor = orderTaskExecutor.getThreadPoolExecutor();

        ThreadPoolStatusResponse response = new ThreadPoolStatusResponse(
                executor.getCorePoolSize(),
                executor.getMaximumPoolSize(),
                executor.getPoolSize(),
                executor.getActiveCount(),
                executor.getQueue().size(),
                executor.getQueue().remainingCapacity(),
                executor.getCompletedTaskCount(),
                executor.getTaskCount()
        );

        return Result.success(response);
    }
}
