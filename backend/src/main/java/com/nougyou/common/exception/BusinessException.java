package com.nougyou.common.exception;

import com.nougyou.common.api.ResultCode;
import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    private final String code;

    public BusinessException(String message) {
        this(ResultCode.BAD_REQUEST, message);
    }

    public BusinessException(String code, String message) {
        super(message);
        this.code = code;
    }
}
