package com.nougyou.product.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 农产品详情（API-042），在列表字段基础上补充图集、详情与委托关系。
 */
@Data
@Builder
public class ProductDetailVO {

    private String id;
    private String farmerId;
    private String farmerName;
    private String shopName;
    private String categoryId;
    private String categoryName;
    private String name;
    private String cover;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private Integer stock;
    private String unit;
    private BigDecimal weight;
    private String origin;
    private Integer isPoverty;
    private Integer saleCount;
    private Integer shelfStatus;
    private String createTime;
    /** 商品图集地址数组 */
    private List<String> images;
    /** 商品详情（富文本 HTML） */
    private String description;
    /** 若该商品处于委托代销中，返回委托关系 ID，否则为 null */
    private String entrustId;
}
