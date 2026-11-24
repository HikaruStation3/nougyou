package com.nougyou.video.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 短视频展示对象（API-075 / 076 / 080 共用）。
 */
@Data
@Builder
public class VideoVO {

    private Long id;

    private Long farmerId;

    /** 商家姓名 */
    private String farmerName;

    /** 店铺名称 */
    private String shopName;

    /** 商家头像 */
    private String farmerAvatar;

    private String title;

    private String cover;

    private String videoUrl;

    /** 时长（秒） */
    private Integer duration;

    private String description;

    /** 审核状态：0 待审核 / 1 通过 / 2 驳回 */
    private Integer auditStatus;

    /** 驳回原因 */
    private String auditRemark;

    private Integer likeCount;

    private Integer commentCount;

    private Integer viewCount;

    /** 当前用户是否已点赞 */
    private Boolean liked;

    /** 当前用户是否已关注该商家 */
    private Boolean followed;

    /** 是否本人发布，前端据此展示删除入口 */
    private Boolean mine;

    /** 视频状态：1 正常 / 0 已下架 */
    private Integer status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime publishTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
