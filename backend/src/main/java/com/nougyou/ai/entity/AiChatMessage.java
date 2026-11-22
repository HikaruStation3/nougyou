package com.nougyou.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ai_chat_message")
public class AiChatMessage {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long sessionId;
    /** user / assistant / system */
    private String msgRole;
    private String content;
    private Integer tokenCount;
    private Integer isFlagged;
    private LocalDateTime createTime;
}
