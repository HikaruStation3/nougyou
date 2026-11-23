package com.nougyou.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 订单明细，保存下单时的商品快照；entrust_id 不为空表示该明细属于委托代销。
 */
@Data
@TableName("order_item")
public class OrderItem {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    private Long productId;
    /** 委托关系 ID，自营商品为空 */
    private Long entrustId;
    private String productName;
    private String productCover;
    private BigDecimal price;
    private Integer quantity;
    private BigDecimal subtotal;
}
