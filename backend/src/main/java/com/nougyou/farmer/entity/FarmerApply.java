package com.nougyou.farmer.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("farmer_apply")
public class FarmerApply {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String applyNo;
    private Long userId;
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
    private Long auditUserId;
    private LocalDateTime auditTime;
    private String auditRemark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
