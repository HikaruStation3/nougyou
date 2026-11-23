package com.nougyou.order.controller;

import com.nougyou.common.api.ApiResponse;
import com.nougyou.common.page.PageResult;
import com.nougyou.order.dto.OrderPageQuery;
import com.nougyou.order.dto.OrderShipForm;
import com.nougyou.order.dto.OrderVO;
import com.nougyou.order.service.OrderService;
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
 * 商家订单接口：API-058 待处理订单列表 / API-059 发货。
 */
@RestController
@RequestMapping({"/api/v1/farmer/orders", "/api/farmer/orders"})
@RequiredArgsConstructor
public class OrderFarmerController {

    private final OrderService orderService;

    @GetMapping
    @PreAuthorize("hasAuthority('order:farmerList')")
    public ApiResponse<PageResult<OrderVO>> page(OrderPageQuery query) {
        return ApiResponse.ok(orderService.farmerPage(SecurityUtils.currentUser().userId(), query));
    }

    @PutMapping("/{id}/ship")
    @PreAuthorize("hasAuthority('order:ship')")
    public ApiResponse<OrderVO> ship(@PathVariable Long id, @Valid @RequestBody OrderShipForm form) {
        return ApiResponse.ok(orderService.ship(SecurityUtils.currentUser().userId(), id, form));
    }
}
