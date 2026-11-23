package com.nougyou.order.controller;

import com.nougyou.common.api.ApiResponse;
import com.nougyou.common.page.PageResult;
import com.nougyou.order.dto.OrderCancelForm;
import com.nougyou.order.dto.OrderCreateForm;
import com.nougyou.order.dto.OrderPageQuery;
import com.nougyou.order.dto.OrderPayForm;
import com.nougyou.order.dto.OrderVO;
import com.nougyou.order.service.OrderService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 买家订单接口：API-052 提交 / API-053 列表 / API-054 详情 / API-055 支付 / API-056 取消 / API-057 确认收货。
 */
@RestController
@RequestMapping({"/api/v1/orders", "/api/orders"})
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @PreAuthorize("hasAuthority('order:create')")
    public ApiResponse<List<OrderVO>> create(@Valid @RequestBody OrderCreateForm form) {
        return ApiResponse.ok(orderService.create(SecurityUtils.currentUser().userId(), form));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('order:list')")
    public ApiResponse<PageResult<OrderVO>> myPage(OrderPageQuery query) {
        return ApiResponse.ok(orderService.myPage(SecurityUtils.currentUser().userId(), query));
    }

    /**
     * 买家、商家、管理员共用；数据归属在 Service 内校验。
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('order:detail')")
    public ApiResponse<OrderVO> detail(@PathVariable Long id) {
        return ApiResponse.ok(orderService.detail(SecurityUtils.currentUser().userId(), id));
    }

    @PutMapping("/{id}/pay")
    @PreAuthorize("hasAuthority('order:pay')")
    public ApiResponse<OrderVO> pay(@PathVariable Long id,
                                    @RequestBody(required = false) OrderPayForm form) {
        return ApiResponse.ok(orderService.pay(SecurityUtils.currentUser().userId(), id, form));
    }

    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('order:cancel')")
    public ApiResponse<OrderVO> cancel(@PathVariable Long id,
                                       @Valid @RequestBody(required = false) OrderCancelForm form) {
        String reason = form == null ? null : form.getReason();
        return ApiResponse.ok(orderService.cancel(SecurityUtils.currentUser().userId(), id, reason));
    }

    @PutMapping("/{id}/confirm")
    @PreAuthorize("hasAuthority('order:confirm')")
    public ApiResponse<OrderVO> confirm(@PathVariable Long id) {
        return ApiResponse.ok(orderService.confirm(SecurityUtils.currentUser().userId(), id));
    }
}
