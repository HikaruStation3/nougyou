package com.nougyou.notice.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 通知列表项：管理端表格与顶部铃铛共用。
 */
@Data
@Builder
public class NoticeVO {

    private Long id;
    private String title;
    private String content;
    private Integer type;
    private String level;

    /** 1 全体 / 2 指定 */
    private Integer targetType;

    /** 0 未发布 / 1 已发布 / -1 已撤回 */
    private Integer publishStatus;

    /** 1 已读 / 0 未读，管理端为 null */
    private Integer isRead;

    private String publisherName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime publishTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime revokeTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
