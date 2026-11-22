package com.nougyou.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ai_content_rule")
public class AiContentRule {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String ruleName;
    /** 1 敏感词 / 2 业务边界 / 3 输出格式约束 */
    private Integer ruleType;
    /** 关键词，英文逗号分隔 */
    private String keywords;
    /** 命中动作 1 拦截 / 2 改写 / 3 提示 */
    private Integer action;
    private String remark;
    private Integer isEnable;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
