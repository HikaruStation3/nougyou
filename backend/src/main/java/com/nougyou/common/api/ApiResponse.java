package com.nougyou.common.api;

/**
 * 统一响应体：{ code, msg, data }。
 */
public record ApiResponse<T>(String code, String msg, T data) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(ResultCode.SUCCESS, "一切ok", data);
    }

    public static ApiResponse<Void> ok() {
        return new ApiResponse<>(ResultCode.SUCCESS, "一切ok", null);
    }

    public static <T> ApiResponse<T> fail(String code, String msg) {
        return new ApiResponse<>(code, msg, null);
    }
}
