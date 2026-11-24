package com.nougyou.home.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 首页资讯卡片，字段与「内容资讯」模块的 NewsVO 保持一致（API-111）。
 */
@Data
@Builder
public class HomeNewsVO {

    private String id;
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
