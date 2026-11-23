package com.nougyou.order.dto;

import lombok.Data;

/**
 * 模拟支付入参（API-055）。
 */
@Data
public class OrderPayForm {

    /** 1 微信 / 2 支付宝 / 3 模拟支付，默认 3 */
    private Integer payType;
}
