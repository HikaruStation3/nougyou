package com.nougyou.farmer.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class FarmerApplyVO {

    private String id;
    private String applyNo;
    private String userId;
    private String username;
    private String realName;
    private String idCard;
    private String phone;
    private String village;
    private String povertyCardNo;
    private String povertyLevel;
    private Integer householdNum;
    private String mainProduct;
    private String idCardFront;
    private String idCardBack;
    private String povertyProof;
    private String applyDesc;
    private Integer auditStatus;
    private String auditStatusText;
    private String auditUserId;
    private String auditUsername;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime auditTime;
    private String auditRemark;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
