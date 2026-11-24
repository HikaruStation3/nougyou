package com.nougyou.video.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 买家关注农户商家，uk_user_farmer 保证关注关系唯一（FR-012）。
 */
@Data
@TableName("video_follow")
public class VideoFollow {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关注人（买家）用户ID */
    private Long userId;

    /** 被关注农户商家ID */
    private Long farmerId;

    private LocalDateTime createTime;
}
