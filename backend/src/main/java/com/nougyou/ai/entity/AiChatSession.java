package com.nougyou.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ai_chat_session")
public class AiChatSession {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String sceneCode;
    private String title;
    private String model;
    private Integer msgCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
