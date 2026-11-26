package com.nougyou.cms.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 扶贫资讯详情（API-028），在列表字段基础上补充正文。
 */
@Data
@Builder
public class NewsDetailVO {

    private Long id;
    private String title;
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
    /** 资讯正文（富文本，兼容纯文本换行） */
    private String content;
}
