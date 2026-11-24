package com.nougyou.cms.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 扶贫政策（FR-003 浏览 / FR-030 发布）。
 * 内容资讯模块尚未开放接口，当前仅提供首页聚合所需的只读数据。
 */
@Data
@TableName("cms_policy")
public class CmsPolicy {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    /** 政策文号 */
    private String policyNo;
    /** 发布单位 */
    private String issueDept;
    private String cover;
    private String summary;
    private String content;
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
