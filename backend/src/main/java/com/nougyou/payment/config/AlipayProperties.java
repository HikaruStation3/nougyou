package com.nougyou.payment.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 支付宝沙箱配置（application.yml 的 alipay 段）。
 * <p>
 * 三个密钥类字段留空时 {@link #isReady()} 为 false，支付接口会明确报错，
 * 前端据此只展示「模拟支付」，保证没申请沙箱也能正常演示其它模块。
 */
@Data
@Component
@ConfigurationProperties(prefix = "alipay")
public class AlipayProperties {

    /** 总开关：false 时前端隐藏支付宝入口 */
    private boolean enabled = false;
    /** true 走沙箱网关，false 走生产网关 */
    private boolean sandbox = true;
    /** 网关地址，沙箱默认 openapi-sandbox.dl.alipaydev.com */
    private String gateway = "https://openapi-sandbox.dl.alipaydev.com/gateway.do";
    /** 沙箱应用的 APPID，形如 9021000xxx */
    private String appId = "";
    /** 应用私钥（PKCS8，支付宝密钥工具导出的单行 Base64，可带 PEM 头尾） */
    private String privateKey = "";
    /** 支付宝公钥（沙箱应用页「支付宝公钥」，非应用公钥） */
    private String alipayPublicKey = "";
    /** 异步通知地址：需公网可达；本机开发填 localhost 不会被调用，靠主动查单兜底 */
    private String notifyUrl = "http://localhost:8080/api/v1/pay/alipay/notify";
    /** 同步跳转地址：浏览器直接跳转，填本机前端地址即可 */
    private String returnUrl = "http://localhost:3100/#/pay/result";
    /** 收银台商品标题前缀 */
    private String subjectPrefix = "钟山镇扶贫助农-";

    /** 开关打开且三个密钥字段都填了才算可用 */
    public boolean isReady() {
        return enabled
                && appId != null && !appId.isBlank()
                && privateKey != null && !privateKey.isBlank()
                && alipayPublicKey != null && !alipayPublicKey.isBlank();
    }
}
