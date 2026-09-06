package com.study.flashsale.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SeckillResponse {

    private Integer status;

    private String statusText;

    private String message;
}
