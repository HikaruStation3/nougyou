package com.nougyou.aftersale.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 售后出参（API-061 ~ API-066）。
 */
@Data
@Builder
public class AfterSaleVO {

    private Long id;
    private String afterSaleNo;
    private Long orderId;
    private String orderNo;
    private Long orderItemId;
    private String productName;
    private String productCover;
    /** 该明细购买数量，退货退款时用于回滚库存与展示 */
    private Integer quantity;
    /** 该明细小计，退款金额上限 */
    private BigDecimal itemSubtotal;
    private Long buyerId;
    private String buyerName;
    private Long farmerId;
    private String farmerName;
    private String shopName;
    /** 1 仅退款 / 2 退货退款 / 3 换货 / 4 咨询 */
    private Integer saleType;
    private String reason;
    private String description;
    private List<String> images;
    private BigDecimal refundAmount;
    /** 0 待处理 / 1 处理中 / 2 已完成 / 3 已驳回 */
    private Integer status;
    private Long handleUserId;
    private String handleUserName;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime handleTime;
    private String handleRemark;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
