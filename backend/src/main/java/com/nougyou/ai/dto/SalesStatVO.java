package com.nougyou.ai.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * 商品维度销售统计（FR-027）。
 *
 * @param stock        当前库存
 * @param saleQty      区间内销量
 * @param saleAmount   区间内销售额
 * @param refundQty    区间内退单件数
 * @param refundAmount 区间内退款金额
 * @param trend        按月走势
 */
public record SalesStatVO(
        Long productId,
        String productName,
        String cover,
        Integer stock,
        String unit,
        int saleQty,
        BigDecimal saleAmount,
        int refundQty,
        BigDecimal refundAmount,
        List<SalesPointVO> trend
) {
}
