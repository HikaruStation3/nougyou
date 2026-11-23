package com.nougyou.order.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 取消订单入参（API-056）。
 * orders 表未设计取消原因字段，该参数仅做长度校验后记录到应用日志。
 */
@Data
public class OrderCancelForm {

    @Size(max = 100, message = "取消原因最长 100 个字符")
    private String reason;
}
