package com.study.flashsale.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("mq_dead_message")
public class MqDeadMessage {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String messageId;

    private String queueName;

    private String exchangeName;

    private String routingKey;

    private String messageBody;

    private String failReason;

    private Integer retryCount;

    private Integer status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
