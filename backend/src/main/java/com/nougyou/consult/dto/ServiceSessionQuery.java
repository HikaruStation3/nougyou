package com.nougyou.consult.dto;

import lombok.Data;

/**
 * 我的会话列表查询（API-068）。
 */
@Data
public class ServiceSessionQuery {

    private long pageNum = 1;
    private long pageSize = 10;

    /** 0 排队中 / 1 进行中 / 2 已结束，为空查全部 */
    private Integer status;
}
