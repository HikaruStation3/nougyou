package com.nougyou.config;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.nougyou.system.entity.SysDept;
import com.nougyou.system.entity.SysRole;
import com.nougyou.system.entity.SysUser;
import com.nougyou.system.mapper.SysDeptMapper;
import com.nougyou.system.mapper.SysRoleMapper;
import com.nougyou.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 初始化演示数据：默认部门、管理员角色、admin/123456 账号。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final SysDeptMapper deptMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        SysDept dept = deptMapper.selectOne(Wrappers.<SysDept>lambdaQuery().eq(SysDept::getName, "总公司"));
        if (dept == null) {
            dept = new SysDept();
            dept.setParentId(0L);
            dept.setName("总公司");
            dept.setSort(1);
            deptMapper.insert(dept);
        }

        SysRole role = roleMapper.selectOne(Wrappers.<SysRole>lambdaQuery().eq(SysRole::getCode, "ADMIN"));
        if (role == null) {
            role = new SysRole();
            role.setName("超级管理员");
            role.setCode("ADMIN");
            role.setSort(1);
            roleMapper.insert(role);
        }

        SysRole userRole = roleMapper.selectOne(Wrappers.<SysRole>lambdaQuery().eq(SysRole::getCode, "USER"));
        if (userRole == null) {
            userRole = new SysRole();
            userRole.setName("普通用户");
            userRole.setCode("USER");
            userRole.setSort(2);
            roleMapper.insert(userRole);
        }

        SysUser admin = userMapper.selectOne(Wrappers.<SysUser>lambdaQuery().eq(SysUser::getUsername, "admin"));
        if (admin == null) {
            admin = new SysUser();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("123456"));
            admin.setNickname("管理员");
            admin.setGender(0);
            admin.setStatus(1);
            admin.setDeptId(dept.getId());
            admin.setRoleId(role.getId());
            userMapper.insert(admin);
            log.info("已初始化默认账号：admin / 123456");
        }
    }
}
