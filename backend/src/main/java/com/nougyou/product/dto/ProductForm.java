package com.nougyou.product.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 农产品表单，上架（API-043）与编辑（API-044）共用。
 * 上架时必填字段由 Service 校验；编辑时仅校验已提供字段的格式。
 */
@Data
public class ProductForm {

    /** 分类 ID，须为启用的分类 */
    private Long categoryId;

    @Size(min = 2, max = 50, message = "商品名称长度需在 2-50 个字符之间")
    private String name;

    @Size(max = 255, message = "封面图地址过长")
    private String cover;

    @Size(max = 9, message = "图片最多 9 张")
    private List<String> images;

    @DecimalMin(value = "0.01", message = "售价必须大于 0")
    private BigDecimal price;

    @DecimalMin(value = "0.00", message = "原价不能为负数")
    private BigDecimal originalPrice;

    @Min(value = 0, message = "库存不能为负数")
    private Integer stock;

    @Size(max = 10, message = "计量单位不能超过 10 个字符")
    private String unit;

    @DecimalMin(value = "0.00", message = "单件重量不能为负数")
    private BigDecimal weight;

    @Size(max = 100, message = "产地不能超过 100 个字符")
    private String origin;

    /** 是否扶贫产品：0 否 / 1 是 */
    private Integer isPoverty;

    @Size(max = 20000, message = "商品详情过长")
    private String description;

    /** 上架状态：0 下架 / 1 上架，默认 1 */
    private Integer shelfStatus;
}
