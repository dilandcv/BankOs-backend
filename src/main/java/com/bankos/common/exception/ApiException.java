package com.bankos.common.exception;

public class ApiException extends RuntimeException {

    private final ErrorCode errorCode;
    private final Object data;

    public ApiException(ErrorCode errorCode) {
        super(errorCode.defaultMessage());
        this.errorCode = errorCode;
        this.data = null;
    }

    public ApiException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.data = null;
    }

    public ApiException(ErrorCode errorCode, String message, Object data) {
        super(message);
        this.errorCode = errorCode;
        this.data = data;
    }

    public ErrorCode errorCode() {
        return errorCode;
    }

    public Object data() {
        return data;
    }
}
