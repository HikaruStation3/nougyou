package com.nougyou.entrust.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 委托结算单出参（API-094 / API-095）。
 */
@Data
@Builder
public class SettlementVO {

    private String id;
    private String entrustId;
    private String entrustNo;
    private String productName;
    /** yyyy-MM */
    private String settleMonth;
    private Integer saleQty;
    private BigDecimal saleAmount;
    private Integer refundQty;
    private BigDecimal refundAmount;
    private BigDecimal entrustIncome;
    private BigDecimal agentIncome;
    /** 0 待结算 / 1 已结算 */
    private Integer status;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime settleTime;
}
