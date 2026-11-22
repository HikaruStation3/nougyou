package com.nougyou.ai.dto;

import java.math.BigDecimal;

/**
 * 按月销量走势点。
 *
 * @param month  yyyy-MM
 * @param qty    当月销量（件）
 * @param amount 当月销售额（元）
 */
public record SalesPointVO(String month, int qty, BigDecimal amount) {
}
