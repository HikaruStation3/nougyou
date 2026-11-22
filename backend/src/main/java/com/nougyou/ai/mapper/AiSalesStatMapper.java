package com.nougyou.ai.mapper;

import com.nougyou.ai.dto.row.ProductMonthSaleRow;
import com.nougyou.ai.dto.row.ProductRefundRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 销售统计聚合查询。口径：
 * <ul>
 *   <li>销量/销售额：已支付且未取消订单（pay_status = 1 且 order_status != 4），按下单时间归月</li>
 *   <li>退单：售后单已完成（status = 2），按售后发起时间归期，取订单明细件数</li>
 * </ul>
 */
@Mapper
public interface AiSalesStatMapper {

    @Select("""
            <script>
            SELECT i.product_id            AS productId,
                   DATE_FORMAT(o.create_time, '%Y-%m') AS month,
                   SUM(i.quantity)         AS qty,
                   SUM(i.subtotal)         AS amount
            FROM order_item i
            JOIN orders o ON o.id = i.order_id
            WHERE o.farmer_id = #{farmerId}
              AND o.deleted = 0
              AND o.pay_status = 1
              AND o.order_status &lt;&gt; 4
              AND o.create_time &gt;= #{start}
              AND o.create_time &lt; #{end}
              <if test="productId != null">AND i.product_id = #{productId}</if>
            GROUP BY i.product_id, month
            ORDER BY i.product_id, month
            </script>
            """)
    List<ProductMonthSaleRow> selectProductMonthSales(@Param("farmerId") Long farmerId,
                                                      @Param("productId") Long productId,
                                                      @Param("start") LocalDateTime start,
                                                      @Param("end") LocalDateTime end);

    @Select("""
            <script>
            SELECT i.product_id                              AS productId,
                   SUM(i.quantity)                           AS refundQty,
                   SUM(a.refund_amount)                      AS refundAmount
            FROM after_sale a
            JOIN order_item i ON i.id = a.order_item_id
            WHERE a.farmer_id = #{farmerId}
              AND a.status = 2
              AND a.create_time &gt;= #{start}
              AND a.create_time &lt; #{end}
              <if test="productId != null">AND i.product_id = #{productId}</if>
            GROUP BY i.product_id
            </script>
            """)
    List<ProductRefundRow> selectProductRefunds(@Param("farmerId") Long farmerId,
                                                @Param("productId") Long productId,
                                                @Param("start") LocalDateTime start,
                                                @Param("end") LocalDateTime end);
}
