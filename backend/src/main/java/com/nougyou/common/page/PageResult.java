package com.nougyou.common.page;

import java.util.List;

/**
 * 分页响应结构，与前端 PageResult 保持一致。
 */
public record PageResult<T>(List<T> list, long total) {
}
