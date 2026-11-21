package com.nougyou.system.dto;

import lombok.Data;

import java.util.List;

@Data
public class UserFormVO {

    private String id;
    private String username;
    private String nickname;
    private String avatar;
    private String mobile;
    private String email;
    private Integer gender;
    private Integer status;
    private String deptId;
    private List<Long> roleIds;
}
