package com.study.flashsale.dto.response;

public record ActivitySummaryResponse(
        String activityId,
        String productId,
        Integer activityStatus,
        String activityStatusText,
        Integer timeStatus,
        String timeStatusText,
        Integer dbSaleStock,
        Long redisStock,
        Long queuedResultCount,
        Long successResultCount,
        Long failedResultCount,
        Long waitPayOrderCount,
        Long paidOrderCount,
        Long canceledOrderCount,
        Long payFailedOrderCount
) {
}
