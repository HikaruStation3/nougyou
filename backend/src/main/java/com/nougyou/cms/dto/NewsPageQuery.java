package com.nougyou.cms.dto;

import lombok.Data;

/**
 * 扶贫资讯列表查询条件（API-027）。
 */
@Data
public class NewsPageQuery {

    private long pageNum = 1;
    private long pageSize = 10;

    /** 栏目：1 景点宣传 / 2 旅游推介 / 3 乡镇历史 / 4 地域文化 / 5 人文地理 */
    private Integer newsType;

    /** 匹配标题与摘要 */
    private String keyword;

    /** 管理端专用：0 草稿 / 1 已发布 / 2 已下架；公开列表忽略该条件 */
    private Integer publishStatus;
}
