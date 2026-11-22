package com.nougyou.ai.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nougyou.ai.dto.AiOverviewVO;
import com.nougyou.ai.dto.ChatMessageVO;
import com.nougyou.ai.dto.ChatSessionVO;
import com.nougyou.ai.entity.AiChatMessage;
import com.nougyou.ai.entity.AiChatSession;
import com.nougyou.ai.enums.AiScene;
import com.nougyou.ai.mapper.AiChatMessageMapper;
import com.nougyou.ai.mapper.AiChatSessionMapper;
import com.nougyou.common.api.ResultCode;
import com.nougyou.common.exception.BusinessException;
import com.nougyou.common.page.PageResult;
import com.nougyou.system.entity.SysUser;
import com.nougyou.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 管理员侧 AI 对话监控（FR-036）。
 */
@Service
@RequiredArgsConstructor
public class AiAdminService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final AiChatSessionMapper sessionMapper;
    private final AiChatMessageMapper messageMapper;
    private final AiConversationStore conversationStore;
    private final AiPromptService promptService;
    private final SysUserMapper userMapper;

    public PageResult<ChatSessionVO> pageSessions(Long userId, String sceneCode, String startTime,
                                                  String endTime, String keyword,
                                                  Integer pageNum, Integer pageSize) {
        AiScene scene = StringUtils.hasText(sceneCode) ? AiScene.of(sceneCode) : null;

        Page<AiChatSession> page = new Page<>(pageNum(pageNum), pageSize(pageSize));
        Page<AiChatSession> result = sessionMapper.selectPage(page, Wrappers.<AiChatSession>lambdaQuery()
                .eq(userId != null, AiChatSession::getUserId, userId)
                .eq(scene != null, AiChatSession::getSceneCode, scene == null ? null : scene.name())
                .like(StringUtils.hasText(keyword), AiChatSession::getTitle, keyword)
                .ge(StringUtils.hasText(startTime), AiChatSession::getCreateTime, parseStart(startTime))
                .le(StringUtils.hasText(endTime), AiChatSession::getCreateTime, parseEnd(endTime))
                .orderByDesc(AiChatSession::getUpdateTime)
                .orderByDesc(AiChatSession::getId));

        Map<Long, String> userNames = resolveUserNames(result.getRecords().stream()
                .map(AiChatSession::getUserId).distinct().toList());

        List<ChatSessionVO> list = result.getRecords().stream()
                .map(item -> ChatSessionVO.of(
                        item,
                        promptService.sceneName(AiScene.of(item.getSceneCode())),
                        userNames.get(item.getUserId())))
                .toList();
        return new PageResult<>(list, result.getTotal());
    }

    /**
     * 管理员可查看任意会话的全文，命中管控规则的消息由 isFlagged 回传前端标红。
     */
    public PageResult<ChatMessageVO> listMessages(Long sessionId, Integer pageNum, Integer pageSize) {
        conversationStore.requireSession(sessionId);
        Page<AiChatMessage> page = new Page<>(pageNum(pageNum), pageSize(pageSize));
        Page<AiChatMessage> result = messageMapper.selectPage(page, Wrappers.<AiChatMessage>lambdaQuery()
                .eq(AiChatMessage::getSessionId, sessionId)
                .orderByAsc(AiChatMessage::getId));
        List<ChatMessageVO> list = result.getRecords().stream().map(ChatMessageVO::of).toList();
        return new PageResult<>(list, result.getTotal());
    }

    public AiOverviewVO overview() {
        long sessionCount = nullSafe(sessionMapper.selectCount(null));
        long messageCount = nullSafe(messageMapper.selectCount(null));
        long flaggedCount = nullSafe(messageMapper.selectCount(Wrappers.<AiChatMessage>lambdaQuery()
                .eq(AiChatMessage::getIsFlagged, 1)));
        long todaySessions = nullSafe(sessionMapper.selectCount(Wrappers.<AiChatSession>lambdaQuery()
                .ge(AiChatSession::getCreateTime, LocalDate.now().atStartOfDay())));
        long activeUsers = sessionMapper.selectList(Wrappers.<AiChatSession>lambdaQuery()
                        .select(AiChatSession::getUserId))
                .stream().map(AiChatSession::getUserId).distinct().count();
        return new AiOverviewVO(sessionCount, messageCount, flaggedCount, todaySessions, activeUsers);
    }

    private Map<Long, String> resolveUserNames(List<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return userMapper.selectList(Wrappers.<SysUser>lambdaQuery().in(SysUser::getId, userIds))
                .stream()
                .collect(Collectors.toMap(SysUser::getId,
                        user -> StringUtils.hasText(user.getNickname()) ? user.getNickname() : user.getUsername(),
                        (left, right) -> left));
    }

    private static long nullSafe(Long value) {
        return value == null ? 0L : value;
    }

    private static LocalDateTime parseStart(String value) {
        return parse(value, true);
    }

    private static LocalDateTime parseEnd(String value) {
        return parse(value, false);
    }

    /**
     * 兼容 yyyy-MM-dd HH:mm:ss 与 yyyy-MM-dd，只传日期时结束边界补到当天 23:59:59。
     */
    private static LocalDateTime parse(String value, boolean startOfDay) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String text = value.trim();
        try {
            if (text.length() <= 10) {
                LocalDate date = LocalDate.parse(text, DATE_FMT);
                return startOfDay ? date.atStartOfDay() : date.atTime(23, 59, 59);
            }
            return LocalDateTime.parse(text, DATE_TIME_FMT);
        } catch (DateTimeParseException ex) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "时间格式应为 yyyy-MM-dd 或 yyyy-MM-dd HH:mm:ss");
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
}
