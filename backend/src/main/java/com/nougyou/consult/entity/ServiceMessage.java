package com.nougyou.consult.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 会话消息（文本/图片）。
 */
@Data
@TableName("service_message")
public class ServiceMessage {

    public static final int TYPE_TEXT = 1;
    public static final int TYPE_IMAGE = 2;

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long sessionId;
    private Long senderId;
    /** BUYER / FARMER / ADMIN */
    private String senderRole;
    /** 1 文本 2 图片 */
    private Integer contentType;
    private String content;
    /** 0 未读 1 已读 */
    private Integer isRead;
    private LocalDateTime createTime;
}
