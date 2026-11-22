package com.nougyou.consult.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 创建或获取会话入参（API-067）。
 */
@Data
public class ServiceSessionCreateRequest {

    /** 1 平台客服 / 2 农户商家 */
    @NotNull(message = "会话对象类型不能为空")
    @Min(value = 1, message = "会话对象类型只能是 1（平台客服）或 2（商家）")
    @Max(value = 2, message = "会话对象类型只能是 1（平台客服）或 2（商家）")
    private Integer targetType;

    /** targetType=2 时必填，商家用户 ID */
    private Long targetId;

    /** 从商品页发起咨询时传入，作为会话上下文 */
    private Long productId;
}
