package com.nougyou.video.dto;

import lombok.Data;

/**
 * 管理端短视频列表查询条件（审核工作台使用）。
 */
@Data
public class AdminVideoQuery {

    private long pageNum = 1;

    private long pageSize = 10;

    /** 0 待审核 / 1 通过 / 2 驳回 */
    private Integer auditStatus;

    /** 标题 / 商家名称关键字 */
    private String keyword;
}
