package com.nougyou.aftersale.controller;

import com.nougyou.aftersale.dto.AfterSaleHandleForm;
import com.nougyou.aftersale.dto.AfterSalePageQuery;
import com.nougyou.aftersale.dto.AfterSaleVO;
import com.nougyou.aftersale.service.AfterSaleService;
import com.nougyou.common.api.ApiResponse;
import com.nougyou.common.page.PageResult;
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
 * 商家售后接口：API-064 售后列表 / API-065 受理或拒绝。
 */
@RestController
@RequestMapping({"/api/v1/farmer/after-sales", "/api/farmer/after-sales"})
@RequiredArgsConstructor
public class AfterSaleFarmerController {

    private final AfterSaleService afterSaleService;

    @GetMapping
    @PreAuthorize("hasAuthority('afterSale:farmerList')")
    public ApiResponse<PageResult<AfterSaleVO>> page(AfterSalePageQuery query) {
        return ApiResponse.ok(afterSaleService.farmerPage(SecurityUtils.currentUser().userId(), query));
    }

    @PutMapping("/{id}/handle")
    @PreAuthorize("hasAuthority('afterSale:handle')")
    public ApiResponse<AfterSaleVO> handle(@PathVariable Long id,
                                           @Valid @RequestBody AfterSaleHandleForm form) {
        return ApiResponse.ok(afterSaleService.handle(SecurityUtils.currentUser().userId(), id, form));
    }
}
