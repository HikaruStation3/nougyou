package com.nougyou.video.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 短视频点赞记录，uk_video_user 保证同一用户对同一视频只计一次（FR-012）。
 */
@Data
@TableName("video_like")
public class VideoLike {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long videoId;

    private Long userId;

    private LocalDateTime createTime;
}
