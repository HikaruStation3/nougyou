package com.nougyou.ai.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.nougyou.ai.entity.AiContentRule;
import com.nougyou.ai.mapper.AiContentRuleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 内容管控（FR-038）：对模型输出做关键词校验。
 * <p>
 * 当前阶段只处理关键词类规则（rule_type 1 敏感词 / 2 业务边界），
 * 输出格式约束（rule_type 3 正则）暂不参与，避免误拦截整段回复。
 */
@Service
@RequiredArgsConstructor
public class AiContentGuardService {

    /** 拦截时的兜底回复 */
    private static final String BLOCKED_REPLY = "抱歉，这个问题超出平台服务范围，建议联系平台客服进一步咨询。";
    private static final String HINT_SUFFIX = "\n\n（提示：本平台仅支持站内业务咨询，请勿引导站外交易。）";

    private final AiContentRuleMapper contentRuleMapper;

    public GuardResult inspect(String content) {
        if (!StringUtils.hasText(content)) {
            return new GuardResult(content, false, false);
        }

        List<AiContentRule> rules = contentRuleMapper.selectList(Wrappers.<AiContentRule>lambdaQuery()
                .eq(AiContentRule::getIsEnable, 1)
                .in(AiContentRule::getRuleType, 1, 2)
                .orderByAsc(AiContentRule::getId));

        String result = content;
        boolean flagged = false;

        for (AiContentRule rule : rules) {
            List<String> keywords = splitKeywords(rule.getKeywords());
            if (keywords.isEmpty()) {
                continue;
            }

            boolean hit = keywords.stream().anyMatch(result::contains);
            if (!hit) {
                continue;
            }

            flagged = true;
            int action = rule.getAction() == null ? 3 : rule.getAction();

            if (action == 1) {
                return new GuardResult(BLOCKED_REPLY, true, true);
            }
            if (action == 2) {
                for (String keyword : keywords) {
                    result = result.replace(keyword, "***");
                }
            } else if (!result.endsWith(HINT_SUFFIX)) {
                result = result + HINT_SUFFIX;
            }
        }

        return new GuardResult(result, false, flagged);
    }

    private List<String> splitKeywords(String keywords) {
        if (!StringUtils.hasText(keywords) || keywords.contains("示例")) {
            return List.of();
        }
        return java.util.Arrays.stream(keywords.split("[,，]"))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
    }

    /**
     * @param content 处理后的内容
     * @param blocked 是否命中拦截规则（内容已被替换为兜底回复）
     * @param flagged 是否命中任意管控规则
     */
    public record GuardResult(String content, boolean blocked, boolean flagged) {
    }
}
