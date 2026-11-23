package com.nougyou.order.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单出参（API-052 ~ API-060）。
 */
@Data
@Builder
public class OrderVO {

    private String id;
    private String orderNo;
    private String buyerId;
    private String buyerName;
    private String farmerId;
    private String farmerName;
    private String shopName;
    private BigDecimal totalAmount;
    private BigDecimal freight;
    private BigDecimal payAmount;
    /** 0 未支付 / 1 已支付 / 2 已退款 */
    private Integer payStatus;
    /** 1 微信 / 2 支付宝 / 3 模拟支付 */
    private Integer payType;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime payTime;
    /** 0 待付款 / 1 待发货 / 2 待收货 / 3 已完成 / 4 已取消 / 5 售后中 */
    private Integer orderStatus;
    private String receiverName;
    /** 收货电话，列表脱敏、详情明文 */
    private String receiverPhone;
    private String receiverAddress;
    private String remark;
    private String logisticsCompany;
    private String logisticsNo;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime shipTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime finishTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    private List<OrderItemVO> items;
    /** 当前是否可申请售后 */
    private Boolean afterSaleAllowed;
}
