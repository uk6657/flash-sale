package com.study.flashsale.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum OrderStatus {

    WAIT_PAY(0, "待支付"),
    PAID(1, "已支付"),
    CANCELED(2, "已取消"),
    PAY_FAILED(3, "支付失败");

    private final Integer code;
    private final String text;

    public static boolean isWaitPay(Integer status) {
        return WAIT_PAY.getCode().equals(status);
    }

    public static boolean isPaid(Integer status) {
        return PAID.getCode().equals(status);
    }

    public static boolean isCanceled(Integer status) {
        return CANCELED.getCode().equals(status);
    }

    public static boolean isPayFailed(Integer status) {
        return PAY_FAILED.getCode().equals(status);
    }

    public static boolean canPay(Integer status) {
        return isWaitPay(status);
    }

    public static boolean cannotPay(Integer status) {
        return !canPay(status);
    }

    public static boolean canCancel(Integer status) {
        return isWaitPay(status);
    }

    public static boolean cannotCancel(Integer status) {
        return !canCancel(status);
    }

    public static boolean canFailPay(Integer status) {
        return isWaitPay(status);
    }

    public static boolean cannotFailPay(Integer status) {
        return !canFailPay(status);
    }

    public static String getTextByCode(Integer code) {
        for (OrderStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status.getText();
            }
        }
        return "未知";
    }
}
