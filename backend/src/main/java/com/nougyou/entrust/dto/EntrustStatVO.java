package com.nougyou.entrust.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 委托销售数据统计（API-093），数据源为视图 v_entrust_sale_stat，
 * 委托方与受托方看到完全一致的数据。
 */
@Data
public class EntrustStatVO {

    private Long entrustId;
    private String entrustNo;
    private Long entrustUserId;
    private String entrustUserName;
    private Long agentUserId;
    private String agentUserName;
    private Long productId;
    private String productName;
    private Integer stock;
    private Integer totalSaleCount;
    private BigDecimal splitRatio;
    private Integer entrustStatus;
    private Integer saleQty;
    private BigDecimal saleAmount;
    private Integer refundQty;
    private BigDecimal refundAmount;
    private BigDecimal entrustIncome;
    private BigDecimal agentIncome;
}
