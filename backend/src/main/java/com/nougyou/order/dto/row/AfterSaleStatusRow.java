package com.nougyou.order.dto.row;

import lombok.Data;

/**
 * 订单明细的售后状态，用于拼装 OrderItemVO.afterSaleStatus。
 */
@Data
public class AfterSaleStatusRow {

    private Long orderItemId;
    /** 0 待处理 / 1 已受理 / 2 已完成 / 3 已拒绝 */
    private Integer status;
}
