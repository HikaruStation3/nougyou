package com.nougyou.order.mapper;

import com.nougyou.order.dto.row.ActiveEntrustRow;
import com.nougyou.order.dto.row.AfterSaleStatusRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 订单模块对其他表的只读 / 定点写入操作。
 * 库存与销量直接走原子 UPDATE，避免并发下单超卖。
 */
@Mapper
public interface OrderReferenceMapper {

    /**
     * 下单预占库存：只有库存充足时才会更新成功，返回 0 表示库存不足。
     */
    @Update("UPDATE product SET stock = stock - #{quantity} "
            + "WHERE id = #{productId} AND deleted = 0 AND stock >= #{quantity}")
    int deductStock(@Param("productId") Long productId, @Param("quantity") int quantity);

    /**
     * 取消订单时回滚库存。
     */
    @Update("UPDATE product SET stock = stock + #{quantity} WHERE id = #{productId}")
    int restoreStock(@Param("productId") Long productId, @Param("quantity") int quantity);

    /**
     * 发货时累加销量（库存在下单时已预占，这里不再重复扣减）。
     */
    @Update("UPDATE product SET sale_count = sale_count + #{quantity} WHERE id = #{productId}")
    int increaseSaleCount(@Param("productId") Long productId, @Param("quantity") int quantity);

    /**
     * 商品唯一生效中的委托关系，用于把代销订单归属到受托方（与种子订单语义一致）。
     * 若历史数据中同一商品存在多条生效关系，则不猜测归属，订单按商品所属商家自营处理。
     */
    @Select("SELECT id, agent_user_id AS agentUserId FROM entrust_sale "
            + "WHERE product_id = #{productId} AND status = 1 "
            + "AND (SELECT COUNT(*) FROM entrust_sale x WHERE x.product_id = #{productId} AND x.status = 1) = 1 "
            + "ORDER BY id DESC LIMIT 1")
    ActiveEntrustRow findSingleActiveEntrust(@Param("productId") Long productId);

    /**
     * 批量查询订单明细的售后状态，同一明细取最新一条记录。
     */
    @Select("<script>"
            + "SELECT order_item_id AS orderItemId, status FROM after_sale "
            + "WHERE order_item_id IN "
            + "<foreach collection='itemIds' item='itemId' open='(' separator=',' close=')'>#{itemId}</foreach> "
            + "ORDER BY id"
            + "</script>")
    List<AfterSaleStatusRow> findAfterSaleStatuses(@Param("itemIds") List<Long> itemIds);
}
