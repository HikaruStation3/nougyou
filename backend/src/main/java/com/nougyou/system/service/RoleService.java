package com.nougyou.system.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.nougyou.common.exception.BusinessException;
import com.nougyou.system.dto.RoleVO;
import com.nougyou.system.entity.SysPermission;
import com.nougyou.system.entity.SysRole;
import com.nougyou.system.mapper.SysPermissionMapper;
import com.nougyou.system.mapper.SysRoleMapper;
import com.nougyou.system.mapper.SysRolePermissionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final SysRoleMapper roleMapper;
    private final SysPermissionMapper permissionMapper;
    private final SysRolePermissionMapper rolePermissionMapper;

    public List<RoleVO> list() {
        return roleMapper.selectList(Wrappers.<SysRole>lambdaQuery()
                        .orderByAsc(SysRole::getSort)
                        .orderByAsc(SysRole::getId)).stream()
                .map(role -> new RoleVO(role.getId(), role.getName(), role.getCode(), role.getSort()))
                .toList();
    }

    public List<SysPermission> listPermissions() {
        return permissionMapper.selectList(Wrappers.<SysPermission>lambdaQuery()
                .orderByAsc(SysPermission::getSort)
                .orderByAsc(SysPermission::getId));
    }

    public List<Long> permissionIds(Long roleId) {
        requireRole(roleId);
        return rolePermissionMapper.selectPermissionIdsByRoleId(roleId);
    }

    @Transactional
    public void updatePermissions(Long roleId, List<Long> permissionIds) {
        requireRole(roleId);
        rolePermissionMapper.deleteByRoleId(roleId);
        if (permissionIds == null || permissionIds.isEmpty()) {
            return;
        }
        List<Long> validIds = permissionMapper.selectByIds(permissionIds).stream()
                .map(SysPermission::getId)
                .toList();
        validIds.forEach(permissionId -> rolePermissionMapper.insertOne(roleId, permissionId));
    }

    private SysRole requireRole(Long roleId) {
        SysRole role = roleMapper.selectById(roleId);
        if (role == null) {
            throw new BusinessException("角色不存在");
        }
        return role;
    }
}
