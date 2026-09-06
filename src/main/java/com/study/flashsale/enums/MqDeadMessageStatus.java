package com.study.flashsale.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum MqDeadMessageStatus {

    UNHANDLED(0, "未处理"),
    HANDLED(1, "已处理");

    private final Integer code;

    private final String text;

    public static String getTextByCode(Integer code) {
        if (code == null) {
            return "";
        }

        for (MqDeadMessageStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status.getText();
            }
        }

        return "";
    }
}
