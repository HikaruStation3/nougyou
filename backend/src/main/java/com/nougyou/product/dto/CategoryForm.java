package com.nougyou.product.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 农产品分类表单，管理端新增与编辑共用。
 */
@Data
public class CategoryForm {

    @Size(max = 50, message = "分类名称不能超过 50 个字符")
    private String name;

    /** 父级 ID，0 表示一级分类 */
    private Long parentId;

    @Size(max = 255, message = "分类图标地址过长")
    private String icon;

    private Integer sort;

    /** 状态：1 启用 / 0 禁用 */
    private Integer status;
}
