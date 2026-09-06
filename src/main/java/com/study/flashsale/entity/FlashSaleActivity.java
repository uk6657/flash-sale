package com.study.flashsale.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("flash_sale_activity")
public class FlashSaleActivity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long productId;

    private BigDecimal salePrice;

    private Integer saleStock;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Integer status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
