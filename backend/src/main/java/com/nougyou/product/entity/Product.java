package com.nougyou.product.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 农产品（FR-006 买家选购 / FR-017 农户商家上下架）。
 */
@Data
@TableName("product")
public class Product {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属农户商家用户ID */
    private Long farmerId;

    /** 分类ID */
    private Long categoryId;

    private String name;

    private String cover;

    /** 商品图集，逗号分隔存储 */
    private String images;

    /** 售价（元） */
    private BigDecimal price;

    /** 原价/划线价 */
    private BigDecimal originalPrice;

    /** 库存 */
    private Integer stock;

    /** 计量单位，如 斤 / 袋 / 盒 */
    private String unit;

    /** 单件重量（kg） */
    private BigDecimal weight;

    /** 产地 */
    private String origin;

    /** 是否扶贫产品：1是 0否 */
    private Integer isPoverty;

    /** 商品详情（富文本 HTML） */
    private String description;

    /** 累计销量 */
    private Integer saleCount;

    /** 上架状态：1已上架 0已下架 */
    private Integer shelfStatus;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
