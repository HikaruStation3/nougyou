package com.nougyou.consult.dto.row;

import lombok.Data;

/**
 * 会话参与人简要信息（只取展示需要的列）。
 */
@Data
public class UserBrief {

    private Long id;
    private String nickname;
    private String avatar;
    /** sys_role.code：ADMIN / FARMER / BUYER */
    private String roleCode;
}
