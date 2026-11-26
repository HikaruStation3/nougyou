package com.nougyou.cms.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 扶贫政策详情（API-022），在列表字段基础上补充正文。
 */
@Data
@Builder
public class PolicyDetailVO {

    private Long id;
    private String title;
    private String policyNo;
    private String issueDept;
    private String cover;
    private String summary;
    private Integer publishStatus;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime publishTime;
    private Integer viewCount;
    private Long authorId;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    /** 政策正文（富文本，兼容纯文本换行） */
    private String content;
}
