package com.nougyou.cms.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 扶贫资讯列表项（API-027），不含正文。
 */
@Data
@Builder
public class NewsVO {

    private Long id;
    private String title;
    /** 1 景点宣传 / 2 旅游推介 / 3 乡镇历史 / 4 地域文化 / 5 人文地理 */
    private Integer newsType;
    private String newsTypeName;
    private String cover;
    private String summary;
    private String location;
    private Integer publishStatus;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime publishTime;
    private Integer viewCount;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
