package com.nougyou.entrust.dto.row;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 某个月的代销 / 退款汇总，用于生成结算单。
 */
@Data
public class SettlementSumRow {

    private Integer qty;
    private BigDecimal amount;
}
