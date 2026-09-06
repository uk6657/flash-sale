package com.study.flashsale.mq.producer;

import com.study.flashsale.common.RabbitMqConstants;
import com.study.flashsale.dto.message.SeckillOrderMessage;
import com.study.flashsale.entity.MqDeadMessage;
import com.study.flashsale.entity.MqMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class SeckillOrderProducer {

    private final RabbitTemplate rabbitTemplate;

    public void sendCreateOrderMessage(MqMessage mqMessage) {
        sendMessage(
                mqMessage.getMessageId(),
                mqMessage.getMessageBody(),
                mqMessage.getRetryCount(),
                RabbitMqConstants.FLASH_SALE_ORDER_ROUTING_KEY,
                "Seckill order message sent"
        );
    }

    public void sendRetryOrderMessage(SeckillOrderMessage message, String messageJson, int retryCount) {
        sendMessage(
                message.getMessageId(),
                messageJson,
                retryCount,
                RabbitMqConstants.FLASH_SALE_ORDER_RETRY_ROUTING_KEY,
                "Seckill order retry message sent"
        );
    }

    public void resendDeadOrderMessage(MqDeadMessage deadMessage) {
        sendMessage(
                deadMessage.getMessageId(),
                deadMessage.getMessageBody(),
                0,
                RabbitMqConstants.FLASH_SALE_ORDER_ROUTING_KEY,
                "Dead seckill order message resent"
        );
    }

    private void sendMessage(String messageId, String messageBody, int retryCount, String routingKey, String logMessage) {
        MessagePostProcessor messagePostProcessor = rabbitMessage -> {
            rabbitMessage.getMessageProperties().setHeader(RabbitMqConstants.MESSAGE_ID_HEADER, messageId);
            rabbitMessage.getMessageProperties().setHeader(RabbitMqConstants.RETRY_COUNT_HEADER, retryCount);
            return rabbitMessage;
        };

        rabbitTemplate.convertAndSend(
                RabbitMqConstants.FLASH_SALE_EXCHANGE,
                routingKey,
                messageBody,
                messagePostProcessor,
                new CorrelationData(messageId)
        );

        log.info("{}, messageId={}, routingKey={}, retryCount={}", logMessage, messageId, routingKey, retryCount);
    }
}
