package com.nougyou.product.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 农产品分类树节点（API-040）。
 */
@Data
@Builder
public class CategoryVO {

    private String id;
    private String name;
    /** 父级 ID，0 为一级分类 */
    private String parentId;
    private String icon;
    private Integer sort;
    /** 状态：1 启用 / 0 禁用，管理端分类维护使用 */
    private Integer status;
    /** 在售商品数；一级分类为其自身与全部子分类之和 */
    private Integer productCount;
    /** 二级分类列表 */
    private List<CategoryVO> children;
}
