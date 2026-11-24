package com.nougyou.home.dto;

import lombok.Data;

/**
 * 库存预警查询的行对象，仅在服务端内部使用，避免 MyBatis 直接映射 String 类型的商品 ID。
 */
@Data
public class StockWarnRow {

    private Long productId;
    private String name;
    private Integer stock;
    private Integer shelfStatus;
}
