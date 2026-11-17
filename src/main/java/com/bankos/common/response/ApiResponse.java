package com.bankos.common.response;

import com.bankos.common.exception.ErrorCode;
import com.bankos.common.web.CorrelationIdContext;

public record ApiResponse<T>(
        boolean success,
        String code,
        String message,
        T data,
        String correlationId
) {

    public static <T> ApiResponse<T> success(String code, String message, T data) {
        return new ApiResponse<>(true, code, message, data, CorrelationIdContext.getOrCreate());
    }

    public static <T> ApiResponse<T> error(ErrorCode errorCode) {
        return error(errorCode, errorCode.defaultMessage(), null);
    }

    public static <T> ApiResponse<T> error(ErrorCode errorCode, T data) {
        return error(errorCode, errorCode.defaultMessage(), data);
    }

    public static <T> ApiResponse<T> error(ErrorCode errorCode, String message, T data) {
        return new ApiResponse<>(false, errorCode.name(), message, data, CorrelationIdContext.getOrCreate());
    }
}
