package com.foodrecommend.letmecook.common.exception;

import com.foodrecommend.letmecook.common.ResultCode;

public class BadRequestException extends ApiException {

    public BadRequestException(String message) {
        super(ResultCode.BAD_REQUEST, message);
    }

    public BadRequestException(ResultCode resultCode) {
        super(resultCode);
    }

    public BadRequestException(ResultCode resultCode, String message) {
        super(resultCode, message);
    }
}
