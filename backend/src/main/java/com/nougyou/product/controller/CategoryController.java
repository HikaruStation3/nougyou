package com.nougyou.product.controller;

import com.nougyou.common.api.ApiResponse;
import com.nougyou.product.dto.CategoryQuery;
import com.nougyou.product.dto.CategoryVO;
import com.nougyou.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 农产品分类接口（API-040 分类树），游客可匿名访问。
 */
@RestController
@RequestMapping({"/api/v1/categories", "/api/categories"})
@RequiredArgsConstructor
public class CategoryController {

    private final ProductService productService;

    @GetMapping
    public ApiResponse<List<CategoryVO>> tree(CategoryQuery query) {
        return ApiResponse.ok(productService.categoryTree(query.getOnlyEnabled()));
    }
}
