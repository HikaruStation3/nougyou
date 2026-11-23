package com.nougyou.entrust.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 建立委托关系入参（API-087）。
 */
@Data
public class EntrustCreateForm {

    @NotNull(message = "受托方不能为空")
    private Long agentUserId;

    @NotNull(message = "被委托商品不能为空")
    private Long productId;

    @NotNull(message = "分成比例不能为空")
    @DecimalMin(value = "0.00", message = "分成比例不能小于 0")
    @DecimalMax(value = "100.00", message = "分成比例不能大于 100")
    private BigDecimal splitRatio;

    /** 1 按月 / 2 按季 / 3 按单 */
    @NotNull(message = "结算周期不能为空")
    @Min(value = 1, message = "结算周期取值为 1-3")
    @Max(value = 3, message = "结算周期取值为 1-3")
    private Integer settleCycle;

    @NotNull(message = "生效开始日期不能为空")
    private LocalDate startDate;

    private LocalDate endDate;

    @Size(max = 500, message = "约定说明最长 500 个字符")
    private String agreementDesc;
}
