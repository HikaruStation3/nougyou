package com.nougyou.entrust.controller;

import com.nougyou.common.api.ApiResponse;
import com.nougyou.common.page.PageResult;
import com.nougyou.entrust.dto.EntrustQuery;
import com.nougyou.entrust.dto.EntrustVO;
import com.nougyou.entrust.dto.SettleForm;
import com.nougyou.entrust.dto.SettlementVO;
import com.nougyou.entrust.service.EntrustService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后台委托结算：API-095 生成月度结算单，附带委托关系查询供结算页选择。
 */
@RestController
@RequestMapping({"/api/v1/admin/entrusts", "/api/admin/entrusts"})
@RequiredArgsConstructor
public class EntrustAdminController {

    private final EntrustService entrustService;

    @GetMapping
    @PreAuthorize("hasAuthority('entrust:settle')")
    public ApiResponse<PageResult<EntrustVO>> page(EntrustQuery query) {
        return ApiResponse.ok(entrustService.adminPage(query));
    }

    @PostMapping("/{id}/settle")
    @PreAuthorize("hasAuthority('entrust:settle')")
    public ApiResponse<SettlementVO> settle(@PathVariable Long id, @Valid @RequestBody SettleForm form) {
        return ApiResponse.ok(entrustService.settle(id, form.getSettleMonth()));
    }
}
