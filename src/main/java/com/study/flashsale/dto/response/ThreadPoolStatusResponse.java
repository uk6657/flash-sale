package com.study.flashsale.dto.response;

public record ThreadPoolStatusResponse(
        Integer corePoolSize,
        Integer maximumPoolSize,
        Integer poolSize,
        Integer activeCount,
        Integer queueSize,
        Integer queueRemainingCapacity,
        Long completedTaskCount,
        Long taskCount
) {
}
