package com.nougyou.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 提交订单入参（API-052）。
 * source=cart 时读取 cartIds；source=buyNow 时读取 productId + quantity。
 */
@Data
public class OrderCreateForm {

    @NotBlank(message = "下单来源不能为空")
    private String source;

    private List<Long> cartIds;

    private Long productId;

    @Min(value = 1, message = "购买数量至少为 1")
    private Integer quantity;

    @NotBlank(message = "收货人不能为空")
    @Size(min = 2, max = 20, message = "收货人姓名长度需在 2-20 个字符之间")
    private String receiverName;

    @NotBlank(message = "收货电话不能为空")
    @Pattern(regexp = "^1\\d{10}$", message = "收货电话格式不正确")
    private String receiverPhone;

    @NotBlank(message = "收货地址不能为空")
    @Size(max = 200, message = "收货地址最长 200 个字符")
    private String receiverAddress;

    @Size(max = 200, message = "买家备注最长 200 个字符")
    private String remark;

    /** 1 微信 / 2 支付宝 / 3 模拟支付，默认 3 */
    private Integer payType;
}
