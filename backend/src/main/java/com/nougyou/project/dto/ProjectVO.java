package com.nougyou.project.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ProjectVO {

    private String id;
    private String farmerId;
    /** 商家姓名 */
    private String farmerName;
    /** 店铺名称 */
    private String shopName;
    private String title;
    private String projectType;
    private String cover;
    private List<String> images;
    private String address;
    private String intro;
    private String priceDesc;
    private Integer capacity;
    private String contactPhone;
    private String businessHours;
    /** 审核状态：0待审核 1通过 2驳回 */
    private Integer auditStatus;
    private String auditRemark;
    /** 上架状态：0下架 1上架 */
    private Integer shelfStatus;
    private Integer viewCount;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
