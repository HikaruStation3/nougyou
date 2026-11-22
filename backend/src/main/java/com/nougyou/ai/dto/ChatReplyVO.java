package com.nougyou.ai.dto;

/**
 * 非流式问答结果。
 */
public record ChatReplyVO(
        Long sessionId,
        Long messageId,
        String content,
        boolean flagged
) {
}
