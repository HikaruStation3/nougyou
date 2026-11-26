package com.nougyou.aftersale.dto;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * 售后列表查询条件，买家（API-062）、商家（API-064）、管理员共用。
 */
@Data
public class AfterSalePageQuery {

    private long pageNum = 1;
    private long pageSize = 10;

    /** 0 待处理 / 1 处理中 / 2 已完成 / 3 已驳回 */
    private Integer status;

    /** 售后单号、订单号或商品名模糊匹配 */
    private String keyword;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;
}
