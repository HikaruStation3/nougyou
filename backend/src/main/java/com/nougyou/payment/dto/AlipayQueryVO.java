package com.nougyou.payment.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 支付宝订单查询结果（GET /api/v1/pay/alipay/query）。
 * <p>
 * 前端从沙箱收银台跳回后没有异步通知可达（本机无公网地址），
 * 靠这个接口主动向支付宝查单，查到 TRADE_SUCCESS 就落库为已支付。
 */
@Data
@Builder
public class AlipayQueryVO {

    private String orderId;
    private String orderNo;
    /** 支付宝交易号，未支付时为 null */
    private String tradeNo;
    /** WAIT_BUYER_PAY / TRADE_SUCCESS / TRADE_FINISHED / TRADE_CLOSED */
    private String tradeStatus;
    /** 支付宝侧的订单金额 */
    private String totalAmount;
    /** 本次查单是否把本地订单置为已支付 */
    private boolean paid;
    /** 本地订单状态，0 待付款 / 1 待发货 / 2 待收货 / 3 已完成 / 4 已取消 */
    private Integer orderStatus;
    private Integer payStatus;
    /** 查单失败时的原因，成功为 null */
    private String message;
}
