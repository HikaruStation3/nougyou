package com.nougyou.order.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 订单明细出参（OrderVO.items）。
 */
@Data
@Builder
public class OrderItemVO {

    private String id;
    private String productId;
    private String productName;
    private String productCover;
    private BigDecimal price;
    private Integer quantity;
    private BigDecimal subtotal;
    /** 委托关系 ID，自营商品为 null */
    private String entrustId;
    /** 该明细的售后状态，无售后为 null */
    private Integer afterSaleStatus;
}
