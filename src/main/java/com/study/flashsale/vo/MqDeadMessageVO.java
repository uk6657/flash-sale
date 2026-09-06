package com.study.flashsale.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MqDeadMessageVO {

    private String id;

    private String messageId;

    private String queueName;

    private String exchangeName;

    private String routingKey;

    private String messageBody;

    private String failReason;

    private Integer retryCount;

    private Integer status;

    private String statusText;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
