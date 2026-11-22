package com.nougyou.consult.dto.row;

import lombok.Data;

/**
 * 会话最后一条消息聚合行。
 */
@Data
public class SessionLastMessageRow {

    private Long sessionId;
    private String content;
    private Integer contentType;
}
