package com.nougyou.home.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 商家工作台：库存预警条目（API-112）。
 */
@Data
@Builder
public class StockWarnVO {

    private String productId;
    private String name;
    private Integer stock;
    /** 上架状态：0 下架 / 1 上架 */
    private Integer shelfStatus;
}
