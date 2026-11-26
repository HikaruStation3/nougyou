package com.nougyou.aftersale.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * API-065 / API-066 处理售后。
 *
 * 商家支持 1 受理并退款、3 拒绝；管理员介入支持 1 支持买家退款、3 驳回申请。
 */
@Data
public class AfterSaleHandleForm {

    @NotNull(message = "请选择处理结果")
    @Min(value = 1, message = "处理结果不正确")
    @Max(value = 3, message = "处理结果不正确")
    private Integer handleResult;

    /** 实际退款金额，受理时可用申请金额兜底 */
    private BigDecimal refundAmount;

    @Size(max = 200, message = "处理意见最多 200 字")
    private String handleRemark;
}
