package com.nougyou.consult.dto.row;

import lombok.Data;

/**
 * 会话未读数聚合行。
 */
@Data
public class SessionUnreadRow {

    private Long sessionId;
    private Long cnt;
}
