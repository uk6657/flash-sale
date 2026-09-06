package com.study.flashsale.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum SeckillResultStatus {

    QUEUED(0, "排队中"),
    SUCCESS(1, "秒杀成功"),
    FAILED(2, "秒杀失败");

    private final Integer code;
    private final String text;

    public static String getTextByCode(Integer code) {
        for (SeckillResultStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status.getText();
            }
        }
        return "未知";
    }
}
