package com.nougyou.product.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 农产品列表项（API-041）。
 */
@Data
@Builder
public class ProductVO {

    private String id;
    private String farmerId;
    /** 商家姓名 */
    private String farmerName;
    /** 店铺名称 */
    private String shopName;
    private String categoryId;
    /** 分类名称 */
    private String categoryName;
    private String name;
    private String cover;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private Integer stock;
    private String unit;
    private BigDecimal weight;
    private String origin;
    /** 是否扶贫产品：0 否 / 1 是 */
    private Integer isPoverty;
    private Integer saleCount;
    /** 上架状态：0 下架 / 1 上架 */
    private Integer shelfStatus;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
