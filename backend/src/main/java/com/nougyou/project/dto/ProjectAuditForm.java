package com.nougyou.project.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ProjectAuditForm {

    /** 审核结果：1通过 2驳回 */
    @NotNull(message = "审核结果不能为空")
    private Integer auditStatus;

    @Size(max = 255, message = "审核意见不能超过 255 个字符")
    private String auditRemark;
}
