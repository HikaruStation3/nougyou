package com.nougyou.entrust.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 受托方确认 / 拒绝委托入参（API-091）。
 */
@Data
public class EntrustConfirmForm {

    @NotNull(message = "请选择是否接受委托")
    private Boolean agree;

    @Size(max = 200, message = "说明最长 200 个字符")
    private String remark;
}
