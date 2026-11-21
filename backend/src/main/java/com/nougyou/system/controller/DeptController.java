package com.nougyou.system.controller;

import com.nougyou.common.api.ApiResponse;
import com.nougyou.system.entity.SysDept;
import com.nougyou.system.mapper.SysDeptMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/depts")
@RequiredArgsConstructor
public class DeptController {

    private final SysDeptMapper deptMapper;

    @GetMapping("/options")
    public ApiResponse<List<Map<String, Object>>> options() {
        List<SysDept> depts = deptMapper.selectList(null);
        Map<Long, Map<String, Object>> nodes = new LinkedHashMap<>();
        depts.forEach(dept -> {
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("value", String.valueOf(dept.getId()));
            node.put("label", dept.getName());
            nodes.put(dept.getId(), node);
        });

        List<Map<String, Object>> roots = new ArrayList<>();
        depts.forEach(dept -> {
            Map<String, Object> node = nodes.get(dept.getId());
            Map<String, Object> parent = nodes.get(dept.getParentId());
            if (parent == null || dept.getParentId() == null || dept.getParentId() == 0L) {
                roots.add(node);
            } else {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> children =
                        (List<Map<String, Object>>) parent.computeIfAbsent("children", key -> new ArrayList<>());
                children.add(node);
            }
        });
        return ApiResponse.ok(roots);
    }
}
