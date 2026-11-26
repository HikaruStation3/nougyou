package com.nougyou.aftersale.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nougyou.aftersale.entity.AfterSale;
import org.apache.ibatis.annotations.Mapper;

/**
 * 售后单基础 CRUD。
 */
@Mapper
public interface AfterSaleMapper extends BaseMapper<AfterSale> {
}
