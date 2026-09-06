package com.study.flashsale.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FlashSaleResultVO {

    private String id;

    private String userId;

    private String activityId;

    private Integer status;

    private String statusText;

    private String message;

    private String orderId;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
