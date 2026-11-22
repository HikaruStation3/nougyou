package com.nougyou.ai.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 销售可行性分析入参（FR-028）。
 *
 * @param productId 须为当前商家名下商品
 * @param months    分析月份跨度，3-12，默认 6
 * @param sessionId 传入则沿用该会话上下文，支持追问
 */
public record SalesAnalysisRequest(
        @NotNull(message = "商品不能为空") Long productId,
        @Min(value = 3, message = "分析跨度最少 3 个月") @Max(value = 12, message = "分析跨度最多 12 个月")
        Integer months,
        Long sessionId
) {
}
