package com.study.flashsale.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActivityVO {

    private String id;

    private String productId;

    private BigDecimal salePrice;

    private Integer saleStock;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Integer status;

    private String statusText;

    private Integer timeStatus;

    private String timeStatusText;
}
