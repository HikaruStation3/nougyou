package com.nougyou.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ai_prompt_config")
public class AiPromptConfig {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String sceneCode;
    private String sceneName;
    private String promptContent;
    private Integer version;
    private Integer isEnable;
    private Long updateUserId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
