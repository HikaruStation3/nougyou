package com.nougyou.log.controller;

import com.nougyou.common.api.ApiResponse;
import com.nougyou.common.page.PageResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 仪表盘访问统计占位接口，接入真实日志后可替换为数据库聚合查询。
 */
@RestController
@RequestMapping("/api/v1/logs")
public class LogController {

    @GetMapping
    public ApiResponse<PageResult<Map<String, Object>>> page() {
        return ApiResponse.ok(new PageResult<>(List.of(), 0));
    }

    @GetMapping("/analytics/overview")
    public ApiResponse<Map<String, Object>> overview() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("todayUvCount", 0);
        data.put("totalUvCount", 0);
        data.put("uvGrowthRate", 0);
        data.put("todayPvCount", 0);
        data.put("totalPvCount", 0);
        data.put("pvGrowthRate", 0);
        return ApiResponse.ok(data);
    }

    @GetMapping("/analytics/trend")
    public ApiResponse<Map<String, Object>> trend(@RequestParam String startDate,
                                                  @RequestParam String endDate) {
        List<String> dates = new ArrayList<>();
        List<Integer> pvList = new ArrayList<>();
        List<Integer> uvList = new ArrayList<>();
        try {
            LocalDate start = LocalDate.parse(startDate);
            LocalDate end = LocalDate.parse(endDate);
            for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
                dates.add(date.toString());
                pvList.add(0);
                uvList.add(0);
            }
        } catch (DateTimeParseException ignored) {
            // 日期格式不正确时返回空数据
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("dates", dates);
        data.put("pvList", pvList);
        data.put("uvList", uvList);
        return ApiResponse.ok(data);
    }
}
