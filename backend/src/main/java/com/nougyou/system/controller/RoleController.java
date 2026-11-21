package com.nougyou.system.controller;

import com.nougyou.common.api.ApiResponse;
import com.nougyou.system.entity.SysRole;
import com.nougyou.system.mapper.SysRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
public class RoleController {

    private final SysRoleMapper roleMapper;

    @GetMapping("/options")
    public ApiResponse<List<Map<String, Object>>> options() {
        List<Map<String, Object>> options = roleMapper.selectList(null).stream()
                .map(role -> Map.<String, Object>of(
                        "value", String.valueOf(role.getId()),
                        "label", role.getName()))
                .toList();
        return ApiResponse.ok(options);
    }
}
