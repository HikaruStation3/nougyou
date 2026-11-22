package com.nougyou.ai.dto;

/**
 * 管理员 AI 监控概览（FR-036）。
 *
 * @param sessionCount   会话总数
 * @param messageCount   消息总数
 * @param flaggedCount   命中管控规则的消息数
 * @param todaySessions  今日新增会话
 * @param activeUsers    产生过会话的用户数
 */
public record AiOverviewVO(
        long sessionCount,
        long messageCount,
        long flaggedCount,
        long todaySessions,
        long activeUsers
) {
}
