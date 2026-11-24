package com.nougyou.home.mapper;

import com.nougyou.home.dto.StockWarnRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 商家工作台只读聚合查询（API-112）。
 * 售后、视频、委托属于其他模块，此处只做统计，不引入对应实体，避免模块间循环依赖。
 * 所有金额口径统一为「已支付且未退款」的订单实付金额（pay_status = 1）。
 */
@Mapper
public interface DashboardMapper {

    @Select("SELECT COUNT(1) FROM product WHERE farmer_id = #{farmerId} AND deleted = 0")
    int countProducts(@Param("farmerId") Long farmerId);

    @Select("SELECT COUNT(1) FROM product WHERE farmer_id = #{farmerId} AND deleted = 0 AND shelf_status = 1")
    int countOnSale(@Param("farmerId") Long farmerId);

    @Select("SELECT COUNT(1) FROM product WHERE farmer_id = #{farmerId} AND deleted = 0 AND shelf_status = 0")
    int countOffSale(@Param("farmerId") Long farmerId);

    @Select("SELECT COUNT(1) FROM product "
            + "WHERE farmer_id = #{farmerId} AND deleted = 0 AND stock < #{threshold}")
    int countStockWarn(@Param("farmerId") Long farmerId, @Param("threshold") int threshold);

    @Select("SELECT id AS productId, name, stock, shelf_status "
            + "FROM product "
            + "WHERE farmer_id = #{farmerId} AND deleted = 0 AND stock < #{threshold} "
            + "ORDER BY shelf_status DESC, stock ASC, id ASC LIMIT #{limit}")
    List<StockWarnRow> stockWarnList(@Param("farmerId") Long farmerId,
                                     @Param("threshold") int threshold,
                                     @Param("limit") int limit);

    @Select("SELECT COUNT(1) FROM orders "
            + "WHERE farmer_id = #{farmerId} AND deleted = 0 AND order_status = #{orderStatus}")
    int countOrders(@Param("farmerId") Long farmerId, @Param("orderStatus") int orderStatus);

    @Select("SELECT COALESCE(SUM(pay_amount), 0) FROM orders "
            + "WHERE farmer_id = #{farmerId} AND deleted = 0 AND pay_status = 1 "
            + "AND order_status <> 4 "
            + "AND pay_time >= #{start} AND pay_time < #{end}")
    BigDecimal sumPaidAmount(@Param("farmerId") Long farmerId,
                             @Param("start") LocalDateTime start,
                             @Param("end") LocalDateTime end);

    @Select("SELECT COUNT(1) FROM after_sale WHERE farmer_id = #{farmerId} AND status = 0")
    int countAfterSalePending(@Param("farmerId") Long farmerId);

    @Select("SELECT COUNT(1) FROM video "
            + "WHERE farmer_id = #{farmerId} AND deleted = 0 AND audit_status = #{auditStatus}")
    int countVideos(@Param("farmerId") Long farmerId, @Param("auditStatus") int auditStatus);

    @Select("SELECT COUNT(1) FROM entrust_sale "
            + "WHERE status = 1 AND (entrust_user_id = #{userId} OR agent_user_id = #{userId})")
    int countActiveEntrusts(@Param("userId") Long userId);
}
