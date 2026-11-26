package com.nougyou.cms.controller;

import com.nougyou.cms.dto.PolicyDetailVO;
import com.nougyou.cms.dto.PolicyPageQuery;
import com.nougyou.cms.dto.PolicyVO;
import com.nougyou.cms.service.PolicyService;
import com.nougyou.common.api.ApiResponse;
import com.nougyou.common.page.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 扶贫政策公开接口（API-021 列表 / API-022 详情），游客可匿名访问。
 */
@RestController
@RequestMapping({"/api/v1/policies", "/api/policies"})
@RequiredArgsConstructor
public class PolicyController {

    private final PolicyService policyService;

    @GetMapping
    public ApiResponse<PageResult<PolicyVO>> page(PolicyPageQuery query) {
        return ApiResponse.ok(policyService.page(query));
    }

    @GetMapping("/{id}")
    public ApiResponse<PolicyDetailVO> detail(@PathVariable Long id) {
        return ApiResponse.ok(policyService.detail(id));
    }
}
