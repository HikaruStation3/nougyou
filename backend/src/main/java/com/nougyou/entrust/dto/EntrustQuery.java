package com.nougyou.entrust.dto;

import lombok.Data;

/**
 * 委托关系列表查询条件（API-088 / API-089 / 后台委托列表）。
 */
@Data
public class EntrustQuery {

    private long pageNum = 1;
    private long pageSize = 10;

    /** 0 待确认 / 1 生效中 / 2 已终止 / 3 已拒绝 */
    private Integer status;

    /** 后台按委托编号或商品名搜索 */
    private String keyword;
}
