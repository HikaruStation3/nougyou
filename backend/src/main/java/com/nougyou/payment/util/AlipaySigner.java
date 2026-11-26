package com.nougyou.payment.util;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Map;
import java.util.StringJoiner;
import java.util.TreeMap;

/**
 * 支付宝 RSA2 签名工具，仅依赖 JDK，避免引入 40MB 的 alipay-sdk。
 * <p>
 * 规则与开放平台一致：待签名串＝除 {@code sign}、{@code sign_type} 外的非空参数，
 * 按参数名 ASCII 升序拼成 {@code k1=v1&k2=v2}；签名算法 {@code SHA256withRSA}，
 * 结果 Base64 编码。同步跳转与异步通知的验签走同一套规则。
 */
public final class AlipaySigner {

    private static final String SIGN_TYPE = "SHA256withRSA";

    private AlipaySigner() {
    }

    /**
     * 拼接待签名串：字典序、剔除空值与 sign / sign_type。
     * 支付宝要求参数值为「原始值」，因此这里不做 URL 编码。
     */
    public static String buildSignContent(Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            return "";
        }
        // TreeMap 按 key 自然序（ASCII）排序，与支付宝要求一致
        Map<String, String> sorted = new TreeMap<>(params);
        StringJoiner joiner = new StringJoiner("&");
        for (Map.Entry<String, String> entry : sorted.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (key == null || key.isEmpty() || "sign".equals(key) || "sign_type".equals(key)) {
                continue;
            }
            if (value == null || value.isEmpty()) {
                continue;
            }
            joiner.add(key + "=" + value);
        }
        return joiner.toString();
    }

    /** 应用私钥签名，返回 Base64 */
    public static String sign(String content, String privateKey) {
        try {
            PrivateKey key = parsePrivateKey(privateKey);
            Signature signature = Signature.getInstance(SIGN_TYPE);
            signature.initSign(key);
            signature.update(content.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signature.sign());
        } catch (Exception e) {
            throw new IllegalStateException("支付宝签名失败：" + e.getMessage(), e);
        }
    }

    /** 支付宝公钥验签 */
    public static boolean verify(String content, String sign, String publicKey) {
        try {
            PublicKey key = parsePublicKey(publicKey);
            Signature signature = Signature.getInstance(SIGN_TYPE);
            signature.initVerify(key);
            signature.update(content.getBytes(StandardCharsets.UTF_8));
            return signature.verify(Base64.getDecoder().decode(sign));
        } catch (Exception e) {
            return false;
        }
    }

    private static PrivateKey parsePrivateKey(String raw) throws Exception {
        byte[] der = Base64.getDecoder().decode(stripPem(raw));
        return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(der));
    }

    private static PublicKey parsePublicKey(String raw) throws Exception {
        byte[] der = Base64.getDecoder().decode(stripPem(raw));
        return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(der));
    }

    /** 去掉 PEM 头尾与所有空白，兼容单行 Base64 与多行 PEM 两种粘贴方式 */
    private static String stripPem(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.replaceAll("-----(BEGIN|END)[^-]+-----", "").replaceAll("\\s", "");
    }

    /** 供诊断用：判断密钥串能否被 JDK 解析成合法的 RSA 私钥/公钥，不参与业务逻辑 */
    public static boolean canParsePrivateKey(String raw) {
        try {
            parsePrivateKey(raw);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean canParsePublicKey(String raw) {
        try {
            parsePublicKey(raw);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /** 密钥长度诊断：1024 位在沙箱仍可用，2048 位是推荐值 */
    public static int keySize(String raw, boolean isPrivate) {
        try {
            if (isPrivate) {
                PrivateKey key = parsePrivateKey(raw);
                return ((java.security.interfaces.RSAKey) key).getModulus().bitLength();
            }
            PublicKey key = parsePublicKey(raw);
            return ((java.security.interfaces.RSAKey) key).getModulus().bitLength();
        } catch (Exception e) {
            return 0;
        }
    }

}
