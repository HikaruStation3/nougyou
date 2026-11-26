package com.nougyou.cms.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 新增/编辑扶贫资讯的提交结构（API-029 / API-030）。
 * 发布与下架不走这里，由 API-032 单独控制。
 */
@Data
public class NewsForm {

    private Long id;

    @NotBlank(message = "资讯标题不能为空")
    @Size(max = 200, message = "资讯标题不能超过 200 字")
    private String title;

    /** 1 景点宣传 / 2 旅游推介 / 3 乡镇历史 / 4 地域文化 / 5 人文地理 */
    @NotNull(message = "请选择资讯栏目")
    @Min(value = 1, message = "资讯栏目不正确")
    @Max(value = 5, message = "资讯栏目不正确")
    private Integer newsType;

    @Size(max = 255, message = "封面地址不能超过 255 字")
    private String cover;

    @Size(max = 500, message = "摘要不能超过 500 字")
    private String summary;

    /**
     * 正文按纯文本存储，门户按换行切段展示，不要塞 HTML 标签。
     */
    @NotBlank(message = "资讯正文不能为空")
    private String content;

    /** 关联地点（景点 / 村落） */
    @Size(max = 120, message = "关联地点不能超过 120 字")
    private String location;
}
