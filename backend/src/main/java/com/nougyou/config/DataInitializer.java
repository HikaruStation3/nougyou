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
            new PermissionSeed("农产品列表", "product:list", "BUTTON", 50),
            new PermissionSeed("农产品详情", "product:detail", "BUTTON", 51),
            new PermissionSeed("我的农产品", "product:my", "BUTTON", 61),
            new PermissionSeed("上架农产品", "product:add", "BUTTON", 52),
            new PermissionSeed("编辑农产品", "product:edit", "BUTTON", 53),
            new PermissionSeed("商品上下架", "product:shelf", "BUTTON", 54),
            new PermissionSeed("删除农产品", "product:delete", "BUTTON", 55),
            new PermissionSeed("购物车列表", "cart:list", "BUTTON", 56),
            new PermissionSeed("加入购物车", "cart:add", "BUTTON", 57),
            new PermissionSeed("修改购物车", "cart:edit", "BUTTON", 58),
            new PermissionSeed("删除购物车行", "cart:delete", "BUTTON", 59),
            new PermissionSeed("清空购物车", "cart:clear", "BUTTON", 60),
            new PermissionSeed("下单", "order:create", "BUTTON", 16),
            new PermissionSeed("订单管理", "order:manage", "BUTTON", 17),
            new PermissionSeed("入驻申请提交", "farmerApply:submit", "BUTTON", 20),
            new PermissionSeed("我的入驻申请", "farmerApply:my", "BUTTON", 21),
            new PermissionSeed("入驻申请列表", "farmerApply:list", "BUTTON", 22),
            new PermissionSeed("入驻申请详情", "farmerApply:detail", "BUTTON", 23),
            new PermissionSeed("入驻申请审核", "farmerApply:audit", "BUTTON", 24),
            new PermissionSeed("农家乐列表", "project:list", "BUTTON", 30),
            new PermissionSeed("农家乐详情", "project:detail", "BUTTON", 31),
            new PermissionSeed("申请农家乐项目", "project:add", "BUTTON", 32),
            new PermissionSeed("我的农家乐项目", "project:my", "BUTTON", 33),
            new PermissionSeed("编辑农家乐项目", "project:edit", "BUTTON", 34),
            new PermissionSeed("项目上下架", "project:shelf", "BUTTON", 35),
            new PermissionSeed("农家乐项目审核", "project:audit", "BUTTON", 36),
            new PermissionSeed("沟通咨询", "service:list", "MENU", 40),
            new PermissionSeed("发起咨询", "service:create", "BUTTON", 41),
            new PermissionSeed("会话消息", "service:message", "BUTTON", 42),
            new PermissionSeed("发送消息", "service:send", "BUTTON", 43),
            new PermissionSeed("标记已读", "service:read", "BUTTON", 44),
            new PermissionSeed("客服工作台", "service:adminList", "MENU", 45),
            new PermissionSeed("客服接入会话", "service:accept", "BUTTON", 46),
            new PermissionSeed("客服结束会话", "service:close", "BUTTON", 47),
            new PermissionSeed("我的订单", "order:list", "BUTTON", 62),
            new PermissionSeed("订单详情", "order:detail", "BUTTON", 63),
            new PermissionSeed("支付订单", "order:pay", "BUTTON", 64),
            new PermissionSeed("取消订单", "order:cancel", "BUTTON", 65),
            new PermissionSeed("确认收货", "order:confirm", "BUTTON", 66),
            new PermissionSeed("商家订单列表", "order:farmerList", "BUTTON", 67),
            new PermissionSeed("订单发货", "order:ship", "BUTTON", 68),
            new PermissionSeed("后台订单查询", "order:adminList", "BUTTON", 69),
            new PermissionSeed("建立委托关系", "entrust:add", "BUTTON", 70),
            new PermissionSeed("委托关系列表", "entrust:list", "BUTTON", 71),
            new PermissionSeed("委托关系详情", "entrust:detail", "BUTTON", 72),
            new PermissionSeed("确认委托", "entrust:confirm", "BUTTON", 73),
            new PermissionSeed("终止委托", "entrust:terminate", "BUTTON", 74),
            new PermissionSeed("委托销售统计", "entrust:stat", "BUTTON", 75),
            new PermissionSeed("委托结算单", "entrust:settlement", "BUTTON", 76),
            new PermissionSeed("生成结算单", "entrust:settle", "BUTTON", 77),
            new PermissionSeed("商家经营概览", "dashboard:view", "BUTTON", 78),
            // 短视频（FR-011 / FR-012 / FR-020 / FR-021 / FR-035）
            new PermissionSeed("助农短视频", "video:list", "MENU", 80),
            new PermissionSeed("短视频详情", "video:detail", "BUTTON", 81),
            new PermissionSeed("短视频点赞", "video:like", "BUTTON", 82),
            new PermissionSeed("短视频评论列表", "video:commentList", "BUTTON", 83),
            new PermissionSeed("发表短视频评论", "video:comment", "BUTTON", 84),
            new PermissionSeed("删除短视频评论", "video:commentDelete", "BUTTON", 85),
            new PermissionSeed("关注商家", "video:follow", "BUTTON", 86),
            new PermissionSeed("商家主页", "video:farmerProfile", "BUTTON", 87),
            new PermissionSeed("发布短视频", "video:add", "BUTTON", 88),
            new PermissionSeed("我的短视频", "video:my", "BUTTON", 89),
            new PermissionSeed("删除短视频", "video:delete", "BUTTON", 90),
            new PermissionSeed("短视频列表(管理端)", "video:adminList", "BUTTON", 91),
            new PermissionSeed("短视频审核", "video:audit", "BUTTON", 92),
            // 通知公告：管理员发布，全站用户在顶部铃铛与「我的通知」接收
            new PermissionSeed("通知公告", "sys:notice:menu", "MENU", 93),
            new PermissionSeed("新增通知", "sys:notice:create", "BUTTON", 94),
            new PermissionSeed("编辑通知", "sys:notice:update", "BUTTON", 95),
            new PermissionSeed("删除通知", "sys:notice:delete", "BUTTON", 96),
            new PermissionSeed("发布通知", "sys:notice:publish", "BUTTON", 97),
            new PermissionSeed("撤回通知", "sys:notice:revoke", "BUTTON", 98),
            // 内容资讯（FR-003 / FR-004 / FR-030）：后台维护政策与资讯
            new PermissionSeed("政策管理", "cms:policy:menu", "MENU", 99),
            new PermissionSeed("新增政策", "cms:policy:create", "BUTTON", 100),
            new PermissionSeed("编辑政策", "cms:policy:update", "BUTTON", 101),
            new PermissionSeed("删除政策", "cms:policy:delete", "BUTTON", 102),
            new PermissionSeed("政策发布下架", "cms:policy:publish", "BUTTON", 103),
            new PermissionSeed("资讯管理", "cms:news:menu", "MENU", 104),
            new PermissionSeed("新增资讯", "cms:news:create", "BUTTON", 105),
            new PermissionSeed("编辑资讯", "cms:news:update", "BUTTON", 106),
            new PermissionSeed("删除资讯", "cms:news:delete", "BUTTON", 107),
            new PermissionSeed("资讯发布下架", "cms:news:publish", "BUTTON", 108),
            // 售后服务（FR-008 买家提交与查询 / FR-019 商家处理与平台介入）
            new PermissionSeed("提交售后申请", "afterSale:add", "BUTTON", 109),
            new PermissionSeed("我的售后列表", "afterSale:list", "BUTTON", 110),
            new PermissionSeed("售后详情", "afterSale:detail", "BUTTON", 111),
            new PermissionSeed("商家售后列表", "afterSale:farmerList", "BUTTON", 112),
            new PermissionSeed("处理售后", "afterSale:handle", "BUTTON", 113),
            new PermissionSeed("平台介入处理", "afterSale:intervene", "BUTTON", 114)
    );

    private static final List<String> ALL_PERMISSION_CODES = PERMISSIONS.stream()
            .map(PermissionSeed::code)
            .toList();

    private static final List<String> FARMER_PERMISSION_CODES = List.of(
            "ai:chat:menu", "ai:chat:use",
            "file:upload:menu", "file:upload", "file:delete",
            "product:view", "order:create", "order:manage",
            "product:list", "product:detail", "product:my",
            "product:add", "product:edit", "product:shelf", "product:delete",
            "order:detail", "order:farmerList", "order:ship",
            // 售后：本店待办处理（FR-019）
            "afterSale:farmerList", "afterSale:handle", "afterSale:detail",
            "entrust:add", "entrust:list", "entrust:detail", "entrust:confirm",
            "entrust:terminate", "entrust:stat", "entrust:settlement",
            "dashboard:view",
            "farmerApply:my",
            "project:list", "project:detail",
            "project:add", "project:my", "project:edit", "project:shelf",
            "service:list", "service:create", "service:message", "service:send", "service:read",
            // 短视频：观看与互动 + 发布管理（FR-011 / FR-012 / FR-020 / FR-021）
            "video:list", "video:detail", "video:like", "video:commentList",
            "video:comment", "video:commentDelete", "video:follow", "video:farmerProfile",
            "video:add", "video:my", "video:delete"
    );

    private static final List<String> BUYER_PERMISSION_CODES = List.of(
            // 文件上传：买家提交入驻申请时要传身份证与贫困证明（API-113）
            "file:upload",
            "product:view", "order:create",
            "product:list", "product:detail",
            "cart:list", "cart:add", "cart:edit", "cart:delete", "cart:clear",
            "order:list", "order:detail", "order:pay", "order:cancel", "order:confirm",
            // 售后：提交诉求与查询进度（FR-008）
            "afterSale:add", "afterSale:list", "afterSale:detail",
            "farmerApply:submit", "farmerApply:my",
            "project:list", "project:detail",
            "service:list", "service:create", "service:message", "service:send", "service:read",
            // 短视频：买家以观看、点赞、评论、关注为主（FR-011 / FR-012）
            "video:list", "video:detail", "video:like", "video:commentList",
            "video:comment", "video:commentDelete", "video:follow", "video:farmerProfile"
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
        ensurePermissions(farmerRole, FARMER_PERMISSION_CODES);
        // 历史版本曾给农户授予 product:manage，会导致后台露出全平台商品与分类菜单，此处统一回收
        revokePermissions(farmerRole, List.of("product:manage"));
        ensurePermissions(buyerRole, BUYER_PERMISSION_CODES);

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

    /**
     * 回收角色上不再应该拥有的权限点，用于修正历史数据里多授的管理端权限。
     */
    private void revokePermissions(SysRole role, List<String> permissionCodes) {
        Map<String, Long> permissionIds = permissionMapper.selectList(null).stream()
                .collect(Collectors.toMap(SysPermission::getCode, SysPermission::getId, (a, b) -> a));
        for (String code : permissionCodes) {
            Long permissionId = permissionIds.get(code);
            if (permissionId != null) {
                rolePermissionMapper.deleteOne(role.getId(), permissionId);
            }
        }
    }

    private record PermissionSeed(String name, String code, String type, int sort) {
    }
}
