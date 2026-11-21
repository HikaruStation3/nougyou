package com.nougyou.system.dto;

import lombok.Data;

import java.util.List;

@Data
public class UserForm {

    private Long id;
    private String username;
    private String password;
    private String nickname;
    private String avatar;
    private String mobile;
    private String email;
    private Integer gender;
    private Integer status;
    private Long deptId;
    private List<Long> roleIds;
}
