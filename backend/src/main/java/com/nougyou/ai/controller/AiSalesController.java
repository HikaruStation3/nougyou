package com.nougyou.ai.controller;

import com.nougyou.ai.dto.AiAnalysisVO;
import com.nougyou.ai.dto.SalesAnalysisRequest;
import com.nougyou.ai.dto.SalesStatResultVO;
import com.nougyou.ai.service.AiSalesService;
import com.nougyou.common.api.ApiResponse;
import com.nougyou.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 销售统计与分析接口（FR-027 / FR-028），仅供农户商家查询本店数据。
 */
@RestController
@RequestMapping("/api/v1/ai/sales")
@RequiredArgsConstructor
public class AiSalesController {

    private final AiSalesService salesService;

    @GetMapping("/stat")
    @PreAuthorize("hasAuthority('ai:salesStat')")
    public ApiResponse<SalesStatResultVO> stat(@RequestParam(required = false) Long productId,
                                               @RequestParam(required = false) String startDate,
                                               @RequestParam(required = false) String endDate,
                                               @RequestParam(required = false) String groupBy) {
        return ApiResponse.ok(salesService.stat(
                SecurityUtils.currentUser().userId(), productId, startDate, endDate, groupBy));
    }

    @PostMapping("/analysis")
    @PreAuthorize("hasAuthority('ai:analysis')")
    public ApiResponse<AiAnalysisVO> analysis(@Valid @RequestBody SalesAnalysisRequest request) {
        return ApiResponse.ok(salesService.analysis(SecurityUtils.currentUser().userId(), request));
    }
}
