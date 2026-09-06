package com.study.flashsale.task;

import com.study.flashsale.common.RedisKeyConstants;
import com.study.flashsale.config.FlashSaleProperties;
import com.study.flashsale.entity.MqMessage;
import com.study.flashsale.mq.producer.SeckillOrderProducer;
import com.study.flashsale.service.MqMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class MqMessageRetryTask {

    private final MqMessageService mqMessageService;

    private final SeckillOrderProducer seckillOrderProducer;

    private final FlashSaleProperties flashSaleProperties;

    private final ScheduledTaskLockExecutor scheduledTaskLockExecutor;

    @Scheduled(fixedDelayString = "${flash-sale.mq.send-retry-fixed-delay-ms}")
    public void retrySendFailedMessages() {
        scheduledTaskLockExecutor.executeWithLock(
                RedisKeyConstants.LOCK_TASK_MQ_MESSAGE_RETRY,
                Duration.ofMillis(flashSaleProperties.getTask().getScheduledLockTtlMs()),
                this::doRetrySendFailedMessages
        );
    }

    private void doRetrySendFailedMessages() {
        handleFinalFailedMessages();
        retryMessages();
    }

    private void retryMessages() {
        List<MqMessage> messages = mqMessageService.listRetryableMessages(
                flashSaleProperties.getMq().getSendRetryBatchSize()
        );
        if (messages.isEmpty()) {
            return;
        }

        log.info("Start retrying local MQ messages, count={}", messages.size());
        for (MqMessage message : messages) {
            try {
                MqMessage retryMessage = mqMessageService.markRetrying(message.getId());
                if (retryMessage == null) {
                    continue;
                }

                seckillOrderProducer.sendCreateOrderMessage(retryMessage);
                log.info("Local MQ message retry sent, messageId={}, retryCount={}",
                        retryMessage.getMessageId(), retryMessage.getRetryCount());
            } catch (Exception e) {
                mqMessageService.markSendFailed(message.getMessageId(), "Retry send exception: " + e.getMessage());
                log.error("Local MQ message retry failed, messageId={}", message.getMessageId(), e);
            }
        }
    }

    private void handleFinalFailedMessages() {
        List<MqMessage> messages = mqMessageService.listFinalFailedMessages(
                flashSaleProperties.getMq().getSendRetryBatchSize()
        );
        if (messages.isEmpty()) {
            return;
        }

        log.warn("Start handling final failed local MQ messages, count={}", messages.size());
        for (MqMessage message : messages) {
            try {
                mqMessageService.markFinalFailedAndCompensate(
                        message,
                        "MQ message send final failed after max retries"
                );
            } catch (Exception e) {
                log.error("Handle final failed local MQ message failed, messageId={}", message.getMessageId(), e);
            }
        }
    }
}
