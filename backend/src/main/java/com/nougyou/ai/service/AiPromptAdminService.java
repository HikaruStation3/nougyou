package com.nougyou.ai.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.nougyou.ai.dto.PromptConfigVO;
import com.nougyou.ai.dto.PromptSaveRequest;
import com.nougyou.ai.entity.AiPromptConfig;
import com.nougyou.ai.enums.AiScene;
import com.nougyou.ai.mapper.AiPromptConfigMapper;
import com.nougyou.common.api.ResultCode;
import com.nougyou.common.exception.BusinessException;
import com.nougyou.system.entity.SysUser;
import com.nougyou.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 提示词版本管理（FR-037）。
 * <p>
 * 同一场景只有一版启用；新增版本自动启用并停用旧版本，旧版本保留以便回滚。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiPromptAdminService {

    private final AiPromptConfigMapper promptConfigMapper;
    private final SysUserMapper userMapper;

    public List<PromptConfigVO> list(String sceneCode) {
        String normalized = StringUtils.hasText(sceneCode)
                ? normalizeSceneCode(sceneCode)
                : null;

        List<AiPromptConfig> configs = promptConfigMapper.selectList(Wrappers.<AiPromptConfig>lambdaQuery()
                .eq(normalized != null, AiPromptConfig::getSceneCode, normalized)
                .orderByAsc(AiPromptConfig::getSceneCode)
                .orderByDesc(AiPromptConfig::getVersion));

        Map<Long, String> userNames = resolveUserNames(configs.stream()
                .map(AiPromptConfig::getUpdateUserId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList());

        return configs.stream()
                .sorted(Comparator.comparing(AiPromptConfig::getSceneCode)
                        .thenComparing(Comparator.comparing(AiPromptConfig::getVersion).reversed()))
                .map(config -> PromptConfigVO.of(config, userNames.get(config.getUpdateUserId())))
                .toList();
    }

    @Transactional
    public PromptConfigVO create(PromptSaveRequest request, Long adminId) {
        String sceneCode = normalizeSceneCode(request.sceneCode());
        List<AiPromptConfig> existing = promptConfigMapper.selectList(
                Wrappers.<AiPromptConfig>lambdaQuery().eq(AiPromptConfig::getSceneCode, sceneCode));

        int nextVersion = existing.stream()
                .map(AiPromptConfig::getVersion)
                .filter(java.util.Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(0) + 1;

        // 停用同场景旧版本，保留记录用于回滚
        if (!existing.isEmpty()) {
            promptConfigMapper.update(null, Wrappers.<AiPromptConfig>lambdaUpdate()
                    .eq(AiPromptConfig::getSceneCode, sceneCode)
                    .set(AiPromptConfig::getIsEnable, 0)
                    .set(AiPromptConfig::getUpdateTime, LocalDateTime.now()));
        }

        AiPromptConfig config = new AiPromptConfig();
        config.setSceneCode(sceneCode);
        config.setSceneName(request.sceneName().trim());
        config.setPromptContent(request.promptContent().trim());
        config.setVersion(nextVersion);
        config.setIsEnable(1);
        config.setUpdateUserId(adminId);
        config.setCreateTime(LocalDateTime.now());
        config.setUpdateTime(LocalDateTime.now());
        promptConfigMapper.insert(config);

        log.info("新增 AI 提示词版本 scene={} version={} admin={}", sceneCode, nextVersion, adminId);
        return PromptConfigVO.of(config, displayName(adminId));
    }

    @Transactional
    public PromptConfigVO enable(Long id, Long adminId) {
        AiPromptConfig config = promptConfigMapper.selectById(id);
        if (config == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "提示词版本不存在");
        }

        promptConfigMapper.update(null, Wrappers.<AiPromptConfig>lambdaUpdate()
                .eq(AiPromptConfig::getSceneCode, config.getSceneCode())
                .ne(AiPromptConfig::getId, id)
                .set(AiPromptConfig::getIsEnable, 0)
                .set(AiPromptConfig::getUpdateTime, LocalDateTime.now()));

        config.setIsEnable(1);
        config.setUpdateUserId(adminId);
        config.setUpdateTime(LocalDateTime.now());
        promptConfigMapper.updateById(config);

        log.info("启用 AI 提示词版本 scene={} version={} admin={}",
                config.getSceneCode(), config.getVersion(), adminId);
        return PromptConfigVO.of(config, displayName(adminId));
    }

    /**
     * 场景编码须与 AiScene 一致，避免写入永远不会被读取的提示词。
     */
    private String normalizeSceneCode(String sceneCode) {
        String code = sceneCode == null ? "" : sceneCode.trim().toUpperCase(Locale.ROOT);
        for (AiScene scene : AiScene.values()) {
            if (scene.name().equals(code)) {
                return scene.name();
            }
        }
        throw new BusinessException(ResultCode.BAD_REQUEST,
                "场景编码不支持，可选值：BIZ_CONSULT / SALE_STAT / SALE_ANALYSIS");
    }

    private Map<Long, String> resolveUserNames(List<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return userMapper.selectList(Wrappers.<SysUser>lambdaQuery().in(SysUser::getId, userIds))
                .stream()
                .collect(Collectors.toMap(SysUser::getId, AiPromptAdminService::displayNameOf,
                        (left, right) -> left));
    }

    private String displayName(Long userId) {
        if (userId == null) {
            return null;
        }
        return displayNameOf(userMapper.selectById(userId));
    }

    private static String displayNameOf(SysUser user) {
        if (user == null) {
            return null;
        }
        return StringUtils.hasText(user.getNickname()) ? user.getNickname() : user.getUsername();
    }
}
