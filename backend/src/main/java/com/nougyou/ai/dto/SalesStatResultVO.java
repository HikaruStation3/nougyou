package com.nougyou.ai.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * 销售统计汇总（FR-027）。
 * <p>
 * {@code items} 固定按商品维度返回（表头指标用），{@code trend} 固定按整店按月汇总，
 * {@code groupBy} 仅表示前端默认展示维度，服务端一次返回两份数据避免二次请求。
 */
public record SalesStatResultVO(
        String startDate,
        String endDate,
        String groupBy,
        int totalQty,
        BigDecimal totalAmount,
        int refundQty,
        BigDecimal refundAmount,
        int productCount,
        int stockTotal,
        List<SalesStatVO> items,
        List<SalesPointVO> trend
) {
}
