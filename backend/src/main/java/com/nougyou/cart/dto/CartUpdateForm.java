package com.nougyou.cart.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 修改购物车表单（API-049）。
 */
@Data
public class CartUpdateForm {

    @Min(value = 1, message = "购买数量至少为 1")
    @Max(value = 999, message = "购买数量不能超过 999")
    private Integer quantity;

    /** 是否勾选：0 否 / 1 是 */
    @Min(value = 0, message = "勾选状态仅支持 0 / 1")
    @Max(value = 1, message = "勾选状态仅支持 0 / 1")
    private Integer selected;
}
