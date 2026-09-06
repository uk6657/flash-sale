package com.study.flashsale.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("flash_sale_result")
public class FlashSaleResult {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long userId;

    private Long activityId;

    private Integer status;

    private String message;

    private Long orderId;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
