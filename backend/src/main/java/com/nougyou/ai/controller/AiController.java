package com.nougyou.ai.controller;

import com.nougyou.ai.dto.ChatRequest;
import com.nougyou.ai.dto.ChatResponse;
import com.nougyou.common.api.ApiResponse;
import com.nougyou.common.api.ResultCode;
import com.nougyou.common.exception.BusinessException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiController {

    private final ChatClient.Builder chatClientBuilder;

    @PostMapping("/chat")
    @PreAuthorize("hasAuthority('ai:chat:use')")
    public ApiResponse<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        try {
            String content = chatClientBuilder.build()
                    .prompt()
                    .system("你是 Nougyou 平台的智能助手，请使用简洁的中文回答。")
                    .user(request.message())
                    .call()
                    .content();
            return ApiResponse.ok(new ChatResponse(content));
        } catch (Exception ex) {
            throw new BusinessException(ResultCode.SERVER_ERROR, "AI 调用失败：" + ex.getMessage());
        }
    }
}
