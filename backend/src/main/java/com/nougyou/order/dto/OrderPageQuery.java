package com.nougyou.order.dto;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * 订单列表查询条件，买家（API-053）、商家（API-058）、管理员（API-060）共用。
 */
@Data
public class OrderPageQuery {

    private long pageNum = 1;
    private long pageSize = 10;

    /** 订单状态：0 待付款 / 1 待发货 / 2 待收货 / 3 已完成 / 4 已取消 / 5 售后中 */
    private Integer orderStatus;

    /** 买家端匹配订单号或商品名，商家端匹配订单号或买家姓名 */
    private String keyword;

    /** 管理员精确匹配订单号 */
    private String orderNo;

    /** 管理员筛选买家 */
    private Long buyerId;

    /** 管理员筛选商家 */
    private Long farmerId;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;
}
