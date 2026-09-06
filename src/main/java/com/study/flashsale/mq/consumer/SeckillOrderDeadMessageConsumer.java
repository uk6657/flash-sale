package com.study.flashsale.mq.consumer;

import com.rabbitmq.client.Channel;
import com.study.flashsale.common.RabbitMqConstants;
import com.study.flashsale.dto.message.SeckillOrderMessage;
import com.study.flashsale.entity.MqDeadMessage;
import com.study.flashsale.enums.MqDeadMessageStatus;
import com.study.flashsale.service.MqDeadMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@Slf4j
@RequiredArgsConstructor
public class SeckillOrderDeadMessageConsumer {

    private static final String RETRY_COUNT_HEADER = "retryCount";

    private final MqDeadMessageService mqDeadMessageService;

    private final ObjectMapper objectMapper;

    @RabbitListener(queues = RabbitMqConstants.FLASH_SALE_ORDER_DEAD_QUEUE)
    public void consumeDeadMessage(Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        String messageBody = new String(message.getBody(), StandardCharsets.UTF_8);

        try {
            MqDeadMessage deadMessage = new MqDeadMessage();
            deadMessage.setMessageId(parseMessageId(messageBody));
            deadMessage.setQueueName(RabbitMqConstants.FLASH_SALE_ORDER_DEAD_QUEUE);
            deadMessage.setExchangeName(valueOrEmpty(message.getMessageProperties().getReceivedExchange()));
            deadMessage.setRoutingKey(valueOrEmpty(message.getMessageProperties().getReceivedRoutingKey()));
            deadMessage.setMessageBody(messageBody);
            deadMessage.setFailReason(parseDeathReason(message));
            deadMessage.setRetryCount(getRetryCount(message));
            deadMessage.setStatus(MqDeadMessageStatus.UNHANDLED.getCode());

            mqDeadMessageService.record(deadMessage);

            channel.basicAck(deliveryTag, false);
            log.warn("Dead seckill order message saved, deadMessageId={}, messageId={}, retryCount={}",
                    deadMessage.getId(), deadMessage.getMessageId(), deadMessage.getRetryCount());
        } catch (Exception e) {
            log.error("Dead seckill order message save failed, messageBody={}", messageBody, e);
            channel.basicNack(deliveryTag, false, true);
        }
    }

    private String parseMessageId(String messageBody) {
        try {
            SeckillOrderMessage orderMessage = objectMapper.readValue(messageBody, SeckillOrderMessage.class);
            return valueOrEmpty(orderMessage.getMessageId());
        } catch (Exception e) {
            return "";
        }
    }

    private String parseDeathReason(Message message) {
        Object xDeath = message.getMessageProperties().getHeaders().get("x-death");
        if (xDeath == null) {
            return "消费失败进入死信队列";
        }
        return "消费失败进入死信队列：" + xDeath;
    }

    private int getRetryCount(Message message) {
        Object retryCount = message.getMessageProperties().getHeaders().get(RETRY_COUNT_HEADER);
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

    private String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }
}
