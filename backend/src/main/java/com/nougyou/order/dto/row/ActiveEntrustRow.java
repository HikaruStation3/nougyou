package com.nougyou.order.dto.row;

import lombok.Data;

/**
 * 商品唯一生效中的委托关系，用于把代销订单归属到受托方。
 */
@Data
public class ActiveEntrustRow {

    private Long id;
    private Long agentUserId;
}
