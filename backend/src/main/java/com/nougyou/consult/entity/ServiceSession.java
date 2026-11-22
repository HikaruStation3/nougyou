package com.nougyou.consult.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 客服/沟通会话（FR-009 平台客服 / FR-010 联系商家 / FR-032 客服响应）。
 */
@Data
@TableName("service_session")
public class ServiceSession {

    /** 会话对象：平台客服 */
    public static final int TARGET_PLATFORM = 1;
    /** 会话对象：农户商家 */
    public static final int TARGET_FARMER = 2;

    /** 排队中：平台客服会话等待管理员接入 */
    public static final int STATUS_WAITING = 0;
    /** 进行中 */
    public static final int STATUS_ACTIVE = 1;
    /** 已结束 */
    public static final int STATUS_CLOSED = 2;

    @TableId(type = IdType.AUTO)
    private Long id;
    private String sessionNo;
    private Long userId;
    /** 发起方角色 BUYER / FARMER */
    private String userRole;
    /** 1 平台客服 2 农户商家 */
    private Integer targetType;
    /** 联系商家时为商家用户 ID */
    private Long targetId;
    /** 从商品页发起咨询时记录的商品 ID */
    private Long productId;
    /** 接待管理员 ID，为空即待接入 */
    private Long adminId;
    /** 0 排队中 1 进行中 2 已结束 */
    private Integer status;
    private LocalDateTime lastMsgTime;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
