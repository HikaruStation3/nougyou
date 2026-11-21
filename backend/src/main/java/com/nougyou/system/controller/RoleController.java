package com.nougyou.system.controller;

import com.nougyou.common.api.ApiResponse;
import com.nougyou.system.dto.RolePermissionForm;
import com.nougyou.system.dto.RoleVO;
import com.nougyou.system.entity.SysPermission;
import com.nougyou.system.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    /**
     * 用户表单的角色下拉，登录即可访问。
     */
    @GetMapping("/options")
    public ApiResponse<List<Map<String, Object>>> options() {
        List<Map<String, Object>> options = roleService.list().stream()
                .map(role -> Map.<String, Object>of("value", role.id(), "label", role.name()))
                .toList();
        return ApiResponse.ok(options);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('sys:role:query')")
    public ApiResponse<List<RoleVO>> list() {
        return ApiResponse.ok(roleService.list());
    }

    @GetMapping("/permissions")
    @PreAuthorize("hasAuthority('sys:role:query')")
    public ApiResponse<List<SysPermission>> permissions() {
        return ApiResponse.ok(roleService.listPermissions());
    }

    @GetMapping("/{id}/permissions")
    @PreAuthorize("hasAuthority('sys:role:query')")
    public ApiResponse<List<Long>> rolePermissions(@PathVariable Long id) {
        return ApiResponse.ok(roleService.permissionIds(id));
    }

    @PutMapping("/{id}/permissions")
    @PreAuthorize("hasAuthority('sys:role:update')")
    public ApiResponse<Void> updatePermissions(@PathVariable Long id,
                                               @RequestBody RolePermissionForm form) {
        roleService.updatePermissions(id, form.permissionIds());
        return ApiResponse.ok();
    }
}
