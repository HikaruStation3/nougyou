package com.nougyou.product.dto;

import lombok.Data;

/**
 * 分类树查询条件（API-040）。
 */
@Data
public class CategoryQuery {

    /** 是否只返回启用分类，默认 true */
    private Boolean onlyEnabled = Boolean.TRUE;
}
