package com.nougyou.notice.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 通知详情：弹窗展示正文，编辑回显走 {@link NoticeForm}。
 */
@Data
@Builder
public class NoticeDetailVO {

    private Long id;
    private String title;
    private String content;
    private Integer type;
    private String level;
    private Integer publishStatus;
    private String publisherName;

    /** 指定接收人 ID，逗号分隔；全体通知为空 */
    private String targetUserIds;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime publishTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
