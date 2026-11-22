package com.nougyou.ai.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nougyou.ai.config.AiProperties;
import com.nougyou.ai.dto.ChatMessageVO;
import com.nougyou.ai.dto.ChatReplyVO;
import com.nougyou.ai.dto.ChatRequest;
import com.nougyou.ai.dto.ChatSessionCreateRequest;
import com.nougyou.ai.dto.ChatSessionVO;
import com.nougyou.ai.entity.AiChatMessage;
import com.nougyou.ai.entity.AiChatSession;
import com.nougyou.ai.enums.AiScene;
import com.nougyou.ai.mapper.AiChatMessageMapper;
import com.nougyou.ai.mapper.AiChatSessionMapper;
import com.nougyou.common.api.ResultCode;
import com.nougyou.common.exception.BusinessException;
import com.nougyou.common.page.PageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * AI 客服对话：会话管理 + 流式问答（SSE）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatService {

    private static final int MAX_PAGE_SIZE = 100;

    private final ChatClient chatClient;
    private final AiChatSessionMapper sessionMapper;
    private final AiChatMessageMapper messageMapper;
    private final AiConversationStore conversationStore;
    private final AiPromptService promptService;
    private final AiContentGuardService contentGuardService;
    private final AiProperties aiProperties;
    private final ObjectMapper objectMapper;

    // ------------------------------------------------------------------ 会话

    public ChatSessionVO createSession(Long userId, ChatSessionCreateRequest request) {
        AiScene scene = AiScene.of(request == null ? null : request.sceneCode());
        String title = StringUtils.hasText(request == null ? null : request.title())
                ? request.title()
                : scene.getLabel();
        AiChatSession session = conversationStore.createSession(userId, scene, title);
        return ChatSessionVO.of(session, promptService.sceneName(scene));
    }

    public PageResult<ChatSessionVO> listSessions(Long userId, Integer pageNum, Integer pageSize, String sceneCode) {
        Page<AiChatSession> page = new Page<>(pageNum(pageNum), pageSize(pageSize));
        AiScene scene = StringUtils.hasText(sceneCode) ? AiScene.of(sceneCode) : null;

        Page<AiChatSession> result = sessionMapper.selectPage(page, Wrappers.<AiChatSession>lambdaQuery()
                .eq(AiChatSession::getUserId, userId)
                .eq(scene != null, AiChatSession::getSceneCode, scene == null ? null : scene.name())
                .orderByDesc(AiChatSession::getUpdateTime)
                .orderByDesc(AiChatSession::getId));

        List<ChatSessionVO> list = result.getRecords().stream()
                .map(item -> ChatSessionVO.of(item, sceneNames().get(AiScene.of(item.getSceneCode()))))
                .toList();
        return new PageResult<>(list, result.getTotal());
    }

    public PageResult<ChatMessageVO> listMessages(Long userId, Long sessionId, Integer pageNum, Integer pageSize) {
        conversationStore.requireOwnedSession(userId, sessionId);
        Page<AiChatMessage> page = new Page<>(pageNum(pageNum), pageSize(pageSize));
        Page<AiChatMessage> result = messageMapper.selectPage(page, Wrappers.<AiChatMessage>lambdaQuery()
                .eq(AiChatMessage::getSessionId, sessionId)
                .orderByAsc(AiChatMessage::getId));
        List<ChatMessageVO> list = result.getRecords().stream().map(ChatMessageVO::of).toList();
        return new PageResult<>(list, result.getTotal());
    }

    // ------------------------------------------------------------------ 问答

    /**
     * 非流式问答，便于脚本或降级调用。
     */
    public ChatReplyVO chat(Long userId, ChatRequest request) {
        PreparedChat chat = prepare(userId, request);
        String content;
        try {
            content = chatClient.prompt()
                    .system(chat.systemPrompt())
                    .messages(chat.history())
                    .user(chat.question())
                    .call()
                    .content();
        } catch (Exception ex) {
            log.error("AI 调用失败 sessionId={}", chat.session().getId(), ex);
            throw new BusinessException(ResultCode.SERVER_ERROR, "AI 服务暂时不可用，请稍后重试");
        }

        AiContentGuardService.GuardResult guard = contentGuardService.inspect(content);
        AiChatMessage reply = conversationStore.appendMessage(
                chat.session().getId(), "assistant", guard.content(), guard.flagged());
        conversationStore.refreshSession(chat.session().getId());
        return new ChatReplyVO(chat.session().getId(), reply.getId(), guard.content(), guard.flagged());
    }

    /**
     * 流式问答：事件 meta → delta（增量） → done（最终内容，含管控结果）；异常发送 error 事件。
     */
    public SseEmitter streamChat(Long userId, ChatRequest request) {
        PreparedChat chat = prepare(userId, request);

        SseEmitter emitter = new SseEmitter(aiProperties.getTimeoutMs());
        AtomicReference<Disposable> subscription = new AtomicReference<>();
        StringBuffer buffer = new StringBuffer();
        AtomicBoolean persisted = new AtomicBoolean(false);
        Long sessionId = chat.session().getId();

        Runnable persistPartial = () -> {
            if (persisted.compareAndSet(false, true) && StringUtils.hasText(buffer)) {
                saveInterruptedReply(sessionId, buffer.toString());
            }
        };
        Runnable cancel = () -> {
            Disposable disposable = subscription.get();
            if (disposable != null && !disposable.isDisposed()) {
                disposable.dispose();
            }
        };

        emitter.onCompletion(() -> {
            // 正常结束已在 completeNormally 中落库，此处仅兜底客户端提前断开
            persistPartial.run();
            cancel.run();
        });
        emitter.onTimeout(() -> {
            persistPartial.run();
            cancel.run();
            emitter.complete();
        });
        emitter.onError(error -> {
            persistPartial.run();
            cancel.run();
        });

        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("sessionId", sessionId);
        meta.put("sceneCode", chat.scene().name());
        meta.put("sceneName", promptService.sceneName(chat.scene()));
        if (!send(emitter, "meta", meta)) {
            return emitter;
        }

        Flux<String> flux = chatClient.prompt()
                .system(chat.systemPrompt())
                .messages(chat.history())
                .user(chat.question())
                .stream()
                .content();

        subscription.set(flux.publishOn(Schedulers.boundedElastic()).subscribe(
                delta -> {
                    if (!StringUtils.hasText(delta)) {
                        return;
                    }
                    buffer.append(delta);
                    if (!send(emitter, "delta", Map.of("content", delta))) {
                        cancel.run();
                        persistPartial.run();
                    }
                },
                error -> {
                    log.error("AI 流式调用失败 sessionId={}", sessionId, error);
                    send(emitter, "error", Map.of("message", "AI 服务暂时不可用，请稍后重试"));
                    complete(emitter);
                },
                () -> completeNormally(emitter, sessionId, buffer.toString(), persisted)));

        return emitter;
    }

    private void completeNormally(SseEmitter emitter, Long sessionId, String rawContent,
                                  AtomicBoolean persisted) {
        try {
            AiContentGuardService.GuardResult guard = contentGuardService.inspect(rawContent);
            if (persisted.compareAndSet(false, true)) {
                AiChatMessage reply = conversationStore.appendMessage(
                        sessionId, "assistant", guard.content(), guard.flagged());
                conversationStore.refreshSession(sessionId);

                Map<String, Object> done = new LinkedHashMap<>();
                done.put("sessionId", sessionId);
                done.put("messageId", reply.getId());
                done.put("content", guard.content());
                done.put("blocked", guard.blocked());
                done.put("flagged", guard.flagged());
                send(emitter, "done", done);
            }
        } catch (Exception ex) {
            log.error("AI 回复落库失败 sessionId={}", sessionId, ex);
            send(emitter, "error", Map.of("message", "回复保存失败，请刷新会话查看"));
        }
        complete(emitter);
    }

    /**
     * 客户端中途断开时保留已生成内容，保证会话记录与 msg_count 一致。
     */
    private void saveInterruptedReply(Long sessionId, String partial) {
        try {
            conversationStore.appendMessage(sessionId, "assistant", partial + "\n\n（回答已中断）", false);
            conversationStore.refreshSession(sessionId);
        } catch (Exception ex) {
            log.warn("中断回复落库失败 sessionId={}", sessionId, ex);
        }
    }

    // ------------------------------------------------------------------ 内部

    private PreparedChat prepare(Long userId, ChatRequest request) {
        String question = request.question() == null ? "" : request.question().trim();
        if (!StringUtils.hasText(question)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "问题不能为空");
        }
        if (question.length() > aiProperties.getMaxQuestionLength()) {
            throw new BusinessException(ResultCode.BAD_REQUEST,
                    "问题不能超过 " + aiProperties.getMaxQuestionLength() + " 字");
        }

        AiChatSession session;
        AiScene scene;
        if (request.sessionId() != null) {
            session = conversationStore.requireOwnedSession(userId, request.sessionId());
            scene = AiScene.of(session.getSceneCode());
        } else {
            scene = AiScene.of(request.sceneCode());
            session = conversationStore.createSession(userId, scene, question);
        }

        // 先取历史，再落用户消息，避免本轮问题重复进入上下文
        List<Message> history = conversationStore.loadHistory(session.getId());
        conversationStore.appendMessage(session.getId(), "user", question, false);

        String systemPrompt = promptService.systemPrompt(scene)
                + "\n\n补充信息：今天是 " + LocalDate.now() + "，当前用户 ID 为 " + userId + "。";

        return new PreparedChat(session, scene, systemPrompt, history, question);
    }

    private Map<AiScene, String> sceneNames() {
        Map<AiScene, String> names = new java.util.EnumMap<>(AiScene.class);
        for (AiScene scene : AiScene.values()) {
            names.put(scene, promptService.sceneName(scene));
        }
        return names;
    }

    private boolean send(SseEmitter emitter, String event, Object data) {
        try {
            emitter.send(SseEmitter.event().name(event).data(objectMapper.writeValueAsString(data)));
            return true;
        } catch (IOException | IllegalStateException ex) {
            log.warn("SSE 发送失败，客户端可能已断开：event={}", event);
            return false;
        }
    }

    private void complete(SseEmitter emitter) {
        try {
            emitter.complete();
        } catch (IllegalStateException ignored) {
            // 已完成或已断开，忽略
        }
    }

    private static int pageNum(Integer pageNum) {
        return pageNum == null || pageNum < 1 ? 1 : pageNum;
    }

    private static int pageSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) {
            return 10;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }

    /**
     * 一次问答的上下文快照。
     */
    private record PreparedChat(AiChatSession session, AiScene scene, String systemPrompt,
                                List<Message> history, String question) {
    }
}
