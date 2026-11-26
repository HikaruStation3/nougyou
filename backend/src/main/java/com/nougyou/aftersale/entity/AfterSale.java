package com.nougyou.aftersale.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 售后诉求表，对应 after_sale（FR-008 买家提交 / FR-019 商家与平台处理）。
 *
 * 一条售后记录针对订单里的一个明细，买家、商家、管理员三方共用同一份进度。
 */
@Data
@TableName("after_sale")
public class AfterSale {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String afterSaleNo;
    private Long orderId;
    private Long orderItemId;
    private Long buyerId;
    private Long farmerId;
    /** 1 仅退款 / 2 退货退款 / 3 换货 / 4 咨询 */
    private Integer saleType;
    private String reason;
    private String description;
    /** 凭证图片，逗号分隔；出参拆成数组 */
    private String images;
    private BigDecimal refundAmount;
    /** 0 待处理 / 1 处理中 / 2 已完成 / 3 已驳回 */
    private Integer status;
    private Long handleUserId;
    private LocalDateTime handleTime;
    private String handleRemark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
