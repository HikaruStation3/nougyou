package com.nougyou.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nougyou.product.entity.Product;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ProductMapper extends BaseMapper<Product> {
}
