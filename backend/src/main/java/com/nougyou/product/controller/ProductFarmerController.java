package com.nougyou.product.controller;

import com.nougyou.common.api.ApiResponse;
import com.nougyou.common.page.PageResult;
import com.nougyou.product.dto.ProductDetailVO;
import com.nougyou.product.dto.ProductForm;
import com.nougyou.product.dto.ProductPageQuery;
import com.nougyou.product.dto.ProductShelfForm;
import com.nougyou.product.dto.ProductVO;
import com.nougyou.product.service.ProductService;
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

/**
 * 农户商家农产品接口（API-043 上架 / API-044 编辑 / API-045 上下架 / API-046 删除）。
 */
@RestController
@RequestMapping({"/api/v1/farmer/products", "/api/farmer/products"})
@RequiredArgsConstructor
public class ProductFarmerController {

    private final ProductService productService;

    @GetMapping
    @PreAuthorize("hasAuthority('product:my')")
    public ApiResponse<PageResult<ProductVO>> my(ProductPageQuery query) {
        return ApiResponse.ok(productService.myPage(SecurityUtils.currentUser().userId(), query));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('product:my')")
    public ApiResponse<ProductDetailVO> detail(@PathVariable Long id) {
        return ApiResponse.ok(productService.myDetail(SecurityUtils.currentUser().userId(), id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('product:add')")
    public ApiResponse<ProductVO> create(@Valid @RequestBody ProductForm form) {
        return ApiResponse.ok(productService.create(SecurityUtils.currentUser().userId(), form));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('product:edit')")
    public ApiResponse<ProductVO> update(@PathVariable Long id, @Valid @RequestBody ProductForm form) {
        return ApiResponse.ok(productService.update(SecurityUtils.currentUser().userId(), id, form));
    }

    @PutMapping("/{id}/shelf")
    @PreAuthorize("hasAuthority('product:shelf')")
    public ApiResponse<ProductVO> shelf(@PathVariable Long id, @Valid @RequestBody ProductShelfForm form) {
        return ApiResponse.ok(productService.shelf(SecurityUtils.currentUser().userId(), id, form));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('product:delete')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        productService.delete(SecurityUtils.currentUser().userId(), id);
        return ApiResponse.ok();
    }
}
