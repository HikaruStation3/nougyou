package com.nougyou.ai.dto;

import jakarta.validation.constraints.Size;

/**
 * 新建会话入参。
 */
public record ChatSessionCreateRequest(
        String sceneCode,
        @Size(max = 50, message = "标题不能超过 50 字") String title
) {
}
