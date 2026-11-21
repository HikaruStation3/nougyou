package com.nougyou.system.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserProfileVO {

    private String id;
    private String username;
    private String nickname;
    private String avatar;
    private Integer gender;
    private String mobile;
    private String email;
    private String deptName;
    private String roleNames;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
