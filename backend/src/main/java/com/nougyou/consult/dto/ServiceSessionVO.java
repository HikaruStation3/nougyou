package com.nougyou.consult.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 沟通咨询会话（API-067 ~ 074 出参）。
 */
@Data
@Builder
public class ServiceSessionVO {

    private Long id;
    private String sessionNo;
    /** 发起人 */
    private Long userId;
    private String userNickname;
    private String userRole;
    /** 1 平台客服 / 2 农户商家 */
    private Integer targetType;
    private Long targetId;
    /** 客服会话为「平台客服」，商家会话为商家昵称 */
    private String targetName;
    /** 接入的客服，未接入为空 */
    private Long adminId;
    private String adminName;
    /** 0 排队中 1 进行中 2 已结束 */
    private Integer status;
    /** 会话标题：对方名称，供列表展示 */
    private String title;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastMsgTime;
    /** 当前查看者的未读数 */
    private Integer unreadCount;
    /** 咨询的商品（从商品页发起时） */
    private Long productId;
    private String productName;
    /** 最后一条消息预览 */
    private String lastMsgPreview;
    private Integer lastMsgType;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;
}
