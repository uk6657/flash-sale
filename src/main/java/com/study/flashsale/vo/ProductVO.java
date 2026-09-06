package com.study.flashsale.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductVO {

    private String id;

    private String name;

    private String description;

    private BigDecimal price;

    private Integer stock;

    private Integer lockStock;

    private Integer availableStock;

    private Integer status;

    private String statusText;
}
