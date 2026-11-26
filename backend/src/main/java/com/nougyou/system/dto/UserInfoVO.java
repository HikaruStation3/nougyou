package com.nougyou.system.dto;

import java.util.List;

/**
 * 当前登录用户信息，字段与前端 UserInfo 对应。
 */
public record UserInfoVO(
        String userId,
        String username,
        String nickname,
        String avatar,
        /** 所属部门ID */
        String deptId,
        /** 部门名称；商户账号的部门名即店铺名 */
        String deptName,
        /** 店铺名称：仅农户商家返回，买家/管理员为 null */
        String shopName,
        List<String> roles,
        List<String> perms
) {
}
