package com.study.flashsale.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MqMessageVO {

    private String id;

    private String messageId;

    private String messageType;

    private String exchangeName;

    private String routingKey;

    private String messageBody;

    private Integer status;

    private String statusText;

    private Integer retryCount;

    private String failReason;

    private LocalDateTime nextRetryTime;

    private LocalDateTime sentAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
