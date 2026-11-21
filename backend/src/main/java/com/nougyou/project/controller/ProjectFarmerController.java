package com.nougyou.project.controller;

import com.nougyou.common.api.ApiResponse;
import com.nougyou.common.page.PageResult;
import com.nougyou.project.dto.ProjectForm;
import com.nougyou.project.dto.ProjectPageQuery;
import com.nougyou.project.dto.ProjectShelfForm;
import com.nougyou.project.dto.ProjectVO;
import com.nougyou.project.service.ProjectService;
import com.nougyou.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 农户商家农家乐项目接口（API-035 申请 / 036 我的 / 037 编辑 / 038 上下架）。
 */
@RestController
@RequestMapping({"/api/v1/farmer/projects", "/api/farmer/projects"})
@RequiredArgsConstructor
public class ProjectFarmerController {

    private final ProjectService projectService;

    @PostMapping
    @PreAuthorize("hasAuthority('project:add')")
    public ApiResponse<ProjectVO> create(@Valid @RequestBody ProjectForm form) {
        return ApiResponse.ok(projectService.create(SecurityUtils.currentUser().userId(), form));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('project:my')")
    public ApiResponse<PageResult<ProjectVO>> my(ProjectPageQuery query) {
        return ApiResponse.ok(projectService.myPage(SecurityUtils.currentUser().userId(), query));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('project:edit')")
    public ApiResponse<ProjectVO> update(@PathVariable Long id, @Valid @RequestBody ProjectForm form) {
        return ApiResponse.ok(projectService.update(SecurityUtils.currentUser().userId(), id, form));
    }

    @PutMapping("/{id}/shelf")
    @PreAuthorize("hasAuthority('project:shelf')")
    public ApiResponse<ProjectVO> shelf(@PathVariable Long id, @Valid @RequestBody ProjectShelfForm form) {
        return ApiResponse.ok(projectService.shelf(SecurityUtils.currentUser().userId(), id, form));
    }
}
