package com.study.flashsale.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TestResetSeckillResponse {

    private String activityId;

    private Integer saleStock;

    private Integer deletedOrders;

    private Integer deletedResults;

    private Long deletedRedisKeys;
}
