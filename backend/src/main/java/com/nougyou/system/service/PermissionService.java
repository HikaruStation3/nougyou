package com.nougyou.system.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.nougyou.system.entity.SysPermission;
import com.nougyou.system.entity.SysUser;
import com.nougyou.system.mapper.SysPermissionMapper;
import com.nougyou.system.mapper.SysRolePermissionMapper;
import com.nougyou.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 权限查询：用户 -> 角色 -> 权限点。
 */
@Service
@RequiredArgsConstructor
public class PermissionService {

    private final SysUserMapper userMapper;
    private final SysPermissionMapper permissionMapper;
    private final SysRolePermissionMapper rolePermissionMapper;

    public List<String> findPermissionCodes(Long userId) {
        SysUser user = userMapper.selectById(userId);
        if (user == null || user.getRoleId() == null) {
            return List.of();
        }
        return rolePermissionMapper.selectCodesByRoleId(user.getRoleId());
    }

    public List<SysPermission> listPermissions() {
        return permissionMapper.selectList(Wrappers.<SysPermission>lambdaQuery()
                .orderByAsc(SysPermission::getSort)
                .orderByAsc(SysPermission::getId));
    }
}
