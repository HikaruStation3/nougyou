package com.nougyou.entrust.controller;

import com.nougyou.common.api.ApiResponse;
import com.nougyou.common.page.PageResult;
import com.nougyou.entrust.dto.EntrustAgentVO;
import com.nougyou.entrust.dto.EntrustConfirmForm;
import com.nougyou.entrust.dto.EntrustCreateForm;
import com.nougyou.entrust.dto.EntrustQuery;
import com.nougyou.entrust.dto.EntrustStatVO;
import com.nougyou.entrust.dto.EntrustTerminateForm;
import com.nougyou.entrust.dto.EntrustVO;
import com.nougyou.entrust.dto.SettlementVO;
import com.nougyou.entrust.service.EntrustService;
import com.nougyou.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 委托销售接口：API-087 建立 / API-088 委托出去 / API-089 代销 / API-090 详情 /
 * API-091 确认 / API-092 终止 / API-093 统计 / API-094 结算单列表。
 */
@RestController
@RequestMapping({"/api/v1/entrusts", "/api/entrusts"})
@RequiredArgsConstructor
public class EntrustController {

    private final EntrustService entrustService;

    @PostMapping
    @PreAuthorize("hasAuthority('entrust:add')")
    public ApiResponse<EntrustVO> create(@Valid @RequestBody EntrustCreateForm form) {
        return ApiResponse.ok(entrustService.create(SecurityUtils.currentUser().userId(), form));
    }

    /**
     * 可选受托方列表，供建立委托关系时下拉选择（文档未列，属交互辅助接口）。
     */
    @GetMapping("/agents")
    @PreAuthorize("hasAuthority('entrust:add')")
    public ApiResponse<List<EntrustAgentVO>> agents() {
        return ApiResponse.ok(entrustService.agents(SecurityUtils.currentUser().userId()));
    }

    @GetMapping("/sent")
    @PreAuthorize("hasAuthority('entrust:list')")
    public ApiResponse<PageResult<EntrustVO>> sent(EntrustQuery query) {
        return ApiResponse.ok(entrustService.sentPage(SecurityUtils.currentUser().userId(), query));
    }

    @GetMapping("/received")
    @PreAuthorize("hasAuthority('entrust:list')")
    public ApiResponse<PageResult<EntrustVO>> received(EntrustQuery query) {
        return ApiResponse.ok(entrustService.receivedPage(SecurityUtils.currentUser().userId(), query));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('entrust:detail')")
    public ApiResponse<EntrustVO> detail(@PathVariable Long id) {
        return ApiResponse.ok(entrustService.detail(SecurityUtils.currentUser().userId(), id));
    }

    @PutMapping("/{id}/confirm")
    @PreAuthorize("hasAuthority('entrust:confirm')")
    public ApiResponse<EntrustVO> confirm(@PathVariable Long id,
                                          @Valid @RequestBody EntrustConfirmForm form) {
        return ApiResponse.ok(entrustService.confirm(SecurityUtils.currentUser().userId(), id, form));
    }

    @PutMapping("/{id}/terminate")
    @PreAuthorize("hasAuthority('entrust:terminate')")
    public ApiResponse<EntrustVO> terminate(@PathVariable Long id,
                                           @Valid @RequestBody(required = false) EntrustTerminateForm form) {
        String reason = form == null ? null : form.getReason();
        return ApiResponse.ok(entrustService.terminate(SecurityUtils.currentUser().userId(), id, reason));
    }

    @GetMapping("/{id}/stat")
    @PreAuthorize("hasAuthority('entrust:stat')")
    public ApiResponse<EntrustStatVO> stat(@PathVariable Long id) {
        return ApiResponse.ok(entrustService.stat(SecurityUtils.currentUser().userId(), id));
    }

    @GetMapping("/{id}/settlements")
    @PreAuthorize("hasAuthority('entrust:settlement')")
    public ApiResponse<PageResult<SettlementVO>> settlements(
            @PathVariable Long id,
            @RequestParam(required = false) String settleMonth,
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "12") long pageSize) {
        return ApiResponse.ok(entrustService.settlementPage(
                SecurityUtils.currentUser().userId(), id, settleMonth, pageNum, pageSize));
    }
}
