package com.nougyou.notice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 通知指定接收人，仅 target_type = 2 时写入。
 */
@Data
@TableName("notice_target")
public class NoticeTarget {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long noticeId;
    private Long userId;
}
