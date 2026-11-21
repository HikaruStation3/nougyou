package com.nougyou.farmer.controller;

import com.nougyou.common.api.ApiResponse;
import com.nougyou.farmer.dto.FarmerApplyForm;
import com.nougyou.farmer.dto.FarmerApplyVO;
import com.nougyou.farmer.service.FarmerApplyService;
import com.nougyou.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 农户商家入驻申请（买家端）。同时兼容接口文档与项目统一的 /api/v1 前缀。
 */
@RestController
@RequestMapping({"/api/v1/farmer/applies", "/api/farmer/applies"})
@RequiredArgsConstructor
public class FarmerApplyController {

    private final FarmerApplyService applyService;

    @PostMapping
    @PreAuthorize("hasAuthority('farmerApply:submit')")
    public ApiResponse<FarmerApplyVO> submit(@Valid @RequestBody FarmerApplyForm form) {
        return ApiResponse.ok(applyService.submit(SecurityUtils.currentUser().userId(), form));
    }

    @GetMapping("/my")
    @PreAuthorize("hasAuthority('farmerApply:my')")
    public ApiResponse<List<FarmerApplyVO>> my() {
        return ApiResponse.ok(applyService.my(SecurityUtils.currentUser().userId()));
    }
}
