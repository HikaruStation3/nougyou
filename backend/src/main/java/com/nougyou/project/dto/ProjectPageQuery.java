package com.nougyou.project.dto;

import lombok.Data;

@Data
public class ProjectPageQuery {

    private long pageNum = 1;
    private long pageSize = 10;

    /** 项目类型，公开列表默认 农家乐 */
    private String projectType;

    /** 模糊匹配地址 */
    private String address;

    /** 匹配项目名称 */
    private String keyword;

    /** 审核状态：0待审核 1通过 2驳回（仅我的/管理员列表使用） */
    private Integer auditStatus;
}
