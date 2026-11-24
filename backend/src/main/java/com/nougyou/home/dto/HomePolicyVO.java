package com.nougyou.home.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 首页政策卡片，字段与「内容资讯」模块的 PolicyVO 保持一致（API-111）。
 */
@Data
@Builder
public class HomePolicyVO {

    private String id;
    private String title;
    private String policyNo;
    private String issueDept;
    private String cover;
    private String summary;
    private Integer publishStatus;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime publishTime;
    private Integer viewCount;
    private String authorId;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
