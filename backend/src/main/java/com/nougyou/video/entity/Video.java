package com.nougyou.video.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 短视频（FR-011 观看 / FR-020 发布 / FR-021、FR-035 审核）。
 */
@Data
@TableName("video")
public class Video {

    /** 审核状态：待审核 */
    public static final int AUDIT_PENDING = 0;
    /** 审核状态：通过，对外展示 */
    public static final int AUDIT_PASS = 1;
    /** 审核状态：驳回 */
    public static final int AUDIT_REJECT = 2;

    /** 视频状态：已下架 */
    public static final int STATUS_OFF = 0;
    /** 视频状态：正常 */
    public static final int STATUS_ON = 1;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 发布农户商家用户ID */
    private Long farmerId;

    private String title;

    /** 封面图地址 */
    private String cover;

    /** 视频播放地址 */
    private String videoUrl;

    /** 时长（秒） */
    private Integer duration;

    private String description;

    /** 审核状态 0待审核 1通过 2驳回 */
    private Integer auditStatus;

    private Long auditUserId;

    private LocalDateTime auditTime;

    private String auditRemark;

    private Integer likeCount;

    private Integer commentCount;

    private Integer viewCount;

    /** 审核通过后的展示时间 */
    private LocalDateTime publishTime;

    /** 1正常 0已下架 */
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
