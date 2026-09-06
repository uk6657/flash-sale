package com.study.flashsale.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum MqMessageStatus {

    PENDING(0, "待发送"),
    SENT(1, "已发送"),
    SEND_FAILED(2, "发送失败"),
    FINAL_FAILED(3, "最终失败");

    private final Integer code;

    private final String text;

    public static String getTextByCode(Integer code) {
        if (code == null) {
            return "";
        }

        for (MqMessageStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status.getText();
            }
        }

        return "";
    }
}
