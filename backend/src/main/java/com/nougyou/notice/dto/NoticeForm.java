package com.nougyou.notice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * 新增/编辑通知的提交结构。
 */
@Data
public class NoticeForm {

    private Long id;

    @NotBlank(message = "通知标题不能为空")
    private String title;

    @NotBlank(message = "通知内容不能为空")
    private String content;

    /** 1 通知 / 2 公告 / 3 提醒 */
    private Integer type;

    /** L 普通 / M 重要 / H 紧急 */
    private String level;

    /** 1 全体 / 2 指定用户 */
    private Integer targetType;

    /** targetType = 2 时生效 */
    private List<Long> targetUserIds;
}
