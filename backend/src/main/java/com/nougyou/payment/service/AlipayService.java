package com.nougyou.payment.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nougyou.common.exception.BusinessException;
import com.nougyou.order.entity.Order;
import com.nougyou.payment.config.AlipayProperties;
import com.nougyou.payment.util.AlipaySigner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringJoiner;

/**
 * 支付宝电脑网站支付（沙箱）。
 * <p>
 * 只用 JDK 的 {@code SHA256withRSA} 与 {@code HttpClient}，不依赖官方 SDK：
 * <ol>
 *   <li>下单跳转：拼公共参数 + {@code biz_content}，用应用私钥签名后拼成收银台 GET 地址；</li>
 *   <li>主动查单：{@code alipay.trade.query} 回查交易状态，并对响应节点验签；</li>
 *   <li>异步通知：验签 + 校验金额后交给订单服务落库。</li>
 * </ol>
 * 本机开发没有公网地址，支付宝的 notify_url 打不进来，所以「同步跳回 + 主动查单」
 * 是主链路，notify 只在部署到公网时作为兜底。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlipayService {

    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String CHARSET = "utf-8";
    /** 收银台停留时限，超时后交易关闭，避免脏单长期挂着 */
    private static final String TIMEOUT_EXPRESS = "30m";

    private final AlipayProperties props;
    private final ObjectMapper objectMapper;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    /** 配置是否可用，用于接口快速失败与前端入口显隐 */
    public boolean isReady() {
        return props.isReady();
    }

    public boolean isSandbox() {
        return props.isSandbox();
    }

    public String gateway() {
        return props.getGateway();
    }

    /**
     * 生成电脑网站支付收银台地址。
     * <p>
     * {@code product_code=FAST_INSTANT_TRADE_PAY} 是电脑网站支付的固定值；
     * subject 限 256 字符，这里用「前缀 + 订单号」避免超长。
     */
    public String pagePayUrl(Order order) {
        requireReady();
        String amount = order.getPayAmount() == null ? "0.00" : order.getPayAmount().toPlainString();

        Map<String, Object> biz = new LinkedHashMap<>();
        biz.put("out_trade_no", order.getOrderNo());
        biz.put("total_amount", amount);
        biz.put("subject", props.getSubjectPrefix() + order.getOrderNo());
        biz.put("product_code", "FAST_INSTANT_TRADE_PAY");
        biz.put("timeout_express", TIMEOUT_EXPRESS);

        Map<String, String> params = baseParams("alipay.trade.page.pay");
        params.put("notify_url", props.getNotifyUrl());
        params.put("return_url", props.getReturnUrl());
        params.put("biz_content", toJson(biz));

        String content = AlipaySigner.buildSignContent(params);
        params.put("sign", AlipaySigner.sign(content, props.getPrivateKey()));

        String url = props.getGateway() + "?" + toQueryString(params);
        log.info("支付宝沙箱下单：订单 {}，金额 {}，网关 {}", order.getOrderNo(), amount, props.getGateway());
        return url;
    }

    /**
     * 主动查单（alipay.trade.query）。
     * <p>
     * 支付宝返回的报文形如 {@code {"alipay_trade_query_response":{...},"sign":"..."}}，
     * 验签对象是 response 节点的**原始 JSON 文本**，不能反序列化后再序列化，
     * 否则字段顺序变化会导致验签失败，因此这里按括号配对截取原文。
     */
    public TradeResult query(String orderNo) {
        requireReady();

        Map<String, Object> biz = new LinkedHashMap<>();
        biz.put("out_trade_no", orderNo);

        Map<String, String> params = baseParams("alipay.trade.query");
        params.put("biz_content", toJson(biz));
        params.put("sign", AlipaySigner.sign(AlipaySigner.buildSignContent(params), props.getPrivateKey()));

        String body = post(toQueryString(params));
        log.debug("支付宝查单响应：{}", body);

        String nodeJson = extractNodeJson(body, "alipay_trade_query_response");
        if (nodeJson == null) {
            return TradeResult.fail(orderNo, "支付宝响应格式异常");
        }

        JsonNode root;
        JsonNode whole;
        try {
            root = objectMapper.readTree(nodeJson);
            whole = objectMapper.readTree(body);
        } catch (Exception e) {
            return TradeResult.fail(orderNo, "支付宝响应解析失败");
        }

        // 业务错误优先返回：APPID 填错时支付宝也会返回错误报文且未必带签名，
        // 先验签会把「app_id 不合法」误报成「公钥配错」，排查成本很高。
        String code = text(root, "code");
        if (!"10000".equals(code)) {
            // ACQ.TRADE_NOT_EXIST 属于正常情况：用户在收银台还没付款，交易尚未创建
            if ("ACQ.TRADE_NOT_EXIST".equals(text(root, "sub_code"))) {
                return TradeResult.fail(orderNo, "尚未在支付宝完成付款");
            }
            String subMsg = text(root, "sub_msg");
            return TradeResult.fail(orderNo, subMsg != null ? subMsg : text(root, "msg"));
        }

        // 交易数据必须验签，防止响应被篡改
        JsonNode signNode = whole.get("sign");
        String sign = signNode == null ? null : signNode.asText();
        if (sign == null || !AlipaySigner.verify(nodeJson, sign, props.getAlipayPublicKey())) {
            log.warn("支付宝查单验签失败，订单 {}，请检查「支付宝公钥」是否填成了应用公钥", orderNo);
            return TradeResult.fail(orderNo, "支付宝响应验签失败，请检查支付宝公钥配置");
        }

        return new TradeResult(
                orderNo,
                text(root, "trade_no"),
                text(root, "trade_status"),
                text(root, "total_amount"),
                text(root, "buyer_logon_id"),
                true,
                null);
    }

    /**
     * 异步通知验签：剔除 sign、sign_type 后按字典序重拼，用支付宝公钥验证。
     * 通知参数是 form 表单，值已是解码后的原文，不需要再做 URL 解码。
     */
    public boolean verifyNotify(Map<String, String> params) {
        if (!props.isReady()) {
            return false;
        }
        String sign = params.get("sign");
        if (sign == null || sign.isEmpty()) {
            return false;
        }
        boolean ok = AlipaySigner.verify(AlipaySigner.buildSignContent(params), sign, props.getAlipayPublicKey());
        if (!ok) {
            log.warn("支付宝异步通知验签失败，out_trade_no={}", params.get("out_trade_no"));
        }
        return ok;
    }

    /* ============================ 内部工具 ============================ */

    private void requireReady() {
        if (!props.isEnabled()) {
            throw new BusinessException("支付宝支付未启用，请在 application.yml 的 alipay.enabled 打开并填入沙箱密钥");
        }
        if (!props.isReady()) {
            throw new BusinessException("支付宝沙箱密钥未配置完整（app-id / private-key / alipay-public-key）");
        }
    }

    /** 公共请求参数，按开放平台要求收齐后才签名 */
    private Map<String, String> baseParams(String method) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("app_id", props.getAppId());
        params.put("method", method);
        params.put("format", "JSON");
        params.put("charset", CHARSET);
        params.put("sign_type", "RSA2");
        params.put("timestamp", LocalDateTime.now().format(TIMESTAMP));
        params.put("version", "1.0");
        return params;
    }

    private String post(String form) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(props.getGateway()))
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/x-www-form-urlencoded;charset=" + CHARSET)
                .POST(HttpRequest.BodyPublishers.ofString(form))
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return response.body();
        } catch (Exception e) {
            throw new BusinessException("调用支付宝网关失败：" + e.getMessage());
        }
    }

    /** 支付宝要求参数值做 URL 编码，空格用 %20 而不是 + */
    private String toQueryString(Map<String, String> params) {
        StringJoiner joiner = new StringJoiner("&");
        params.forEach((k, v) -> {
            if (v == null || v.isEmpty()) {
                return;
            }
            joiner.add(k + "=" + URLEncoder.encode(v, StandardCharsets.UTF_8).replace("+", "%20"));
        });
        return joiner.toString();
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new BusinessException("构造支付宝请求参数失败");
        }
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    /**
     * 从完整报文中截取指定节点的原始 JSON 文本（按大括号配对，跳过字符串内的括号）。
     * 验签必须用原文，不能用「解析后再序列化」的结果。
     */
    static String extractNodeJson(String body, String nodeName) {
        if (body == null) {
            return null;
        }
        String marker = "\"" + nodeName + "\"";
        int idx = body.indexOf(marker);
        if (idx < 0) {
            return null;
        }
        int start = body.indexOf('{', idx + marker.length());
        if (start < 0) {
            return null;
        }
        int depth = 0;
        boolean inString = false;
        boolean escaped = false;
        for (int i = start; i < body.length(); i++) {
            char c = body.charAt(i);
            if (inString) {
                if (escaped) {
                    escaped = false;
                } else if (c == '\\') {
                    escaped = true;
                } else if (c == '"') {
                    inString = false;
                }
                continue;
            }
            if (c == '"') {
                inString = true;
            } else if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    return body.substring(start, i + 1);
                }
            }
        }
        return null;
    }

    /**
     * 一次查单的结果。
     *
     * @param verified 是否通过验签
     * @param message  失败原因，成功为 null
     */
    public record TradeResult(String outTradeNo, String tradeNo, String tradeStatus,
                              String totalAmount, String buyerLogonId,
                              boolean verified, String message) {

        static TradeResult fail(String orderNo, String message) {
            return new TradeResult(orderNo, null, null, null, null, false, message);
        }

        /** TRADE_SUCCESS 与 TRADE_FINISHED 都表示钱已到账 */
        public boolean paid() {
            return "TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus);
        }

        public BigDecimal amount() {
            try {
                return totalAmount == null ? null : new BigDecimal(totalAmount);
            } catch (NumberFormatException e) {
                return null;
            }
        }
    }
}
