package com.nougyou.ai.dto;

/**
 * 单品分析结果（FR-028）。
 *
 * @param trendDirection 走势判断：up 上升 / flat 平稳 / down 下降
 * @param suggestion     规则建议，如「可维持库存」「建议控制上架量」
 */
public record ProductAnalysisVO(
        Long productId,
        String productName,
        int saleQty,
        String trendDirection,
        String suggestion
) {
}
