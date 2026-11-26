package com.nougyou.payment.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 支付宝收银台跳转信息（GET /api/v1/pay/alipay/page）。
 */
@Data
@Builder
public class AlipayPayVO {

    private String orderId;
    private String orderNo;
    /** 应付金额，两位小数字符串 */
    private String amount;
    /** 收银台完整地址，前端直接 location.href 跳转 */
    private String payUrl;
    /** 是否沙箱环境，前端用来显示「沙箱」标识 */
    private boolean sandbox;
    private String gateway;
}
