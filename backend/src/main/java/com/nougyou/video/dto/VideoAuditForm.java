package com.nougyou.video.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 短视频审核（API-086）。
 */
@Data
public class VideoAuditForm {

    /** 1 通过 / 2 驳回 */
    @NotNull(message = "请选择审核结果")
    private Integer auditStatus;

    @Size(max = 200, message = "审核意见最长 200 字")
    private String auditRemark;
}
