package com.nougyou.config;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.nougyou.system.entity.SysDept;
import com.nougyou.system.entity.SysPermission;
import com.nougyou.system.entity.SysRole;
import com.nougyou.system.entity.SysUser;
import com.nougyou.system.mapper.SysDeptMapper;
import com.nougyou.system.mapper.SysPermissionMapper;
import com.nougyou.system.mapper.SysRoleMapper;
import com.nougyou.system.mapper.SysRolePermissionMapper;
import com.nougyou.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 初始化演示数据：默认部门、RBAC 角色与权限点、admin/123456 账号。
 * 重复执行不会产生重复数据。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_FARMER = "FARMER";
    private static final String ROLE_BUYER = "BUYER";

    private static final List<PermissionSeed> PERMISSIONS = List.of(
            new PermissionSeed("用户管理", "sys:user:menu", "MENU", 1),
            new PermissionSeed("用户查询", "sys:user:query", "BUTTON", 2),
            new PermissionSeed("用户新增", "sys:user:create", "BUTTON", 3),
            new PermissionSeed("用户编辑", "sys:user:update", "BUTTON", 4),
            new PermissionSeed("用户删除", "sys:user:delete", "BUTTON", 5),
            new PermissionSeed("重置密码", "sys:user:reset-password", "BUTTON", 6),
            new PermissionSeed("角色查询", "sys:role:query", "BUTTON", 7),
            new PermissionSeed("角色授权", "sys:role:update", "BUTTON", 8),
            new PermissionSeed("角色管理", "sys:role:menu", "MENU", 19),
            new PermissionSeed("AI 对话", "ai:chat:menu", "MENU", 9),
            new PermissionSeed("AI 对话调用", "ai:chat:use", "BUTTON", 10),
            new PermissionSeed("文件上传", "file:upload:menu", "MENU", 11),
            new PermissionSeed("文件上传", "file:upload", "BUTTON", 12),
            new PermissionSeed("文件删除", "file:delete", "BUTTON", 13),
            new PermissionSeed("商品浏览", "product:view", "BUTTON", 14),
            new PermissionSeed("商品管理", "product:manage", "BUTTON", 15),
            new PermissionSeed("下单", "order:create", "BUTTON", 16),
            new PermissionSeed("订单管理", "order:manage", "BUTTON", 17)
    );

    private static final List<String> ALL_PERMISSION_CODES = PERMISSIONS.stream()
            .map(PermissionSeed::code)
            .toList();

    private static final List<String> FARMER_PERMISSION_CODES = List.of(
            "ai:chat:menu", "ai:chat:use",
            "file:upload:menu", "file:upload", "file:delete",
            "product:view", "product:manage", "order:create", "order:manage"
    );

    private static final List<String> BUYER_PERMISSION_CODES = List.of(
            "product:view", "order:create"
    );

    private final SysDeptMapper deptMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserMapper userMapper;
    private final SysPermissionMapper permissionMapper;
    private final SysRolePermissionMapper rolePermissionMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        SysDept root = ensureMerchantRoot();
        SysDept farm = ensureMerchant("绿野农场", root.getId(), 1);
        ensureMerchant("鲜果合作社", root.getId(), 2);
        ensureMerchant("云岭茶庄", root.getId(), 3);

        SysRole adminRole = ensureRole(ROLE_ADMIN, "管理员", 1);
        SysRole farmerRole = ensureRole(ROLE_FARMER, "农户商家", 2);
        SysRole buyerRole = ensureBuyerRole();

        seedPermissions();

        // 管理员始终拥有全部权限点，后续新增权限时会自动补齐
        ensurePermissions(adminRole, ALL_PERMISSION_CODES);
        assignIfEmpty(farmerRole, FARMER_PERMISSION_CODES);
        assignIfEmpty(buyerRole, BUYER_PERMISSION_CODES);

        ensureUser("farmer01", "绿野农场店主", farmerRole.getId(), farm.getId());

        SysUser admin = userMapper.selectOne(Wrappers.<SysUser>lambdaQuery().eq(SysUser::getUsername, "admin"));
        if (admin == null) {
            admin = new SysUser();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("123456"));
            admin.setNickname("管理员");
            admin.setGender(0);
            admin.setStatus(1);
            admin.setDeptId(root.getId());
            admin.setRoleId(adminRole.getId());
            userMapper.insert(admin);
            log.info("已初始化默认账号：admin / 123456");
        }
    }

    /**
     * 商户树根节点，历史数据中的「总公司」会重命名为「商户中心」。
     */
    private SysDept ensureMerchantRoot() {
        SysDept root = deptMapper.selectOne(Wrappers.<SysDept>lambdaQuery()
                .and(w -> w.eq(SysDept::getName, "商户中心").or().eq(SysDept::getName, "总公司"))
                .orderByAsc(SysDept::getId)
                .last("limit 1"));
        if (root == null) {
            root = createDept("商户中心", 0L, 0);
            log.info("已初始化商户根节点：商户中心");
        } else if (!"商户中心".equals(root.getName())) {
            root.setName("商户中心");
            deptMapper.updateById(root);
        }
        return root;
    }

    private SysDept ensureMerchant(String name, Long parentId, int sort) {
        SysDept dept = deptMapper.selectOne(Wrappers.<SysDept>lambdaQuery()
                .eq(SysDept::getName, name)
                .eq(SysDept::getParentId, parentId));
        return dept != null ? dept : createDept(name, parentId, sort);
    }

    private SysDept createDept(String name, Long parentId, int sort) {
        SysDept dept = new SysDept();
        dept.setParentId(parentId);
        dept.setName(name);
        dept.setSort(sort);
        deptMapper.insert(dept);
        return dept;
    }

    private void ensureUser(String username, String nickname, Long roleId, Long deptId) {
        if (userMapper.exists(Wrappers.<SysUser>lambdaQuery().eq(SysUser::getUsername, username))) {
            return;
        }
        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode("123456"));
        user.setNickname(nickname);
        user.setGender(0);
        user.setStatus(1);
        user.setDeptId(deptId);
        user.setRoleId(roleId);
        userMapper.insert(user);
        log.info("已初始化商户测试账号：{} / 123456", username);
    }

    private SysRole ensureRole(String code, String name, int sort) {
        SysRole role = roleMapper.selectOne(Wrappers.<SysRole>lambdaQuery().eq(SysRole::getCode, code));
        if (role == null) {
            role = new SysRole();
            role.setName(name);
            role.setCode(code);
            role.setSort(sort);
            roleMapper.insert(role);
            log.info("已初始化角色：{}（{}）", name, code);
            return role;
        }
        if (!name.equals(role.getName()) || !Integer.valueOf(sort).equals(role.getSort())) {
            role.setName(name);
            role.setSort(sort);
            roleMapper.updateById(role);
        }
        return role;
    }

    /**
     * 兼容旧数据：早期版本的 USER 角色就地升级为 BUYER（买家），用户绑定关系无需变更。
     */
    private SysRole ensureBuyerRole() {
        SysRole buyer = roleMapper.selectOne(Wrappers.<SysRole>lambdaQuery().eq(SysRole::getCode, ROLE_BUYER));
        if (buyer != null) {
            return ensureRole(ROLE_BUYER, "买家", 3);
        }
        SysRole legacy = roleMapper.selectOne(Wrappers.<SysRole>lambdaQuery().eq(SysRole::getCode, "USER"));
        if (legacy != null) {
            legacy.setCode(ROLE_BUYER);
            legacy.setName("买家");
            legacy.setSort(3);
            roleMapper.updateById(legacy);
            log.info("已将旧角色 USER 迁移为 BUYER（买家）");
            return legacy;
        }
        return ensureRole(ROLE_BUYER, "买家", 3);
    }

    private void seedPermissions() {
        Map<String, SysPermission> existing = permissionMapper.selectList(null).stream()
                .collect(Collectors.toMap(SysPermission::getCode, permission -> permission, (a, b) -> a));
        for (PermissionSeed seed : PERMISSIONS) {
            if (existing.containsKey(seed.code())) {
                continue;
            }
            SysPermission permission = new SysPermission();
            permission.setParentId(0L);
            permission.setName(seed.name());
            permission.setCode(seed.code());
            permission.setType(seed.type());
            permission.setSort(seed.sort());
            permissionMapper.insert(permission);
        }
    }

    private void assignIfEmpty(SysRole role, List<String> permissionCodes) {
        if (!rolePermissionMapper.selectPermissionIdsByRoleId(role.getId()).isEmpty()) {
            return;
        }
        ensurePermissions(role, permissionCodes);
    }

    private void ensurePermissions(SysRole role, List<String> permissionCodes) {
        Set<Long> existingIds = new HashSet<>(rolePermissionMapper.selectPermissionIdsByRoleId(role.getId()));
        Map<String, Long> permissionIds = permissionMapper.selectList(null).stream()
                .collect(Collectors.toMap(SysPermission::getCode, SysPermission::getId, (a, b) -> a));
        permissionCodes.stream()
                .map(permissionIds::get)
                .filter(Objects::nonNull)
                .filter(permissionId -> !existingIds.contains(permissionId))
                .forEach(permissionId -> rolePermissionMapper.insertOne(role.getId(), permissionId));
    }

    private record PermissionSeed(String name, String code, String type, int sort) {
    }
}
