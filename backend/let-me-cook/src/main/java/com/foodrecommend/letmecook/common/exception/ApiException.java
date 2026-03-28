package com.foodrecommend.letmecook.common.exception;

import com.foodrecommend.letmecook.common.ResultCode;

public class ApiException extends RuntimeException {

    private final int code;

    public ApiException(int code, String message) {
        super(message);
        this.code = code;
    }

    public ApiException(ResultCode resultCode) {
        this(resultCode.getCode(), resultCode.getMessage());
    }

    public ApiException(ResultCode resultCode, String message) {
        this(resultCode.getCode(), message);
    }

    public int getCode() {
        return code;
    }
}
