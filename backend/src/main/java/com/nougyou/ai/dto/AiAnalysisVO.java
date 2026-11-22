package com.nougyou.ai.dto;

import java.util.List;

/**
 * 销售可行性分析结果（FR-028）。
 *
 * @param sessionId  会话 ID，可继续追问
 * @param messageId  回答消息 ID
 * @param answer     AI 分析结论
 * @param products   按商品拆分的规则化分析结果
 * @param tokenCount 消耗 token 数
 */
public record AiAnalysisVO(
        Long sessionId,
        Long messageId,
        String answer,
        List<ProductAnalysisVO> products,
        int tokenCount
) {
}
