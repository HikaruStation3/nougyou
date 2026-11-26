package com.nougyou.aftersale.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;

/**
 * 售后模块对其他表的定点操作：退货退款完成时回滚库存、统计订单已退金额。
 */
@Mapper
public interface AfterSaleReferenceMapper {

    /**
     * 退货退款完成后把数量退回库存（仅退款不回滚，商品仍在买家手里）。
     */
    @Update("UPDATE product SET stock = stock + #{quantity} WHERE id = #{productId}")
    int restoreStock(@Param("productId") Long productId, @Param("quantity") int quantity);

    /**
     * 订单已完成售后的退款总额，用于判断整单是否已全额退款。
     */
    @Select("SELECT COALESCE(SUM(refund_amount), 0) FROM after_sale "
            + "WHERE order_id = #{orderId} AND status = 2")
    BigDecimal sumRefunded(@Param("orderId") Long orderId);
}
