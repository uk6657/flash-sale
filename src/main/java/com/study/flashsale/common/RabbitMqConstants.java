package com.study.flashsale.common;

public class RabbitMqConstants {

    private RabbitMqConstants() {
    }

    public static final String FLASH_SALE_EXCHANGE = "flash.sale.exchange";

    public static final String FLASH_SALE_ORDER_QUEUE = "flash.sale.order.queue";

    public static final String FLASH_SALE_ORDER_ROUTING_KEY = "flash.sale.order.create";

    public static final String FLASH_SALE_ORDER_RETRY_QUEUE = "flash.sale.order.retry.queue";

    public static final String FLASH_SALE_ORDER_RETRY_ROUTING_KEY = "flash.sale.order.retry";

    public static final String FLASH_SALE_DEAD_EXCHANGE = "flash.sale.dead.exchange";

    public static final String FLASH_SALE_ORDER_DEAD_QUEUE = "flash.sale.order.dead.queue";

    public static final String FLASH_SALE_ORDER_DEAD_ROUTING_KEY = "flash.sale.order.dead";

    public static final String RETRY_COUNT_HEADER = "retryCount";

    public static final String MESSAGE_ID_HEADER = "messageId";
}
