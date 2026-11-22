package com.nougyou.consult.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 会话消息。
 */
@Data
@Builder
public class ServiceMessageVO {

    private Long id;
    private Long sessionId;
    private Long senderId;
    /** buyer / farmer / admin */
    private String senderRole;
    private String senderName;
    private String senderAvatar;
    /** 1 文本 2 图片 */
    private Integer contentType;
    private String content;
    private Integer isRead;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
