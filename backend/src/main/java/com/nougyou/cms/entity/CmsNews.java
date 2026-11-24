package com.nougyou.cms.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 扶贫资讯（FR-004 浏览 / FR-030 发布）。
 * 内容资讯模块尚未开放接口，当前仅提供首页聚合所需的只读数据。
 */
@Data
@TableName("cms_news")
public class CmsNews {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    /** 1 景点宣传 / 2 旅游推介 / 3 乡镇历史 / 4 地域文化 / 5 人文地理 */
    private Integer newsType;
    private String cover;
    private String summary;
    private String content;
    /** 关联地点（景点 / 村落） */
    private String location;
    /** 0 草稿 / 1 已发布 / 2 已下架 */
    private Integer publishStatus;
    private LocalDateTime publishTime;
    private Integer viewCount;
    private Long authorId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}
