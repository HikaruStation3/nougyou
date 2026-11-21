package com.nougyou.system.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UserItemVO {

    private String id;
    private String username;
    private String nickname;
    private String avatar;
    private String mobile;
    private String email;
    private Integer gender;
    private Integer status;
    private String deptName;
    private String roleNames;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
