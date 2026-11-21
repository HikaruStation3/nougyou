package com.nougyou.farmer.dto;

import lombok.Data;

@Data
public class FarmerApplyQuery {

    private long pageNum = 1;
    private long pageSize = 10;
    private Integer auditStatus;
    private String keywords;
}
