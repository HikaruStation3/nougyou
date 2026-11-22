package com.nougyou.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "nougyou.ai")
public class AiProperties {

    /** 模型标识，仅用于会话记录展示 */
    private String modelName = "glm-4-flash";
    /** 携带给模型的历史消息条数 */
    private int historySize = 10;
    /** SSE 超时时间（毫秒） */
    private long timeoutMs = 180_000L;
    /** 单次提问最大字数 */
    private int maxQuestionLength = 1000;
}
