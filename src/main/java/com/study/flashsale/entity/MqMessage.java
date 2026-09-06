package com.study.flashsale.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("mq_message")
public class MqMessage {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String messageId;

    private String messageType;

    private String exchangeName;

    private String routingKey;

    private String messageBody;

    private Integer status;

    private Integer retryCount;

    private String failReason;

    private LocalDateTime nextRetryTime;

    private LocalDateTime sentAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
