package com.nougyou.video.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 短视频评论（API-081 / 082），两级结构：children 为一级评论下的回复。
 */
@Data
@Builder
public class VideoCommentVO {

    private Long id;

    private Long videoId;

    private Long userId;

    private String nickname;

    private String avatar;

    /** 父评论ID，0 表示一级评论 */
    private Long parentId;

    /** 被回复人昵称：回复某条回复时展示「回复 @某某」 */
    private String replyToName;

    private String content;

    private Integer likeCount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /** 二级回复列表 */
    private List<VideoCommentVO> children;

    /** 当前用户是否可删除：本人 / 视频作者 / 管理员 */
    private Boolean canDelete;

    /** 是否本人发表 */
    private Boolean mine;
}
