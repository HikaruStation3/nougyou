package com.nougyou.farmer.controller;

import com.nougyou.common.api.ApiResponse;
import com.nougyou.common.page.PageResult;
import com.nougyou.farmer.dto.FarmerApplyAuditForm;
import com.nougyou.farmer.dto.FarmerApplyQuery;
import com.nougyou.farmer.dto.FarmerApplyVO;
import com.nougyou.farmer.service.FarmerApplyService;
import com.nougyou.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 入驻申请审核（管理端）。同时兼容接口文档与项目统一的 /api/v1 前缀。
 */
@RestController
@RequestMapping({"/api/v1/admin/farmer/applies", "/api/admin/farmer/applies"})
@RequiredArgsConstructor
public class FarmerApplyAdminController {

    private final FarmerApplyService applyService;

    @GetMapping
    @PreAuthorize("hasAuthority('farmerApply:list')")
    public ApiResponse<PageResult<FarmerApplyVO>> page(FarmerApplyQuery query) {
        return ApiResponse.ok(applyService.page(query));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('farmerApply:detail')")
    public ApiResponse<FarmerApplyVO> detail(@PathVariable Long id) {
        return ApiResponse.ok(applyService.detail(id));
    }

    @PutMapping("/{id}/audit")
    @PreAuthorize("hasAuthority('farmerApply:audit')")
    public ApiResponse<FarmerApplyVO> audit(@PathVariable Long id, @Valid @RequestBody FarmerApplyAuditForm form) {
        return ApiResponse.ok(applyService.audit(id, SecurityUtils.currentUser().userId(), form));
    }
}
