package com.nougyou.ai.dto;

import com.nougyou.ai.entity.AiChatSession;

import java.time.LocalDateTime;

/**
 * 会话列表项。{@code userName} 仅在管理员监控视角返回。
 */
public record ChatSessionVO(
        Long id,
        Long userId,
        String userName,
        String sceneCode,
        String sceneName,
        String title,
        String model,
        Integer msgCount,
        LocalDateTime createTime,
        LocalDateTime updateTime
) {

    public static ChatSessionVO of(AiChatSession session, String sceneName) {
        return of(session, sceneName, null);
    }

    public static ChatSessionVO of(AiChatSession session, String sceneName, String userName) {
        return new ChatSessionVO(
                session.getId(),
                session.getUserId(),
                userName,
                session.getSceneCode(),
                sceneName,
                session.getTitle(),
                session.getModel(),
                session.getMsgCount(),
                session.getCreateTime(),
                session.getUpdateTime()
        );
    }
}
