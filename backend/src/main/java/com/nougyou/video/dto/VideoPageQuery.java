package com.nougyou.video.dto;

import lombok.Data;

/**
 * 短视频列表查询条件（API-075）。
 */
@Data
public class VideoPageQuery {

    private long pageNum = 1;

    private long pageSize = 10;

    /** 按商家筛选 */
    private Long farmerId;

    /** 排序方式：hot 按点赞 / new 按发布时间，默认 new */
    private String sortBy = "new";
}
