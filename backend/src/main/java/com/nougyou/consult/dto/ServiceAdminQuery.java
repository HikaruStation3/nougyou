package com.nougyou.consult.dto;

import lombok.Data;

/**
 * 客服工作台查询（API-072）。管理员可见全部会话，优先展示待接入。
 */
@Data
public class ServiceAdminQuery {

    private long pageNum = 1;
    private long pageSize = 10;

    /** 0 待接入 / 1 进行中 / 2 已结束 */
    private Integer status;

    /** 1 平台客服 / 2 商家，默认 1 */
    private Integer targetType = 1;

    /** 发起人昵称或会话编号模糊匹配 */
    private String keyword;

    /** 只看我接待的会话 */
    private Boolean mine;
}
