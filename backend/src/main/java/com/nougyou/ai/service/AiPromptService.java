package com.nougyou.ai.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.nougyou.ai.entity.AiPromptConfig;
import com.nougyou.ai.enums.AiScene;
import com.nougyou.ai.mapper.AiPromptConfigMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 按场景读取启用的系统提示词（FR-037：版本化，取最高版本）。
 */
@Service
@RequiredArgsConstructor
public class AiPromptService {

    private final AiPromptConfigMapper promptConfigMapper;

    public String systemPrompt(AiScene scene) {
        AiPromptConfig config = findEnabled(scene);
        if (config != null && StringUtils.hasText(config.getPromptContent())) {
            return config.getPromptContent();
        }
        return scene.getDefaultSystemPrompt();
    }

    public String sceneName(AiScene scene) {
        AiPromptConfig config = findEnabled(scene);
        return config != null && StringUtils.hasText(config.getSceneName())
                ? config.getSceneName()
                : scene.getLabel();
    }

    private AiPromptConfig findEnabled(AiScene scene) {
        return promptConfigMapper.selectOne(Wrappers.<AiPromptConfig>lambdaQuery()
                .eq(AiPromptConfig::getSceneCode, scene.name())
                .eq(AiPromptConfig::getIsEnable, 1)
                .orderByDesc(AiPromptConfig::getVersion)
                .last("LIMIT 1"));
    }
}
