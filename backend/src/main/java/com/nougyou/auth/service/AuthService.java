package com.nougyou.auth.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.nougyou.auth.dto.LoginRequest;
import com.nougyou.auth.dto.LoginResponse;
import com.nougyou.auth.dto.RegisterRequest;
import com.nougyou.common.api.ResultCode;
import com.nougyou.common.exception.BusinessException;
import com.nougyou.security.JwtTokenProvider;
import com.nougyou.system.entity.SysRole;
import com.nougyou.system.entity.SysUser;
import com.nougyou.system.mapper.SysRoleMapper;
import com.nougyou.system.mapper.SysUserMapper;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final CaptchaService captchaService;

    public LoginResponse login(LoginRequest request) {
        captchaService.validate(request.captchaId(), request.captchaCode());

        SysUser user = userMapper.selectOne(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getUsername, request.username()));
        if (user == null || !passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BusinessException(ResultCode.LOGIN_FAILED, "用户名或密码错误");
        }
        if (Integer.valueOf(0).equals(user.getStatus())) {
            throw new BusinessException(ResultCode.LOGIN_FAILED, "账号已被禁用");
        }
        return buildTokens(user);
    }

    public void register(RegisterRequest request) {
        String username = request.username().trim();
        if (userMapper.exists(Wrappers.<SysUser>lambdaQuery().eq(SysUser::getUsername, username))) {
            throw new BusinessException("用户名已存在");
        }
        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setNickname(StringUtils.hasText(request.nickname()) ? request.nickname().trim() : username);
        user.setMobile(request.mobile());
        user.setEmail(request.email());
        user.setGender(0);
        user.setStatus(1);

        SysRole userRole = roleMapper.selectOne(Wrappers.<SysRole>lambdaQuery().eq(SysRole::getCode, "USER"));
        if (userRole != null) {
            user.setRoleId(userRole.getId());
        }
        userMapper.insert(user);
    }

    public LoginResponse refresh(String refreshToken) {
        if (!StringUtils.hasText(refreshToken)) {
            throw new BusinessException(ResultCode.REFRESH_TOKEN_INVALID, "刷新令牌不能为空");
        }
        try {
            Claims claims = tokenProvider.parse(refreshToken);
            if (!"refresh".equals(claims.get("type", String.class))) {
                throw new BusinessException(ResultCode.REFRESH_TOKEN_INVALID, "刷新令牌无效");
            }
            Long userId = ((Number) claims.get("uid")).longValue();
            SysUser user = userMapper.selectById(userId);
            if (user == null || Integer.valueOf(0).equals(user.getStatus())) {
                throw new BusinessException(ResultCode.REFRESH_TOKEN_INVALID, "账号不存在或已禁用");
            }
            return buildTokens(user);
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException(ResultCode.REFRESH_TOKEN_INVALID, "刷新令牌无效或已过期");
        }
    }

    public List<String> roleCodes(SysUser user) {
        if (user.getRoleId() == null) {
            return List.of();
        }
        SysRole role = roleMapper.selectById(user.getRoleId());
        return role == null ? List.of() : List.of(role.getCode());
    }

    private LoginResponse buildTokens(SysUser user) {
        List<String> authorities = roleCodes(user).stream().map(code -> "ROLE_" + code).toList();
        String accessToken = tokenProvider.createAccessToken(user.getId(), user.getUsername(), authorities);
        String refreshToken = tokenProvider.createRefreshToken(user.getId(), user.getUsername());
        return new LoginResponse(accessToken, refreshToken, "Bearer", tokenProvider.getAccessTokenExpireSeconds());
    }
}
