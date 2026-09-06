package com.study.flashsale.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderVO {

    private String id;

    private String orderNo;

    private String activityId;

    private String productId;

    private BigDecimal price;

    private Integer status;

    private String statusText;

    private LocalDateTime createdAt;
}
