package com.nougyou.product.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 农产品列表查询条件（API-041）。
 */
@Data
public class ProductPageQuery {

    private long pageNum = 1;
    private long pageSize = 10;

    /** 分类 ID，传父分类时包含其子分类 */
    private Long categoryId;

    /** 匹配商品名称与产地 */
    private String keyword;

    /** 最低价 */
    private BigDecimal minPrice;

    /** 最高价 */
    private BigDecimal maxPrice;

    /** 只看扶贫产品：0 否 / 1 是 */
    private Integer isPoverty;

    /** 排序方式：price_asc / price_desc / sale_desc / new */
    private String sortBy;
}
