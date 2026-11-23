package com.nougyou.entrust.mapper;

import com.nougyou.entrust.dto.EntrustStatVO;
import com.nougyou.entrust.dto.row.SettlementSumRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 委托统计相关查询：实时统计走视图，结算单按月汇总订单与退款。
 */
@Mapper
public interface EntrustReferenceMapper {

    /**
     * API-093 委托销售统计，数据源为 v_entrust_sale_stat。
     */
    @Select("SELECT * FROM v_entrust_sale_stat WHERE entrust_id = #{entrustId}")
    EntrustStatVO findStat(@Param("entrustId") Long entrustId);

    /**
     * 指定月份的代销销量与销售额：已支付且未取消的订单。
     */
    @Select("SELECT IFNULL(SUM(oi.quantity), 0) AS qty, IFNULL(SUM(oi.subtotal), 0) AS amount "
            + "FROM order_item oi JOIN orders o ON o.id = oi.order_id "
            + "WHERE oi.entrust_id = #{entrustId} AND o.pay_status = 1 AND o.order_status <> 4 "
            + "AND DATE_FORMAT(o.create_time, '%Y-%m') = #{month}")
    SettlementSumRow sumMonthSale(@Param("entrustId") Long entrustId, @Param("month") String month);

    /**
     * 指定月份的退单件数与退款金额：已完成（status=2）的售后单。
     */
    @Select("SELECT IFNULL(SUM(oi.quantity), 0) AS qty, IFNULL(SUM(a.refund_amount), 0) AS amount "
            + "FROM after_sale a JOIN order_item oi ON oi.id = a.order_item_id "
            + "WHERE oi.entrust_id = #{entrustId} AND a.status = 2 "
            + "AND DATE_FORMAT(a.create_time, '%Y-%m') = #{month}")
    SettlementSumRow sumMonthRefund(@Param("entrustId") Long entrustId, @Param("month") String month);
}
