package com.nougyou.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 新增提示词版本入参（FR-037）。新增后立即启用，旧版本自动停用但保留，可用于回滚。
 */
public record PromptSaveRequest(
        @NotBlank(message = "场景编码不能为空") @Size(max = 30) String sceneCode,
        @NotBlank(message = "场景名称不能为空")
        @Size(min = 2, max = 30, message = "场景名称需 2-30 字") String sceneName,
        @NotBlank(message = "提示词正文不能为空")
        @Size(max = 8000, message = "提示词正文不能超过 8000 字") String promptContent
) {
}
