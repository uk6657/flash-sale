package com.study.flashsale.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OperationLogVO {

    private String id;

    private String userId;

    private String operation;

    private String requestUri;

    private String requestMethod;

    private String ip;

    private String traceId;

    private LocalDateTime createdAt;
}
