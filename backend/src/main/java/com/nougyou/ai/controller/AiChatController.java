package com.nougyou.ai.controller;

import com.nougyou.ai.dto.ChatMessageVO;
import com.nougyou.ai.dto.ChatReplyVO;
import com.nougyou.ai.dto.ChatRequest;
import com.nougyou.ai.dto.ChatSessionCreateRequest;
import com.nougyou.ai.dto.ChatSessionVO;
import com.nougyou.ai.service.AiChatService;
import com.nougyou.common.api.ApiResponse;
import com.nougyou.common.page.PageResult;
import com.nougyou.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * AI 客服对话接口。登录后即可使用（买家、农户商家共享）。
 */
@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiChatController {

    private final AiChatService aiChatService;

    @PostMapping("/sessions")
    public ApiResponse<ChatSessionVO> createSession(
            @RequestBody(required = false) ChatSessionCreateRequest request) {
        ChatSessionCreateRequest safeRequest = request == null
                ? new ChatSessionCreateRequest(null, null)
                : request;
        return ApiResponse.ok(aiChatService.createSession(SecurityUtils.currentUser().userId(), safeRequest));
    }

    @GetMapping("/sessions")
    public ApiResponse<PageResult<ChatSessionVO>> listSessions(
            @RequestParam(required = false) Integer pageNum,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String sceneCode) {
        return ApiResponse.ok(aiChatService.listSessions(
                SecurityUtils.currentUser().userId(), pageNum, pageSize, sceneCode));
    }

    @GetMapping("/sessions/{id}/messages")
    public ApiResponse<PageResult<ChatMessageVO>> listMessages(
            @PathVariable Long id,
            @RequestParam(required = false) Integer pageNum,
            @RequestParam(required = false) Integer pageSize) {
        return ApiResponse.ok(aiChatService.listMessages(
                SecurityUtils.currentUser().userId(), id, pageNum, pageSize));
    }

    @PostMapping("/chat")
    public ApiResponse<ChatReplyVO> chat(@Valid @RequestBody ChatRequest request) {
        return ApiResponse.ok(aiChatService.chat(SecurityUtils.currentUser().userId(), request));
    }

    /**
     * 流式问答：SSE 事件 meta / delta / done / error。
     */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> chatStream(@Valid @RequestBody ChatRequest request) {
        SseEmitter emitter = aiChatService.streamChat(SecurityUtils.currentUser().userId(), request);
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-cache")
                .header("X-Accel-Buffering", "no")
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .body(emitter);
    }
}
