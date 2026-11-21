package com.nougyou.system.dto;

import lombok.Data;

@Data
public class UserQuery {

    private long pageNum = 1;
    private long pageSize = 10;
    private String keywords;
    private Integer status;
    private Long deptId;
}
