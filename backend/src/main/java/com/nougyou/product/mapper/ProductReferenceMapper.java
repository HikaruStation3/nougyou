package com.nougyou.product.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 农产品关联查询：订单明细与委托代销关系。
 * 这两张表归属订单 / 委托销售模块，此处仅做只读判定，避免模块间循环依赖。
 */
@Mapper
public interface ProductReferenceMapper {

    /**
     * 商品是否存在订单明细；存在时不允许删除，只允许下架。
     */
    @Select("SELECT COUNT(1) FROM order_item WHERE product_id = #{productId}")
    long countOrderItems(@Param("productId") Long productId);

    /**
     * 商品是否处于委托代销中（status=1 生效中），命中时返回委托关系 ID。
     */
    @Select("SELECT id FROM entrust_sale WHERE product_id = #{productId} AND status = 1 ORDER BY id DESC LIMIT 1")
    Long findActiveEntrustId(@Param("productId") Long productId);
}
