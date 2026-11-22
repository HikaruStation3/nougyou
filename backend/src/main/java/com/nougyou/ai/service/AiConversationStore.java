package com.nougyou.ai.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.nougyou.ai.config.AiProperties;
import com.nougyou.ai.entity.AiChatMessage;
import com.nougyou.ai.entity.AiChatSession;
import com.nougyou.ai.enums.AiScene;
import com.nougyou.ai.mapper.AiChatMessageMapper;
import com.nougyou.ai.mapper.AiChatSessionMapper;
import com.nougyou.common.api.ResultCode;
import com.nougyou.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * AI 会话与消息的持久化门面，供对话、销售分析、后台监控共用。
 */
@Component
@RequiredArgsConstructor
public class AiConversationStore {

    private final AiChatSessionMapper sessionMapper;
    private final AiChatMessageMapper messageMapper;
    private final AiProperties aiProperties;

    public AiChatSession createSession(Long userId, AiScene scene, String title) {
        AiChatSession session = new AiChatSession();
        session.setUserId(userId);
        session.setSceneCode(scene.name());
        session.setTitle(truncate(StringUtils.hasText(title) ? title.trim() : scene.getLabel(), 50));
        session.setModel(aiProperties.getModelName());
        session.setMsgCount(0);
        LocalDateTime now = LocalDateTime.now();
        session.setCreateTime(now);
        session.setUpdateTime(now);
        sessionMapper.insert(session);
        return session;
    }

    /**
     * 会话必须属于当前用户，否则按不存在处理，避免越权探知会话 ID。
     */
    public AiChatSession requireOwnedSession(Long userId, Long sessionId) {
        AiChatSession session = requireSession(sessionId);
        if (!session.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "会话不存在或无权访问");
        }
        return session;
    }

    public AiChatSession requireSession(Long sessionId) {
        AiChatSession session = sessionId == null ? null : sessionMapper.selectById(sessionId);
        if (session == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "会话不存在");
        }
        return session;
    }

    public AiChatMessage appendMessage(Long sessionId, String role, String content, boolean flagged) {
        AiChatMessage message = new AiChatMessage();
        message.setSessionId(sessionId);
        message.setMsgRole(role);
        message.setContent(content);
        message.setTokenCount(0);
        message.setIsFlagged(flagged ? 1 : 0);
        message.setCreateTime(LocalDateTime.now());
        messageMapper.insert(message);
        return message;
    }

    /**
     * 取最近 N 条消息作为多轮上下文，按时间正序返回。
     * 调用方需在本轮问题落库前调用，避免问题重复进入上下文。
     */
    public List<Message> loadHistory(Long sessionId) {
        int size = Math.max(0, aiProperties.getHistorySize());
        if (size == 0) {
            return List.of();
        }
        List<AiChatMessage> records = messageMapper.selectList(Wrappers.<AiChatMessage>lambdaQuery()
                .eq(AiChatMessage::getSessionId, sessionId)
                .orderByDesc(AiChatMessage::getId)
                .last("LIMIT " + size));
        if (records.isEmpty()) {
            return List.of();
        }
        List<AiChatMessage> ordered = new ArrayList<>(records);
        Collections.reverse(ordered);

        List<Message> history = new ArrayList<>(ordered.size());
        for (AiChatMessage record : ordered) {
            if (!StringUtils.hasText(record.getContent())) {
                continue;
            }
            if ("assistant".equalsIgnoreCase(record.getMsgRole())) {
                history.add(new AssistantMessage(record.getContent()));
            } else if ("user".equalsIgnoreCase(record.getMsgRole())) {
                history.add(new UserMessage(record.getContent()));
            }
        }
        return history;
    }

    /**
     * 以实际消息条数回写会话计数，避免并发或中断导致计数漂移。
     */
    public void refreshSession(Long sessionId) {
        Long count = messageMapper.selectCount(Wrappers.<AiChatMessage>lambdaQuery()
                .eq(AiChatMessage::getSessionId, sessionId));
        sessionMapper.update(null, Wrappers.<AiChatSession>lambdaUpdate()
                .eq(AiChatSession::getId, sessionId)
                .set(AiChatSession::getMsgCount, count == null ? 0 : count.intValue())
                .set(AiChatSession::getUpdateTime, LocalDateTime.now()));
    }

    public static String truncate(String text, int maxLength) {
        if (text == null) {
            return null;
        }
        return text.length() <= maxLength ? text : text.substring(0, maxLength);
    }
}
