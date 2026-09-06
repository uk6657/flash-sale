package com.study.flashsale.dto.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SeckillOrderMessage {

    private String messageId;

    private Long userId;

    private Long activityId;

    private LocalDateTime createdAt;
}