package com.nougyou.entrust.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 委托销售结算单（FR-024 / FR-025），按月唯一。
 */
@Data
@TableName("entrust_settlement")
public class EntrustSettlement {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long entrustId;
    /** 结算月份 yyyy-MM */
    private String settleMonth;
    private Integer saleQty;
    private BigDecimal saleAmount;
    private Integer refundQty;
    private BigDecimal refundAmount;
    /** 委托方应得 */
    private BigDecimal entrustIncome;
    /** 受托方应得 */
    private BigDecimal agentIncome;
    /** 0 待结算 / 1 已结算 */
    private Integer status;
    private LocalDateTime settleTime;
    private LocalDateTime createTime;
}
