package com.nougyou.notice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 通知公告：管理员创建，发布后全站或指定用户可见。
 */
@Data
@TableName("notice")
public class Notice {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    /** 富文本 HTML，前端用 v-html 渲染 */
    private String content;

    /** 1 通知 / 2 公告 / 3 提醒 */
    private Integer type;

    /** L 普通 / M 重要 / H 紧急 */
    private String level;

    /** 1 全体用户 / 2 指定用户 */
    private Integer targetType;

    /** 0 未发布 / 1 已发布 / -1 已撤回 */
    private Integer publishStatus;

    private Long publisherId;
    private LocalDateTime publishTime;
    private LocalDateTime revokeTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
