package com.nougyou.cart.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 购物车行（API-047 ~ API-051）。
 */
@Data
@Builder
public class CartVO {

    private String id;
    private String productId;
    private String productName;
    private String cover;
    /** 当前售价（元） */
    private BigDecimal price;
    /** 当前库存，用于前端限制数量 */
    private Integer stock;
    private String unit;
    private Integer quantity;
    /** 是否勾选：0 否 / 1 是 */
    private Integer selected;
    /** 小计金额 = price × quantity */
    private BigDecimal subtotal;
    /** 商品是否已下架或库存不足，true 时前端置灰 */
    private Boolean invalid;
    /** 所属商家ID，用于按商家分组展示 */
    private String farmerId;
    /** 商家姓名 */
    private String farmerName;
    /** 店铺名称 */
    private String shopName;
}
