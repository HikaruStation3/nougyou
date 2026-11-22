package com.nougyou.ai.dto;

import com.nougyou.ai.entity.AiChatMessage;

import java.time.LocalDateTime;

/**
 * 会话消息项。
 */
public record ChatMessageVO(
        Long id,
        Long sessionId,
        String role,
        String content,
        boolean flagged,
        LocalDateTime createTime
) {

    public static ChatMessageVO of(AiChatMessage message) {
        return new ChatMessageVO(
                message.getId(),
                message.getSessionId(),
                message.getMsgRole(),
                message.getContent(),
                message.getIsFlagged() != null && message.getIsFlagged() == 1,
                message.getCreateTime()
        );
    }
}
