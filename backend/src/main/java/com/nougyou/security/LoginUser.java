package com.nougyou.security;

/**
 * 登录用户信息，作为 SecurityContext 的 principal。
 */
public record LoginUser(Long userId, String username) {
}
