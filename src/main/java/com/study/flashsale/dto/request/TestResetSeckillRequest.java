package com.study.flashsale.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TestResetSeckillRequest {

    @NotNull(message = "重置库存不能为空")
    @Min(value = 0, message = "重置库存不能小于0")
    private Integer saleStock;
}
