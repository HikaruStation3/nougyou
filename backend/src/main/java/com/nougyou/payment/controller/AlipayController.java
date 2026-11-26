package com.nougyou.payment.controller;

import com.nougyou.common.api.ApiResponse;
import com.nougyou.common.api.ResultCode;
import com.nougyou.common.exception.BusinessException;
import com.nougyou.order.entity.Order;
import com.nougyou.order.service.OrderService;
import com.nougyou.payment.config.AlipayProperties;
import com.nougyou.payment.dto.AlipayPayVO;
import com.nougyou.payment.dto.AlipayQueryVO;
import com.nougyou.payment.service.AlipayService;
import com.nougyou.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 支付宝沙箱支付接口。
 * <p>
 * 链路：下单页拿收银台地址 → 浏览器跳转 → 沙箱付款 → 支付宝按 return_url 跳回前端
 * → 前端拿 out_trade_no 调 {@code /query} 主动查单 → 后端验签后落库为已支付。
 * 之所以用「主动查单」而不是等异步通知：本机没有公网地址，notify_url 打不进来。
 * 部署到公网后把 notify_url 改成真实域名，{@code /notify} 会自动接管。
 */
@Slf4j
@RestController
@RequestMapping({"/api/v1/pay/alipay", "/api/pay/alipay"})
@RequiredArgsConstructor
public class AlipayController {

    private final AlipayService alipayService;
    private final AlipayProperties props;
    private final OrderService orderService;

    /**
     * 支付能力探测，公开接口。前端据此决定是否展示「支付宝沙箱」入口，
     * 以及提示用户去补哪几个配置项。
     */
    @GetMapping("/config")
    public ApiResponse<Map<String, Object>> config() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("enabled", props.isEnabled());
        data.put("ready", props.isReady());
        data.put("sandbox", props.isSandbox());
        data.put("gateway", props.getGateway());
        data.put("returnUrl", props.getReturnUrl());
        // 未就绪时告诉前端缺哪一项，页面上直接显示，省得去翻日志
        data.put("missing", missingKeys());
        return ApiResponse.ok(data);
    }

    /**
     * 生成电脑网站支付收银台地址。
     * 校验订单归属与状态后才签名，避免拿别人的订单号生成支付链接。
     */
    @GetMapping("/page")
    public ApiResponse<AlipayPayVO> page(@RequestParam Long orderId) {
        Long buyerId = SecurityUtils.currentUser().userId();
        Order order = orderService.requirePayableOrder(buyerId, orderId);

        AlipayPayVO vo = AlipayPayVO.builder()
                .orderId(String.valueOf(order.getId()))
                .orderNo(order.getOrderNo())
                .amount(order.getPayAmount() == null ? "0.00" : order.getPayAmount().toPlainString())
                .payUrl(alipayService.pagePayUrl(order))
                .sandbox(alipayService.isSandbox())
                .gateway(alipayService.gateway())
                .build();
        return ApiResponse.ok(vo);
    }

    /**
     * 主动查单。前端从收银台跳回后调用，查到已付款就把本地订单置为已支付。
     * 重复调用安全：订单已是已支付时直接返回，不再请求支付宝。
     */
    @GetMapping("/query")
    public ApiResponse<AlipayQueryVO> query(@RequestParam String orderNo) {
        Long currentUserId = SecurityUtils.currentUser().userId();
        Order order = orderService.findByOrderNo(orderNo);
        boolean admin = SecurityUtils.hasAuthority("order:adminList");
        if (!admin && !currentUserId.equals(order.getBuyerId())) {
            throw new BusinessException(ResultCode.PERMISSION_DENIED, "无权查看该订单");
        }

        AlipayQueryVO.AlipayQueryVOBuilder builder = AlipayQueryVO.builder()
                .orderId(String.valueOf(order.getId()))
                .orderNo(order.getOrderNo())
                .orderStatus(order.getOrderStatus())
                .payStatus(order.getPayStatus());

        // 本地已是已支付：无需再请求支付宝，直接返回（幂等，前端可放心轮询）
        if (Integer.valueOf(1).equals(order.getPayStatus())) {
            return ApiResponse.ok(builder
                    .tradeNo(order.getTradeNo())
                    .paid(true)
                    .tradeStatus("TRADE_SUCCESS")
                    .totalAmount(order.getPayAmount() == null ? null : order.getPayAmount().toPlainString())
                    .build());
        }

        AlipayService.TradeResult result = alipayService.query(orderNo);
        builder.tradeNo(result.tradeNo())
                .tradeStatus(result.tradeStatus())
                .totalAmount(result.totalAmount());

        if (!result.verified() && result.message() != null) {
            // 验签失败 / 网关报错：把原因交给前端展示，不吞掉
            return ApiResponse.ok(builder.paid(false).message(result.message()).build());
        }

        if (result.paid()) {
            Order updated = orderService.markPaidByAlipay(order.getId(), result.tradeNo(), result.amount());
            builder.paid(true)
                    .tradeNo(updated.getTradeNo())
                    .orderStatus(updated.getOrderStatus())
                    .payStatus(updated.getPayStatus());
        } else {
            builder.paid(false).message(describe(result.tradeStatus()));
        }
        return ApiResponse.ok(builder.build());
    }

    /**
     * 异步通知。仅部署到公网后支付宝才会调用，返回纯文本 {@code success}
     * 表示已处理，否则支付宝会按策略重试。
     */
    @PostMapping(value = "/notify", produces = "text/plain;charset=UTF-8")
    public String notify(@RequestParam Map<String, String> params) {
        String orderNo = params.get("out_trade_no");
        String tradeStatus = params.get("trade_status");
        log.info("收到支付宝异步通知：out_trade_no={}, trade_status={}", orderNo, tradeStatus);

        if (!alipayService.verifyNotify(params)) {
            return "failure";
        }
        // 只有付款成功才改单，WAIT_BUYER_PAY 等状态只确认收到
        if (!"TRADE_SUCCESS".equals(tradeStatus) && !"TRADE_FINISHED".equals(tradeStatus)) {
            return "success";
        }
        try {
            Order order = orderService.findByOrderNo(orderNo);
            BigDecimal amount = params.get("total_amount") == null ? null : new BigDecimal(params.get("total_amount"));
            orderService.markPaidByAlipay(order.getId(), params.get("trade_no"), amount);
            return "success";
        } catch (Exception e) {
            // 金额不符、订单状态异常等交给支付宝重试，同时留日志便于排查
            log.error("处理支付宝异步通知失败：out_trade_no={}", orderNo, e);
            return "failure";
        }
    }

    private String missingKeys() {
        StringBuilder sb = new StringBuilder();
        if (props.getAppId() == null || props.getAppId().isBlank()) {
            sb.append("app-id ");
        }
        if (props.getPrivateKey() == null || props.getPrivateKey().isBlank()) {
            sb.append("private-key ");
        }
        if (props.getAlipayPublicKey() == null || props.getAlipayPublicKey().isBlank()) {
            sb.append("alipay-public-key ");
        }
        return sb.toString().trim();
    }

    private String describe(String tradeStatus) {
        if (tradeStatus == null) {
            return "未查询到该笔交易，请确认已在收银台完成付款";
        }
        return switch (tradeStatus) {
            case "WAIT_BUYER_PAY" -> "交易已创建，等待付款";
            case "TRADE_CLOSED" -> "交易已关闭（超时未付或已全额退款）";
            default -> "交易状态：" + tradeStatus;
        };
    }
}
