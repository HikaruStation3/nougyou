package com.nougyou.video.dto;

import lombok.Data;

/**
 * 商家「我的短视频」查询条件（API-078），可按审核状态查看审核进度。
 */
@Data
public class MyVideoQuery {

    private long pageNum = 1;

    private long pageSize = 10;

    /** 0 待审核 / 1 通过 / 2 驳回 */
    private Integer auditStatus;
}
