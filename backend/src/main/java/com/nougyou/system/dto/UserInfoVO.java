package com.nougyou.system.dto;

import java.util.List;

/**
 * 当前登录用户信息，字段与前端 UserInfo 对应。
 */
public record UserInfoVO(
        String userId,
        String username,
        String nickname,
        String avatar,
        List<String> roles,
        List<String> perms
) {
}
