package com.nougyou.security;

import com.nougyou.common.api.ResultCode;
import com.nougyou.common.exception.BusinessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static LoginUser currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof LoginUser loginUser) {
            return loginUser;
        }
        throw new BusinessException(ResultCode.ACCESS_TOKEN_INVALID, "访问令牌无效或已过期");
    }

    /**
     * 可选登录用户：公开接口（如短视频列表）允许游客访问，
     * 登录后才需要拼装「是否已点赞 / 是否已关注」，因此不能用 currentUser() 直接抛错。
     */
    public static Long currentUserIdOrNull() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof LoginUser loginUser) {
            return loginUser.userId();
        }
        return null;
    }

    /**
     * 判断当前登录用户是否拥有指定权限点，用于需要按角色区分数据范围的共享接口。
     */
    public static boolean hasAuthority(String authority) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authority == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(item -> authority.equals(item.getAuthority()));
    }
}
