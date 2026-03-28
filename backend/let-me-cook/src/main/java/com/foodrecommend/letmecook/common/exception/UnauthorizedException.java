package com.foodrecommend.letmecook.common.exception;

import com.foodrecommend.letmecook.common.ResultCode;

public class UnauthorizedException extends ApiException {

    public UnauthorizedException(String message) {
        super(ResultCode.UNAUTHORIZED, message);
    }

    public UnauthorizedException(ResultCode resultCode) {
        super(resultCode);
    }

    public UnauthorizedException(ResultCode resultCode, String message) {
        super(resultCode, message);
    }
}
