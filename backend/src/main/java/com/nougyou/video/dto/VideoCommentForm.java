package com.nougyou.video.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 发表评论 / 回复（API-082）。
 */
@Data
public class VideoCommentForm {

    @NotBlank(message = "评论内容不能为空")
    @Size(max = 200, message = "评论内容最长 200 字")
    private String content;

    /** 回复时传被回复的评论 ID，一级评论传 0 */
    private Long parentId = 0L;
}
