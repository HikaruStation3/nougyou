package com.nougyou.cms.dto;

import lombok.Data;

/**
 * 扶贫政策列表查询条件（API-021）。
 */
@Data
public class PolicyPageQuery {

    private long pageNum = 1;
    private long pageSize = 10;

    /** 匹配标题与政策文号 */
    private String keyword;

    /** 发布单位 */
    private String issueDept;
}
