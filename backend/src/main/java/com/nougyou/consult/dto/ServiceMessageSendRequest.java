package com.nougyou.consult.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 发送消息入参（API-070）。
 */
@Data
public class ServiceMessageSendRequest {

    /** 1 文本 2 图片 */
    @NotNull(message = "内容类型不能为空")
    @Min(value = 1, message = "内容类型只能是 1（文本）或 2（图片）")
    @Max(value = 2, message = "内容类型只能是 1（文本）或 2（图片）")
    private Integer contentType;

    /** 文本最长 500 字；图片传上传后的地址 */
    @NotBlank(message = "消息内容不能为空")
    @Size(max = 500, message = "消息内容不能超过 500 个字符")
    private String content;
}
