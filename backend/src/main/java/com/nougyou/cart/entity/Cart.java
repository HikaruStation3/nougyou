package com.nougyou.cart.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 购物车行（FR-006 买家在线选购），同一买家同一商品合并为一行。
 */
@Data
@TableName("cart")
public class Cart {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 买家用户ID */
    private Long buyerId;

    private Long productId;

    /** 购买数量 */
    private Integer quantity;

    /** 是否勾选：1 是 0 否 */
    private Integer selected;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
