package com.nougyou.entrust.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 委托关系出参（API-087 ~ API-092）。
 */
@Data
@Builder
public class EntrustVO {

    private String id;
    private String entrustNo;
    private String entrustUserId;
    private String entrustUserName;
    private String agentUserId;
    private String agentUserName;
    private String productId;
    private String productName;
    private String productCover;
    /** 受托方分成比例（百分数） */
    private BigDecimal splitRatio;
    /** 1 按月 / 2 按季 / 3 按单 */
    private Integer settleCycle;
    private String agreementDesc;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;
    /** 0 待确认 / 1 生效中 / 2 已终止 / 3 已拒绝 */
    private Integer status;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime confirmTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
