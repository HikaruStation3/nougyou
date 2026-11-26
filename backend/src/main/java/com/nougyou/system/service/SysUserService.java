package com.nougyou.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nougyou.common.api.ResultCode;
import com.nougyou.common.exception.BusinessException;
import com.nougyou.common.page.PageResult;
import com.nougyou.system.dto.UserForm;
import com.nougyou.system.dto.UserFormVO;
import com.nougyou.system.dto.UserInfoVO;
import com.nougyou.system.dto.UserItemVO;
import com.nougyou.system.dto.UserProfileVO;
import com.nougyou.system.dto.UserQuery;
import com.nougyou.system.entity.SysDept;
import com.nougyou.system.entity.SysRole;
import com.nougyou.system.entity.SysUser;
import com.nougyou.system.mapper.SysDeptMapper;
import com.nougyou.system.mapper.SysRoleMapper;
import com.nougyou.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SysUserService {

    private static final String DEFAULT_PASSWORD = "123456";

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysDeptMapper deptMapper;
    private final PermissionService permissionService;
    private final PasswordEncoder passwordEncoder;

    public PageResult<UserItemVO> page(UserQuery query) {
        LambdaQueryWrapper<SysUser> wrapper = Wrappers.lambdaQuery();
        if (StringUtils.hasText(query.getKeywords())) {
            String keywords = query.getKeywords().trim();
            wrapper.and(w -> w.like(SysUser::getUsername, keywords)
                    .or().like(SysUser::getNickname, keywords)
                    .or().like(SysUser::getMobile, keywords));
        }
        if (query.getStatus() != null) {
            wrapper.eq(SysUser::getStatus, query.getStatus());
        }
        if (query.getDeptId() != null) {
            wrapper.eq(SysUser::getDeptId, query.getDeptId());
        }
        wrapper.orderByDesc(SysUser::getId);

        Page<SysUser> page = userMapper.selectPage(new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
        Map<Long, String> roleNames = roleNames();
        Map<Long, String> deptNames = deptNames();

        List<UserItemVO> items = page.getRecords().stream()
                .map(user -> UserItemVO.builder()
                        .id(String.valueOf(user.getId()))
                        .username(user.getUsername())
                        .nickname(user.getNickname())
                        .avatar(user.getAvatar())
                        .mobile(user.getMobile())
                        .email(user.getEmail())
                        .gender(user.getGender())
                        .status(user.getStatus())
                        .deptName(deptNames.get(user.getDeptId()))
                        .roleNames(roleNames.get(user.getRoleId()))
                        .createTime(user.getCreateTime())
                        .build())
                .toList();
        return new PageResult<>(items, page.getTotal());
    }

    public UserFormVO getForm(Long id) {
        SysUser user = requireUser(id);
        UserFormVO form = new UserFormVO();
        form.setId(String.valueOf(user.getId()));
        form.setUsername(user.getUsername());
        form.setNickname(user.getNickname());
        form.setAvatar(user.getAvatar());
        form.setMobile(user.getMobile());
        form.setEmail(user.getEmail());
        form.setGender(user.getGender());
        form.setStatus(user.getStatus());
        form.setDeptId(user.getDeptId() == null ? null : String.valueOf(user.getDeptId()));
        form.setRoleIds(user.getRoleId() == null ? List.of() : List.of(user.getRoleId()));
        return form;
    }

    public void create(UserForm form) {
        if (!StringUtils.hasText(form.getUsername())) {
            throw new BusinessException("用户名不能为空");
        }
        if (userMapper.exists(Wrappers.<SysUser>lambdaQuery().eq(SysUser::getUsername, form.getUsername()))) {
            throw new BusinessException("用户名已存在");
        }
        SysUser user = new SysUser();
        copyForm(form, user);
        String rawPassword = StringUtils.hasText(form.getPassword()) ? form.getPassword() : DEFAULT_PASSWORD;
        user.setPassword(passwordEncoder.encode(rawPassword));
        if (user.getStatus() == null) {
            user.setStatus(1);
        }
        userMapper.insert(user);
    }

    public void update(Long id, UserForm form) {
        requireUser(id);
        if (StringUtils.hasText(form.getUsername())
                && userMapper.exists(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getUsername, form.getUsername()).ne(SysUser::getId, id))) {
            throw new BusinessException("用户名已存在");
        }
        SysUser user = new SysUser();
        user.setId(id);
        copyForm(form, user);
        user.setPassword(null);
        userMapper.updateById(user);
    }

    public void resetPassword(Long id, String password) {
        requireUser(id);
        if (!StringUtils.hasText(password) || password.length() < 6) {
            throw new BusinessException("密码长度不能少于 6 位");
        }
        SysUser user = new SysUser();
        user.setId(id);
        user.setPassword(passwordEncoder.encode(password));
        userMapper.updateById(user);
    }

    public void deleteByIds(String ids) {
        List<Long> idList = Arrays.stream(ids.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(Long::valueOf)
                .toList();
        idList.forEach(userMapper::deleteById);
    }

    public UserInfoVO currentUserInfo(Long userId) {
        SysUser user = requireUser(userId);
        List<String> roles = roleCodes(user);
        SysDept dept = user.getDeptId() == null ? null : deptMapper.selectById(user.getDeptId());
        String deptName = dept == null ? null : dept.getName();
        // 店铺名取商家账号所属部门名；仅农户商家返回，避免买家/管理员看到无意义的“店铺”
        String shopName = roles.contains("FARMER") ? deptName : null;
        return new UserInfoVO(String.valueOf(user.getId()), user.getUsername(), user.getNickname(),
                user.getAvatar(),
                user.getDeptId() == null ? null : String.valueOf(user.getDeptId()),
                deptName, shopName, roles, permissionService.findPermissionCodes(userId));
    }

    public UserProfileVO getProfile(Long userId) {
        SysUser user = requireUser(userId);
        UserProfileVO profile = new UserProfileVO();
        profile.setId(String.valueOf(user.getId()));
        profile.setUsername(user.getUsername());
        profile.setNickname(user.getNickname());
        profile.setAvatar(user.getAvatar());
        profile.setGender(user.getGender());
        profile.setMobile(user.getMobile());
        profile.setEmail(user.getEmail());
        profile.setDeptName(deptNames().get(user.getDeptId()));
        profile.setRoleNames(roleNames().get(user.getRoleId()));
        profile.setCreateTime(user.getCreateTime());
        return profile;
    }

    public void updateProfile(Long userId, UserForm form) {
        requireUser(userId);
        SysUser user = new SysUser();
        user.setId(userId);
        user.setNickname(form.getNickname());
        user.setAvatar(form.getAvatar());
        user.setGender(form.getGender());
        userMapper.updateById(user);
    }

    public void changePassword(Long userId, String oldPassword, String newPassword, String confirmPassword) {
        SysUser user = requireUser(userId);
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BusinessException("原密码不正确");
        }
        if (!StringUtils.hasText(newPassword) || newPassword.length() < 6) {
            throw new BusinessException("新密码长度不能少于 6 位");
        }
        if (!newPassword.equals(confirmPassword)) {
            throw new BusinessException("两次输入的密码不一致");
        }
        SysUser update = new SysUser();
        update.setId(userId);
        update.setPassword(passwordEncoder.encode(newPassword));
        userMapper.updateById(update);
    }

    public List<Map<String, Object>> options() {
        return userMapper.selectList(Wrappers.<SysUser>lambdaQuery().orderByAsc(SysUser::getId)).stream()
                .map(user -> Map.<String, Object>of(
                        "value", String.valueOf(user.getId()),
                        "label", StringUtils.hasText(user.getNickname()) ? user.getNickname() : user.getUsername()))
                .toList();
    }

    public SysUser requireUser(Long id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "用户不存在");
        }
        return user;
    }

    private void copyForm(UserForm form, SysUser user) {
        user.setUsername(form.getUsername());
        user.setNickname(form.getNickname());
        user.setAvatar(form.getAvatar());
        user.setMobile(form.getMobile());
        user.setEmail(form.getEmail());
        user.setGender(form.getGender());
        user.setStatus(form.getStatus());
        user.setDeptId(form.getDeptId());
        if (form.getRoleIds() != null && !form.getRoleIds().isEmpty()) {
            user.setRoleId(form.getRoleIds().get(0));
        }
    }

    private List<String> roleCodes(SysUser user) {
        if (user.getRoleId() == null) {
            return List.of();
        }
        SysRole role = roleMapper.selectById(user.getRoleId());
        return role == null ? List.of() : List.of(role.getCode());
    }

    private Map<Long, String> roleNames() {
        return roleMapper.selectList(null).stream()
                .collect(Collectors.toMap(SysRole::getId, SysRole::getName, (a, b) -> a));
    }

    private Map<Long, String> deptNames() {
        return deptMapper.selectList(null).stream()
                .collect(Collectors.toMap(SysDept::getId, SysDept::getName, (a, b) -> a));
    }
}
