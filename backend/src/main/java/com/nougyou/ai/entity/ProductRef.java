package com.nougyou.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 商品只读视图，仅映射 AI 统计/分析需要的列，避免与商品模块耦合。
 */
@Data
@TableName("product")
public class ProductRef {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long farmerId;
    private Long categoryId;
    private String name;
    private String cover;
    private BigDecimal price;
    private Integer stock;
    private String unit;
    private Integer saleCount;
    private Integer shelfStatus;
    @TableLogic
    private Integer deleted;
}
