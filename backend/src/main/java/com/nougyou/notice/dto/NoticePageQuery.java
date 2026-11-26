package com.nougyou.notice.dto;

import lombok.Data;

/**
 * 通知分页查询条件：管理端按标题/状态筛选，「我的通知」按已读状态筛选。
 */
@Data
public class NoticePageQuery {

    private long pageNum = 1;
    private long pageSize = 10;

    /** 标题模糊匹配（管理端） */
    private String title;

    /** 发布状态：0 未发布 / 1 已发布 / -1 已撤回（管理端） */
    private Integer publishStatus;

    /** 是否已读：1 已读 / 0 未读（我的通知） */
    private Integer isRead;
}
