package com.nougyou.home.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 商家经营概览（API-112 / FR-014）。
 * 全部字段为只读统计，数据范围限定为当前商家本店。
 */
@Data
@Builder
public class FarmerDashboardVO {

    /** 商品总数 */
    private Integer productTotal;
    /** 在售商品数 */
    private Integer onSaleCount;
    /** 已下架商品数 */
    private Integer offSaleCount;
    /** 库存低于 10 件的商品数 */
    private Integer stockWarnCount;
    /** 待发货订单数 */
    private Integer orderWaitShip;
    /** 待收货订单数 */
    private Integer orderWaitReceive;
    /** 已完成订单数 */
    private Integer orderFinished;
    /** 今日成交额 */
    private BigDecimal todayAmount;
    /** 本月成交额 */
    private BigDecimal monthAmount;
    /** 待处理售后数 */
    private Integer afterSalePending;
    /** 审核中的视频数 */
    private Integer videoAuditPending;
    /** 被驳回的视频数 */
    private Integer videoRejected;
    /** 参与中的委托关系数（含委托出去与代别人卖） */
    private Integer entrustCount;
    /** 库存预警清单，最多 5 条 */
    private List<StockWarnVO> stockWarnList;
}
