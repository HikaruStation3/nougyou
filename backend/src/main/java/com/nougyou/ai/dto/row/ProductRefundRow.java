package com.nougyou.ai.dto.row;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 按商品聚合的退单行（已完成售后的退款）。
 */
@Data
public class ProductRefundRow {

    private Long productId;
    private Integer refundQty;
    private BigDecimal refundAmount;
}
