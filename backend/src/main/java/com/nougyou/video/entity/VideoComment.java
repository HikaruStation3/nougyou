package com.nougyou.video.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 短视频评论，parent_id = 0 为一级评论（FR-012）。
 */
@Data
@TableName("video_comment")
public class VideoComment {

    /** 评论状态：已删除 */
    public static final int STATUS_DELETED = 0;
    /** 评论状态：正常 */
    public static final int STATUS_NORMAL = 1;
    /** 一级评论的 parent_id */
    public static final long ROOT_PARENT_ID = 0L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long videoId;

    private Long userId;

    /** 父评论ID，0为一级评论；回复回复时保存被回复评论ID，展示时归并到一级评论下 */
    private Long parentId;

    private String content;

    private Integer likeCount;

    /** 1正常 0已删除 */
    private Integer status;

    private LocalDateTime createTime;
}
