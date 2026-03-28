package com.foodrecommend.letmecook.common.exception;

import com.foodrecommend.letmecook.common.ResultCode;

public class NotFoundException extends ApiException {

    public NotFoundException(String message) {
        super(ResultCode.NOT_FOUND, message);
    }

    public NotFoundException(ResultCode resultCode) {
        super(resultCode);
    }

    public NotFoundException(ResultCode resultCode, String message) {
        super(resultCode, message);
    }
}
