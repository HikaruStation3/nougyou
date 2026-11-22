package com.nougyou.ai.dto;

import com.nougyou.ai.entity.AiPromptConfig;

import java.time.LocalDateTime;

/**
 * 提示词配置（FR-037），含当前启用标记与最后修改人。
 */
public record PromptConfigVO(
        Long id,
        String sceneCode,
        String sceneName,
        String promptContent,
        Integer version,
        Integer isEnable,
        Long updateUserId,
        String updateUserName,
        LocalDateTime updateTime
) {

    public static PromptConfigVO of(AiPromptConfig config, String updateUserName) {
        return new PromptConfigVO(
                config.getId(),
                config.getSceneCode(),
                config.getSceneName(),
                config.getPromptContent(),
                config.getVersion(),
                config.getIsEnable(),
                config.getUpdateUserId(),
                updateUserName,
                config.getUpdateTime()
        );
    }
}
