package com.nougyou.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;

@Component
public class JwtTokenProvider {

    private static final String TYPE_ACCESS = "access";
    private static final String TYPE_REFRESH = "refresh";

    private final SecretKey key;
    private final long accessTokenExpireSeconds;
    private final long refreshTokenExpireSeconds;

    public JwtTokenProvider(JwtProperties properties) {
        this.key = Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpireSeconds = properties.getAccessTokenExpireMinutes() * 60;
        this.refreshTokenExpireSeconds = properties.getRefreshTokenExpireDays() * 24 * 3600;
    }

    public String createAccessToken(Long userId, String username, List<String> authorities) {
        return buildToken(userId, username, authorities, TYPE_ACCESS, accessTokenExpireSeconds);
    }

    public String createRefreshToken(Long userId, String username) {
        return buildToken(userId, username, List.of(), TYPE_REFRESH, refreshTokenExpireSeconds);
    }

    private String buildToken(Long userId, String username, List<String> authorities,
                              String type, long expireSeconds) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(username)
                .claim("uid", userId)
                .claim("type", type)
                .claim("authorities", authorities)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(expireSeconds)))
                .signWith(key)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public long getAccessTokenExpireSeconds() {
        return accessTokenExpireSeconds;
    }
}
