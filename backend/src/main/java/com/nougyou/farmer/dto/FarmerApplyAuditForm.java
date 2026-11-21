package com.nougyou.farmer.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class FarmerApplyAuditForm {

    @NotNull(message = "审核结果不能为空")
    private Boolean approved;

    @Size(max = 255, message = "审核意见不能超过 255 个字符")
    private String remark;
}
