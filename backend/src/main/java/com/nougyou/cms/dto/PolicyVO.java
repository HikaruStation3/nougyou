package com.nougyou.cms.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 扶贫政策列表项（API-021），不含正文。
 */
@Data
@Builder
public class PolicyVO {

    private Long id;
    private String title;
    private String policyNo;
    private String issueDept;
    private String cover;
    private String summary;
    /** 0 草稿 / 1 已发布 / 2 已下架 */
    private Integer publishStatus;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime publishTime;
    private Integer viewCount;
    private Long authorId;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
