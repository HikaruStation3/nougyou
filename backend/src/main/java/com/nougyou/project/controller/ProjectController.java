package com.nougyou.project.controller;

import com.nougyou.common.api.ApiResponse;
import com.nougyou.common.page.PageResult;
import com.nougyou.project.dto.ProjectPageQuery;
import com.nougyou.project.dto.ProjectVO;
import com.nougyou.project.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 农家乐项目公开接口（API-033 列表 / API-034 详情），游客可匿名访问。
 */
@RestController
@RequestMapping({"/api/v1/projects", "/api/projects"})
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @GetMapping
    public ApiResponse<PageResult<ProjectVO>> page(ProjectPageQuery query) {
        return ApiResponse.ok(projectService.page(query));
    }

    @GetMapping("/{id}")
    public ApiResponse<ProjectVO> detail(@PathVariable Long id) {
        return ApiResponse.ok(projectService.detail(id));
    }
}
