package com.study.flashsale.mq.consumer;

import com.rabbitmq.client.Channel;
import com.study.flashsale.common.RabbitMqConstants;
import com.study.flashsale.common.RedisKeyConstants;
import com.study.flashsale.config.FlashSaleProperties;
import com.study.flashsale.dto.message.SeckillOrderMessage;
import com.study.flashsale.infrastructure.redis.RedisService;
import com.study.flashsale.mq.producer.SeckillOrderProducer;
import com.study.flashsale.service.FlashSaleResultService;
import com.study.flashsale.service.MqConsumeLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

@Component
@Slf4j
@RequiredArgsConstructor
public class SeckillOrderConsumer {

    private final ObjectMapper objectMapper;

    private final FlashSaleResultService flashSaleResultService;

    private final MqConsumeLogService mqConsumeLogService;

    private final RedisService redisService;

    private final SeckillOrderProducer seckillOrderProducer;

    private final FlashSaleProperties flashSaleProperties;

    @RabbitListener(queues = RabbitMqConstants.FLASH_SALE_ORDER_QUEUE)
    public void consume(String messageJson, Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        SeckillOrderMessage orderMessage = null;

        try {
            orderMessage = objectMapper.readValue(messageJson, SeckillOrderMessage.class);
            int retryCount = getRetryCount(message);

            log.info("Seckill order message received, messageId={}, userId={}, activityId={}, retryCount={}",
                    orderMessage.getMessageId(),
                    orderMessage.getUserId(),
                    orderMessage.getActivityId(),
                    retryCount);

            if (mqConsumeLogService.hasConsumed(orderMessage.getMessageId())) {
                channel.basicAck(deliveryTag, false);
                log.info("Repeated MQ message acknowledged, messageId={}", orderMessage.getMessageId());
                return;
            }

            Long orderId = mqConsumeLogService.consumeSeckillOrderMessage(
                    orderMessage,
                    RabbitMqConstants.FLASH_SALE_ORDER_QUEUE
            );

            channel.basicAck(deliveryTag, false);
            log.info("Seckill order message consumed, messageId={}, orderId={}",
                    orderMessage.getMessageId(), orderId);
        } catch (Exception e) {
            handleConsumeFailed(messageJson, message, channel, deliveryTag, orderMessage, e);
        }
    }

    private void handleConsumeFailed(
            String messageJson,
            Message message,
            Channel channel,
            long deliveryTag,
            SeckillOrderMessage orderMessage,
            Exception cause
    ) throws IOException {
        int retryCount = getRetryCount(message);
        int maxRetryCount = flashSaleProperties.getMq().getOrderMaxRetryCount();
        log.error("Seckill order message consume failed, retryCount={}, maxRetryCount={}, message={}",
                retryCount, maxRetryCount, messageJson, cause);

        if (orderMessage != null && retryCount < maxRetryCount) {
            seckillOrderProducer.sendRetryOrderMessage(orderMessage, messageJson, retryCount + 1);
            channel.basicAck(deliveryTag, false);
            log.warn("Seckill order message sent to retry queue, messageId={}, nextRetryCount={}, retryDelayMs={}",
                    orderMessage.getMessageId(),
                    retryCount + 1,
                    flashSaleProperties.getMq().getOrderRetryDelayMs());
            return;
        }

        if (orderMessage != null) {
            handleOrderFailed(orderMessage, "下单失败：" + cause.getMessage());
        }

        channel.basicReject(deliveryTag, false);
        log.warn("Seckill order message rejected to dead queue, retryCount={}, message={}", retryCount, messageJson);
    }

    private int getRetryCount(Message message) {
        Object retryCount = message.getMessageProperties().getHeaders().get(RabbitMqConstants.RETRY_COUNT_HEADER);
        if (retryCount instanceof Integer value) {
            return value;
        }
        if (retryCount instanceof Number value) {
            return value.intValue();
        }
        if (retryCount instanceof String value) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }

    private void handleOrderFailed(SeckillOrderMessage orderMessage, String failMessage) {
        Long userId = orderMessage.getUserId();
        Long activityId = orderMessage.getActivityId();

        flashSaleResultService.markFailed(userId, activityId, failMessage);
        redisService.increment(RedisKeyConstants.flashSaleStock(activityId));
        redisService.delete(RedisKeyConstants.flashSaleUser(activityId, userId));

        log.info("Seckill order failure compensated, messageId={}, userId={}, activityId={}",
                orderMessage.getMessageId(), userId, activityId);
    }
}
