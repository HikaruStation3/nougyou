package com.nougyou.entrust.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 委托销售关系（FR-022 / FR-023）。
 * 委托方把商品交给同村受托人代销，受托方确认后关系生效。
 */
@Data
@TableName("entrust_sale")
public class EntrustSale {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String entrustNo;
    /** 委托方用户 ID */
    private Long entrustUserId;
    /** 受托方用户 ID */
    private Long agentUserId;
    private Long productId;
    /** 受托方分成比例（百分数） */
    private BigDecimal splitRatio;
    /** 结算周期：1 按月 / 2 按季 / 3 按单 */
    private Integer settleCycle;
    private String agreementDesc;
    private LocalDate startDate;
    private LocalDate endDate;
    /** 0 待确认 / 1 生效中 / 2 已终止 / 3 已拒绝 */
    private Integer status;
    private LocalDateTime confirmTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
