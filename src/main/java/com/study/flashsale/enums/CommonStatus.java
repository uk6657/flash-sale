package com.study.flashsale.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CommonStatus {

    DISABLED(0, "禁用"),
    ENABLED(1, "启用");

    private final Integer code;
    private final String text;

    public static boolean isValid(Integer code) {
        return DISABLED.code.equals(code) || ENABLED.code.equals(code);
    }

    public static String getTextByCode(Integer code) {
        for (CommonStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status.getText();
            }
        }
        return "未知";
    }
}
