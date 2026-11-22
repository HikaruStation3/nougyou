package com.nougyou.ai.dto.row;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 按商品+月份聚合的销售行（统计查询投影）。
 */
@Data
public class ProductMonthSaleRow {

    private Long productId;
    /** yyyy-MM */
    private String month;
    private Integer qty;
    private BigDecimal amount;
}
