package com.nougyou.project.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 扶贫项目（农家乐）。
 */
@Data
@TableName("farm_project")
public class FarmProject {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属农户商家用户ID */
    private Long farmerId;

    private String title;

    /** 项目类型，当前固定为 农家乐 */
    private String projectType;

    private String cover;

    /** 环境图集，逗号分隔存储 */
    private String images;

    private String address;

    private String intro;

    private String priceDesc;

    private Integer capacity;

    private String contactPhone;

    private String businessHours;

    /** 审核状态：0待审核 1通过 2驳回 */
    private Integer auditStatus;

    private Long auditUserId;

    private LocalDateTime auditTime;

    private String auditRemark;

    /** 上架状态：1已上架 0已下架 */
    private Integer shelfStatus;

    private Integer viewCount;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
