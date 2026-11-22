package com.nougyou.cart.dto;

import lombok.Data;

import java.util.List;

/**
 * 批量删除购物车行（API-051），不传 ids 表示清空全部。
 */
@Data
public class CartBatchDeleteForm {

    /** 购物车行 ID 列表，为空时清空当前买家的全部购物车 */
    private List<Long> ids;
}
