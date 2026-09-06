package com.study.flashsale.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MqConsumeLogVO {

    private String id;

    private String messageId;

    private String queueName;

    private String messageType;

    private String orderId;

    private Integer status;

    private LocalDateTime consumeTime;

    private LocalDateTime createdAt;
}
