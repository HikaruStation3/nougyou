package com.nougyou.aftersale.controller;

import com.nougyou.aftersale.dto.AfterSaleCreateForm;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 买家售后接口：API-061 提交申请 / API-062 我的售后 / API-063 详情（三方共用）。
 */
@RestController
@RequestMapping({"/api/v1/after-sales", "/api/after-sales"})
@RequiredArgsConstructor
public class AfterSaleController {

    private final AfterSaleService afterSaleService;

    @PostMapping
    @PreAuthorize("hasAuthority('afterSale:add')")
    public ApiResponse<AfterSaleVO> create(@Valid @RequestBody AfterSaleCreateForm form) {
        return ApiResponse.ok(afterSaleService.create(SecurityUtils.currentUser().userId(), form));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('afterSale:list')")
    public ApiResponse<PageResult<AfterSaleVO>> myPage(AfterSalePageQuery query) {
        return ApiResponse.ok(afterSaleService.myPage(SecurityUtils.currentUser().userId(), query));
    }

    /**
     * 买家、商家、管理员共用；数据归属在 Service 内校验。
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('afterSale:detail')")
    public ApiResponse<AfterSaleVO> detail(@PathVariable Long id) {
        return ApiResponse.ok(afterSaleService.detail(SecurityUtils.currentUser().userId(), id));
    }

    /**
     * 订单详情页按明细查询售后状态，返回该订单下全部售后单，便于展示每个商品的处理进度。
     */
    @GetMapping("/by-order/{orderId}")
    @PreAuthorize("hasAuthority('afterSale:detail')")
    public ApiResponse<List<AfterSaleVO>> byOrder(@PathVariable Long orderId) {
        return ApiResponse.ok(afterSaleService.byOrder(SecurityUtils.currentUser().userId(), orderId));
    }
}
