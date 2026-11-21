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
}
