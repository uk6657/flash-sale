package com.study.flashsale.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("mq_consume_log")
public class MqConsumeLog {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String messageId;

    private String queueName;

    private String messageType;

    private Long orderId;

    private Integer status;

    private LocalDateTime consumeTime;

    private LocalDateTime createdAt;
}
