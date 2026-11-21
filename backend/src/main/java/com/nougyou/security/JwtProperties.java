package com.nougyou.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "nougyou.jwt")
public class JwtProperties {

    /** 签名密钥（不少于 32 字节） */
    private String secret;
    /** 访问令牌有效期（分钟） */
    private long accessTokenExpireMinutes = 120;
    /** 刷新令牌有效期（天） */
    private long refreshTokenExpireDays = 7;
}
