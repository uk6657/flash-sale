package com.study.flashsale.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ActivityTimeStatus {

    NOT_STARTED(0, "未开始"),
    IN_PROGRESS(1, "进行中"),
    ENDED(2, "已结束");

    private final Integer code;
    private final String text;

    public static String getTextByCode(Integer code) {
        for (ActivityTimeStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status.getText();
            }
        }
        return "未知";
    }
}
