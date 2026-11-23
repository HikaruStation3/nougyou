package com.nougyou.order.controller;

import com.nougyou.common.api.ApiResponse;
import com.nougyou.common.page.PageResult;
import com.nougyou.order.dto.OrderPageQuery;
import com.nougyou.order.dto.OrderVO;
import com.nougyou.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后台订单查询（API-060）：只有查询权，不改金额与状态。
 */
@RestController
@RequestMapping({"/api/v1/admin/orders", "/api/admin/orders"})
@RequiredArgsConstructor
public class OrderAdminController {

    private final OrderService orderService;

    @GetMapping
    @PreAuthorize("hasAuthority('order:adminList')")
    public ApiResponse<PageResult<OrderVO>> page(OrderPageQuery query) {
        return ApiResponse.ok(orderService.adminPage(query));
    }
}
