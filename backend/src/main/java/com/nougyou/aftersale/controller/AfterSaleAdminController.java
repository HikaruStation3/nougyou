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
 * 后台售后（API-066 平台介入）：列表用于筛出商家已拒绝、需要终裁的售后单。
 */
@RestController
@RequestMapping({"/api/v1/admin/after-sales", "/api/admin/after-sales"})
@RequiredArgsConstructor
public class AfterSaleAdminController {

    private final AfterSaleService afterSaleService;

    @GetMapping
    @PreAuthorize("hasAuthority('afterSale:intervene')")
    public ApiResponse<PageResult<AfterSaleVO>> page(AfterSalePageQuery query) {
        return ApiResponse.ok(afterSaleService.adminPage(query));
    }

    @PutMapping("/{id}/handle")
    @PreAuthorize("hasAuthority('afterSale:intervene')")
    public ApiResponse<AfterSaleVO> handle(@PathVariable Long id,
                                           @Valid @RequestBody AfterSaleHandleForm form) {
        return ApiResponse.ok(afterSaleService.intervene(SecurityUtils.currentUser().userId(), id, form));
    }
}
