package com.nougyou.product.controller;

import com.nougyou.common.api.ApiResponse;
import com.nougyou.common.page.PageResult;
import com.nougyou.product.dto.ProductDetailVO;
import com.nougyou.product.dto.ProductPageQuery;
import com.nougyou.product.dto.ProductVO;
import com.nougyou.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 农产品公开接口（API-041 列表 / API-042 详情），游客可匿名访问。
 */
@RestController
@RequestMapping({"/api/v1/products", "/api/products"})
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public ApiResponse<PageResult<ProductVO>> page(ProductPageQuery query) {
        return ApiResponse.ok(productService.page(query));
    }

    @GetMapping("/{id}")
    public ApiResponse<ProductDetailVO> detail(@PathVariable Long id) {
        return ApiResponse.ok(productService.detail(id));
    }
}
