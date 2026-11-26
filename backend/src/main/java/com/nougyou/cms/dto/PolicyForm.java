package com.nougyou.cms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 新增/编辑扶贫政策的提交结构（API-023 / API-024）。
 * 发布与下架不走这里，由 API-026 单独控制。
 */
@Data
public class PolicyForm {

    private Long id;

    @NotBlank(message = "政策标题不能为空")
    @Size(max = 200, message = "政策标题不能超过 200 字")
    private String title;

    /** 政策文号，如 钟政发〔2026〕7号 */
    @Size(max = 60, message = "政策文号不能超过 60 字")
    private String policyNo;

    /** 发布单位 */
    @Size(max = 100, message = "发布单位不能超过 100 字")
    private String issueDept;

    @Size(max = 255, message = "封面地址不能超过 255 字")
    private String cover;

    @Size(max = 500, message = "摘要不能超过 500 字")
    private String summary;

    /**
     * 正文按纯文本存储，门户按换行切段展示，不要塞 HTML 标签。
     */
    @NotBlank(message = "政策正文不能为空")
    private String content;
}
