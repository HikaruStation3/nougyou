package com.nougyou.home.controller;

import com.nougyou.common.api.ApiResponse;
import com.nougyou.home.dto.HomeVO;
import com.nougyou.home.service.HomeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 首页聚合接口（API-111 / FR-002），游客可匿名访问。
 */
@RestController
@RequestMapping({"/api/v1/home", "/api/home"})
@RequiredArgsConstructor
public class HomeController {

    private final HomeService homeService;

    @GetMapping
    public ApiResponse<HomeVO> home() {
        return ApiResponse.ok(homeService.home());
    }
}
