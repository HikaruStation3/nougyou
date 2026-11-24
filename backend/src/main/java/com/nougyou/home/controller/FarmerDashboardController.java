package com.nougyou.home.controller;

import com.nougyou.common.api.ApiResponse;
import com.nougyou.home.dto.FarmerDashboardVO;
import com.nougyou.home.service.FarmerDashboardService;
import com.nougyou.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 商家经营概览（API-112 / FR-014）。
 * 数据范围固定为当前商家本店，不接受前端传入 farmerId，避免越权查看他人经营数据。
 */
@RestController
@RequestMapping({"/api/v1/farmer/dashboard", "/api/farmer/dashboard"})
@RequiredArgsConstructor
public class FarmerDashboardController {

    private final FarmerDashboardService dashboardService;

    @GetMapping
    @PreAuthorize("hasAuthority('dashboard:view')")
    public ApiResponse<FarmerDashboardVO> overview() {
        return ApiResponse.ok(dashboardService.overview(SecurityUtils.currentUser().userId()));
    }
}
