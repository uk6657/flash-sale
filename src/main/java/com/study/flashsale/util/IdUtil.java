package com.study.flashsale.util;

import com.study.flashsale.common.ErrorCode;
import com.study.flashsale.exception.BusinessException;

public class IdUtil {

    private IdUtil() {
    }

    public static Long parseId(String id) {
        try {
            return Long.valueOf(id);
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "ID格式不正确");
        }
    }

    public static String toString(Long id) {
        return id == null ? null : String.valueOf(id);
    }
}
