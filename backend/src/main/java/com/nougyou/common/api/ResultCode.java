package com.nougyou.common.api;

/**
 * 统一响应码，与前端 ApiCodeEnum 保持一致。
 */
public final class ResultCode {

    public static final String SUCCESS = "00000";
    public static final String LOGIN_FAILED = "A0210";
    public static final String CAPTCHA_ERROR = "A0211";
    public static final String ACCESS_TOKEN_INVALID = "A0230";
    public static final String REFRESH_TOKEN_INVALID = "A0231";
    public static final String PERMISSION_DENIED = "A0301";
    public static final String BAD_REQUEST = "A0400";
    public static final String SERVER_ERROR = "B0001";

    private ResultCode() {
    }
}
