package com.nougyou.product.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 农产品上下架表单（API-045）。
 */
@Data
public class ProductShelfForm {

    /** 目标状态：0 下架 / 1 上架 */
    @Min(value = 0, message = "上架状态仅支持 0 下架 / 1 上架")
    @Max(value = 1, message = "上架状态仅支持 0 下架 / 1 上架")
    private Integer shelfStatus;
}
