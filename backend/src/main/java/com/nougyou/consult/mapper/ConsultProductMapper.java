package com.nougyou.consult.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 商品只读查询：会话列表里展示「咨询的是哪件商品」。
 */
@Mapper
public interface ConsultProductMapper {

    @Select("select name from product where id = #{id} and deleted = 0")
    String selectName(@Param("id") Long id);
}
