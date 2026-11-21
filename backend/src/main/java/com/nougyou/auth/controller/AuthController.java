package com.nougyou.auth.controller;

import com.nougyou.auth.dto.CaptchaResponse;
import com.nougyou.auth.dto.LoginRequest;
import com.nougyou.auth.dto.LoginResponse;
import com.nougyou.auth.dto.RegisterRequest;
import com.nougyou.auth.service.AuthService;
import com.nougyou.auth.service.CaptchaService;
import com.nougyou.common.api.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final CaptchaService captchaService;

    @GetMapping("/captcha")
    public ApiResponse<CaptchaResponse> captcha() {
        return ApiResponse.ok(captchaService.generate());
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }

    @PostMapping("/register")
    public ApiResponse<Void> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ApiResponse.ok();
    }

    @PostMapping("/refresh-token")
    public ApiResponse<LoginResponse> refreshToken(@RequestParam String refreshToken) {
        return ApiResponse.ok(authService.refresh(refreshToken));
    }

    @DeleteMapping("/logout")
    public ApiResponse<Void> logout() {
        return ApiResponse.ok();
    }
}
