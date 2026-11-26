package com.nougyou.aftersale.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * API-061 提交售后申请。
 */
@Data
public class AfterSaleCreateForm {

    @NotNull(message = "订单不能为空")
    private Long orderId;

    @NotNull(message = "请选择要申请售后的商品")
    private Long orderItemId;

    /** 1 仅退款 / 2 退货退款 / 3 换货 / 4 咨询 */
    @NotNull(message = "请选择售后类型")
    @Min(value = 1, message = "售后类型不正确")
    @Max(value = 4, message = "售后类型不正确")
    private Integer saleType;

    @Size(max = 100, message = "申请原因最多 100 字")
    private String reason;

    @Size(max = 500, message = "问题描述最多 500 字")
    private String description;

    /** 凭证图片地址，最多 9 张；前端复用文件上传接口后回填 */
    @Size(max = 9, message = "凭证图片最多 9 张")
    private List<String> images;

    /** 申请退款金额；仅退款 / 退货退款时必填 */
    private BigDecimal refundAmount;
}
