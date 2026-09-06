package com.study.flashsale.common;

public class RedisKeyConstants {

    private RedisKeyConstants() {
    }

    public static final String PRODUCT_DETAIL = "product:detail:";

    public static final String FLASH_SALE_STOCK = "flashsale:stock:";

    public static final String FLASH_SALE_USER = "flashsale:user:";

    public static final String RATE_LIMIT_SECKILL = "rate:seckill:";

    public static final String ORDER_TIMEOUT_QUEUE = "order:timeout:queue";

    public static final String ACTIVITY_DETAIL = "activity:detail:";

    public static final String LOCK_PRODUCT_DETAIL = "lock:product:detail:";

    public static final String LOCK_ACTIVITY_DETAIL = "lock:activity:detail:";

    public static final String DUPLICATE_SUBMIT = "duplicate:submit:";

    public static final String LOCK_TASK_ORDER_TIMEOUT = "lock:task:order-timeout";

    public static final String LOCK_TASK_STOCK_COMPENSATION = "lock:task:stock-compensation";

    public static final String LOCK_TASK_MQ_MESSAGE_RETRY = "lock:task:mq-message-retry";

    public static final String EMPTY_VALUE = "__EMPTY__";

    public static String productDetail(Long productId) {
        return PRODUCT_DETAIL + productId;
    }

    public static String flashSaleStock(Long activityId) {
        return FLASH_SALE_STOCK + activityId;
    }

    public static String flashSaleUser(Long activityId, Long userId) {
        return FLASH_SALE_USER + activityId + ":" + userId;
    }

    public static String flashSaleUserPattern(Long activityId) {
        return FLASH_SALE_USER + activityId + ":*";
    }

    public static String rateLimitSeckill(Long activityId, Long userId) {
        return RATE_LIMIT_SECKILL + activityId + ":" + userId;
    }

    public static String rateLimitSeckillPattern(Long activityId) {
        return RATE_LIMIT_SECKILL + activityId + ":*";
    }

    public static String activityDetail(Long activityId) {
        return ACTIVITY_DETAIL + activityId;
    }

    public static String lockProductDetail(Long productId) {
        return LOCK_PRODUCT_DETAIL + productId;
    }

    public static String lockActivityDetail(Long activityId) {
        return LOCK_ACTIVITY_DETAIL + activityId;
    }

    public static String duplicateSubmit(Long userId, String method, String uri, String paramHash) {
        return DUPLICATE_SUBMIT + userId + ":" + method + ":" + uri + ":" + paramHash;
    }

}
