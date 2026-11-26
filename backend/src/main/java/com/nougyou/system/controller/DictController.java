package com.nougyou.system.controller;

import com.nougyou.common.api.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 演示用静态字典，后续可替换为数据库字典表。
 */
@RestController
@RequestMapping("/api/v1/dicts")
public class DictController {

    private static final Map<String, List<Map<String, Object>>> DICTS = Map.of(
            "gender", List.of(
                    item(0, "未知", "info"),
                    item(1, "男", "primary"),
                    item(2, "女", "danger")),
            "status", List.of(
                    item(1, "启用", "success"),
                    item(0, "禁用", "info")),
            // 通知公告：类型 / 等级，与 notice 表注释一致
            "notice_type", List.of(
                    item(1, "通知", "primary"),
                    item(2, "公告", "success"),
                    item(3, "提醒", "warning")),
            "notice_level", List.of(
                    item("L", "普通", "info"),
                    item("M", "重要", "warning"),
                    item("H", "紧急", "danger"))
    );

    @GetMapping("/{dictCode}/items/options")
    public ApiResponse<List<Map<String, Object>>> options(@PathVariable String dictCode) {
        return ApiResponse.ok(DICTS.getOrDefault(dictCode, List.of()));
    }

    private static Map<String, Object> item(int value, String label, String tagType) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("value", value);
        item.put("label", label);
        item.put("tagType", tagType);
        return item;
    }

    private static Map<String, Object> item(String value, String label, String tagType) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("value", value);
        item.put("label", label);
        item.put("tagType", tagType);
        return item;
    }
}
