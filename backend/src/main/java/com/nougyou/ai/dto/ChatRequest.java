package com.nougyou.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 向 AI 提问。sessionId 为空时按 sceneCode 新建会话。
 */
public record ChatRequest(
        Long sessionId,
        String sceneCode,
        @NotBlank(message = "问题不能为空")
        @Size(max = 1000, message = "问题不能超过 1000 字")
        String question,
        Boolean stream
) {
}
