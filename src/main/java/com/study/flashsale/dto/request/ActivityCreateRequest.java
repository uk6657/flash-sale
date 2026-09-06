package com.study.flashsale.dto.request;

import com.study.flashsale.enums.CommonStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ActivityCreateRequest {

    @NotBlank(message = "商品ID不能为空")
    private String productId;

    @NotNull(message = "秒杀价格不能为空")
    @DecimalMin(value = "0.01", message = "秒杀价格必须大于0")
    private BigDecimal salePrice;

    @NotNull(message = "秒杀库存不能为空")
    @Min(value = 1, message = "秒杀库存最少为1")
    private Integer saleStock;

    @NotNull(message = "活动开始时间不能为空")
    private LocalDateTime startTime;

    @NotNull(message = "活动结束时间不能为空")
    private LocalDateTime endTime;

    @Min(value = 0, message = "活动状态只能是0或1")
    @Max(value = 1, message = "活动状态只能是0或1")
    private Integer status = CommonStatus.ENABLED.getCode();
}
