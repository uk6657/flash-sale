package com.study.flashsale.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScheduledTaskLockStatusVO {

    private String taskName;

    private String lockKey;

    private Boolean locked;

    private Long ttlMs;

    private Long fixedDelayMs;

    private String description;
}
