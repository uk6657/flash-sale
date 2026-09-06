package com.study.flashsale.config;

import com.study.flashsale.common.RabbitMqConstants;
import com.study.flashsale.service.MqMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class RabbitMqConfig {

    private final FlashSaleProperties flashSaleProperties;

    @Bean
    public DirectExchange flashSaleExchange() {
        return new DirectExchange(RabbitMqConstants.FLASH_SALE_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange flashSaleDeadExchange() {
        return new DirectExchange(RabbitMqConstants.FLASH_SALE_DEAD_EXCHANGE, true, false);
    }

    @Bean
    public Queue flashSaleOrderQueue() {
        return QueueBuilder.durable(RabbitMqConstants.FLASH_SALE_ORDER_QUEUE)
                .deadLetterExchange(RabbitMqConstants.FLASH_SALE_DEAD_EXCHANGE)
                .deadLetterRoutingKey(RabbitMqConstants.FLASH_SALE_ORDER_DEAD_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue flashSaleOrderDeadQueue() {
        return QueueBuilder.durable(RabbitMqConstants.FLASH_SALE_ORDER_DEAD_QUEUE).build();
    }

    @Bean
    public Queue flashSaleOrderRetryQueue() {
        return QueueBuilder.durable(RabbitMqConstants.FLASH_SALE_ORDER_RETRY_QUEUE)
                .ttl(flashSaleProperties.getMq().getOrderRetryDelayMs())
                .deadLetterExchange(RabbitMqConstants.FLASH_SALE_EXCHANGE)
                .deadLetterRoutingKey(RabbitMqConstants.FLASH_SALE_ORDER_ROUTING_KEY)
                .build();
    }

    @Bean
    public Binding flashSaleOrderBinding() {
        return BindingBuilder.bind(flashSaleOrderQueue())
                .to(flashSaleExchange())
                .with(RabbitMqConstants.FLASH_SALE_ORDER_ROUTING_KEY);
    }

    @Bean
    public Binding flashSaleOrderDeadBinding() {
        return BindingBuilder.bind(flashSaleOrderDeadQueue())
                .to(flashSaleDeadExchange())
                .with(RabbitMqConstants.FLASH_SALE_ORDER_DEAD_ROUTING_KEY);
    }

    @Bean
    public Binding flashSaleOrderRetryBinding() {
        return BindingBuilder.bind(flashSaleOrderRetryQueue())
                .to(flashSaleExchange())
                .with(RabbitMqConstants.FLASH_SALE_ORDER_RETRY_ROUTING_KEY);
    }

    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        RabbitAdmin rabbitAdmin = new RabbitAdmin(connectionFactory);
        rabbitAdmin.setAutoStartup(true);
        return rabbitAdmin;
    }

    @Bean
    public SmartInitializingSingleton rabbitTemplateInitializer(
            RabbitTemplate rabbitTemplate,
            MqMessageService mqMessageService
    ) {
        return () -> {
            rabbitTemplate.setMandatory(true);
            rabbitTemplate.setConfirmCallback((CorrelationData correlationData, boolean ack, String cause) -> {
                if (correlationData == null) {
                    return;
                }

                String messageId = correlationData.getId();
                if (messageId.isBlank()) {
                    return;
                }

                if (ack) {
                    mqMessageService.markSent(messageId);
                    log.info("RabbitMQ confirm success, messageId={}", messageId);
                    return;
                }

                mqMessageService.markSendFailed(messageId, "Confirm failed: " + cause);
                log.error("RabbitMQ confirm failed, messageId={}, cause={}", messageId, cause);
            });

            rabbitTemplate.setReturnsCallback((ReturnedMessage returned) -> {
                Object messageIdHeader = returned.getMessage()
                        .getMessageProperties()
                        .getHeaders()
                        .get(RabbitMqConstants.MESSAGE_ID_HEADER);
                if (messageIdHeader == null) {
                    log.error("RabbitMQ return failed without messageId, replyCode={}, replyText={}, exchange={}, routingKey={}",
                            returned.getReplyCode(),
                            returned.getReplyText(),
                            returned.getExchange(),
                            returned.getRoutingKey());
                    return;
                }

                String messageId = String.valueOf(messageIdHeader);
                String failReason = "Return failed: " + returned.getReplyText()
                        + ", exchange=" + returned.getExchange()
                        + ", routingKey=" + returned.getRoutingKey();
                mqMessageService.markSendFailed(messageId, failReason);
                log.error("RabbitMQ return failed, messageId={}, replyCode={}, replyText={}, exchange={}, routingKey={}",
                        messageId,
                        returned.getReplyCode(),
                        returned.getReplyText(),
                        returned.getExchange(),
                        returned.getRoutingKey());
            });
        };
    }
}
