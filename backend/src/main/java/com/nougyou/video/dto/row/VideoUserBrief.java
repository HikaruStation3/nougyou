package com.nougyou.video.dto.row;

import lombok.Data;

/**
 * 短视频展示所需的用户快照（只读），由 VideoReferenceMapper 一次查出，避免 N+1。
 */
@Data
public class VideoUserBrief {

    private Long id;

    private String nickname;

    private String avatar;

    /** 店铺名称，取自 sys_dept.name */
    private String shopName;

    /** 角色编码：ADMIN / FARMER / BUYER */
    private String roleCode;
}
