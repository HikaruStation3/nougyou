package com.nougyou.cms.controller;

import com.nougyou.cms.dto.PolicyForm;
import com.nougyou.cms.dto.PolicyPageQuery;
import com.nougyou.cms.dto.PolicyVO;
import com.nougyou.cms.service.PolicyService;
import com.nougyou.common.api.ApiResponse;
import com.nougyou.common.page.PageResult;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 扶贫政策后台维护（API-023 新增 / API-024 编辑 / API-025 删除 / API-026 发布下架）。
 * 列表与回显是配套的支撑接口，权限统一用 cms:policy:*。
 */
@RestController
@RequestMapping({"/api/v1/admin/policies", "/api/admin/policies"})
@RequiredArgsConstructor
public class AdminPolicyController {

    private final PolicyService policyService;

    /** 管理端分页：含草稿与已下架 */
    @GetMapping
    @PreAuthorize("hasAuthority('cms:policy:menu')")
    public ApiResponse<PageResult<PolicyVO>> page(PolicyPageQuery query) {
        return ApiResponse.ok(policyService.adminPage(query));
    }

    /** 编辑回显 */
    @GetMapping("/{id}/form")
    @PreAuthorize("hasAuthority('cms:policy:menu')")
    public ApiResponse<PolicyForm> form(@PathVariable Long id) {
        return ApiResponse.ok(policyService.form(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('cms:policy:create')")
    public ApiResponse<Long> create(@Valid @RequestBody PolicyForm form) {
        return ApiResponse.ok(policyService.create(form, SecurityUtils.currentUser().userId()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('cms:policy:update')")
    public ApiResponse<Void> update(@PathVariable Long id, @Valid @RequestBody PolicyForm form) {
        policyService.update(id, form);
        return ApiResponse.ok();
    }

    /** 批量删除，多个 ID 用英文逗号分隔 */
    @DeleteMapping("/{ids}")
    @PreAuthorize("hasAuthority('cms:policy:delete')")
    public ApiResponse<Void> delete(@PathVariable String ids) {
        policyService.delete(ids);
        return ApiResponse.ok();
    }

    /** 发布 / 下架：status=1 发布，status=2 下架，默认发布 */
    @PutMapping("/{id}/publish")
    @PreAuthorize("hasAuthority('cms:policy:publish')")
    public ApiResponse<Void> publish(@PathVariable Long id,
                                     @RequestParam(defaultValue = "1") Integer status) {
        policyService.publish(id, status, SecurityUtils.currentUser().userId());
        return ApiResponse.ok();
    }
}
