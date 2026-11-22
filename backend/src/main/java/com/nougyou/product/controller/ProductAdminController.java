package com.nougyou.product.controller;

import com.nougyou.common.api.ApiResponse;
import com.nougyou.common.page.PageResult;
import com.nougyou.product.dto.CategoryForm;
import com.nougyou.product.dto.CategoryVO;
import com.nougyou.product.dto.ProductPageQuery;
import com.nougyou.product.dto.ProductVO;
import com.nougyou.product.service.ProductService;
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
 * 管理端农产品与分类管理接口（配套 FR-030 平台内容增删改查）。
 */
@RestController
@RequestMapping({"/api/v1/admin/products", "/api/admin/products"})
@RequiredArgsConstructor
public class ProductAdminController {

    private final ProductService productService;

    @GetMapping
    @PreAuthorize("hasAuthority('product:manage')")
    public ApiResponse<PageResult<ProductVO>> page(ProductPageQuery query) {
        return ApiResponse.ok(productService.adminPage(query));
    }

    @GetMapping("/categories")
    @PreAuthorize("hasAuthority('product:manage')")
    public ApiResponse<List<CategoryVO>> categories() {
        return ApiResponse.ok(productService.categoryTree(Boolean.FALSE));
    }

    @PostMapping("/categories")
    @PreAuthorize("hasAuthority('product:manage')")
    public ApiResponse<CategoryVO> createCategory(@Valid @RequestBody CategoryForm form) {
        return ApiResponse.ok(productService.createCategory(form));
    }

    @PutMapping("/categories/{id}")
    @PreAuthorize("hasAuthority('product:manage')")
    public ApiResponse<CategoryVO> updateCategory(@PathVariable Long id, @Valid @RequestBody CategoryForm form) {
        return ApiResponse.ok(productService.updateCategory(id, form));
    }

    @DeleteMapping("/categories/{id}")
    @PreAuthorize("hasAuthority('product:manage')")
    public ApiResponse<Void> deleteCategory(@PathVariable Long id) {
        productService.deleteCategory(id);
        return ApiResponse.ok();
    }
}
