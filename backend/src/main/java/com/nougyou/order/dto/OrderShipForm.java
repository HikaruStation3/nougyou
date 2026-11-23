package com.nougyou.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 订单发货入参（API-059）。
 */
@Data
public class OrderShipForm {

    @NotBlank(message = "物流公司不能为空")
    @Size(max = 50, message = "物流公司最长 50 个字符")
    private String logisticsCompany;

    @NotBlank(message = "物流单号不能为空")
    @Size(max = 50, message = "物流单号最长 50 个字符")
    private String logisticsNo;
}
