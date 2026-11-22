package com.nougyou.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 商品分类只读视图。
 */
@Data
@TableName("product_category")
public class CategoryRef {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
}
