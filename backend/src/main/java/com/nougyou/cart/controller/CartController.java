package com.nougyou.cart.controller;

import com.nougyou.common.api.ApiResponse;
import com.nougyou.cart.dto.CartAddForm;
import com.nougyou.cart.dto.CartBatchDeleteForm;
import com.nougyou.cart.dto.CartUpdateForm;
import com.nougyou.cart.dto.CartVO;
import com.nougyou.cart.service.CartService;
import com.nougyou.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 买家购物车接口（API-047 列表 / API-048 加购 / API-049 修改 / API-050 删除 / API-051 批量删除）。
 */
@RestController
@RequestMapping({"/api/v1/cart", "/api/cart"})
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping
    @PreAuthorize("hasAuthority('cart:list')")
    public ApiResponse<List<CartVO>> list() {
        return ApiResponse.ok(cartService.list(SecurityUtils.currentUser().userId()));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('cart:add')")
    public ApiResponse<CartVO> add(@Valid @RequestBody CartAddForm form) {
        return ApiResponse.ok(cartService.add(SecurityUtils.currentUser().userId(), form));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('cart:edit')")
    public ApiResponse<CartVO> update(@PathVariable Long id, @Valid @RequestBody CartUpdateForm form) {
        return ApiResponse.ok(cartService.update(SecurityUtils.currentUser().userId(), id, form));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('cart:delete')")
    public ApiResponse<Void> remove(@PathVariable Long id) {
        cartService.remove(SecurityUtils.currentUser().userId(), id);
        return ApiResponse.ok();
    }

    @DeleteMapping
    @PreAuthorize("hasAuthority('cart:clear')")
    public ApiResponse<Void> removeBatch(@RequestBody(required = false) CartBatchDeleteForm form) {
        cartService.removeBatch(SecurityUtils.currentUser().userId(), form == null ? null : form.getIds());
        return ApiResponse.ok();
    }
}
