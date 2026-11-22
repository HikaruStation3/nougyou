package com.nougyou.product.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 农产品分类，parentId=0 为一级分类。
 */
@Data
@TableName("product_category")
public class ProductCategory {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    /** 父级ID，0 为一级分类 */
    private Long parentId;

    private String icon;

    private Integer sort;

    /** 状态：1启用 0禁用 */
    private Integer status;
}
