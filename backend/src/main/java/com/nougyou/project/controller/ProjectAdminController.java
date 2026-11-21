package com.nougyou.project.controller;

import com.nougyou.common.api.ApiResponse;
import com.nougyou.common.page.PageResult;
import com.nougyou.project.dto.ProjectAuditForm;
import com.nougyou.project.dto.ProjectPageQuery;
import com.nougyou.project.dto.ProjectVO;
import com.nougyou.project.service.ProjectService;
import com.nougyou.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理员农家乐项目接口（API-039 审核，及审核配套的项目列表）。
 */
@RestController
@RequestMapping({"/api/v1/admin/projects", "/api/admin/projects"})
@RequiredArgsConstructor
public class ProjectAdminController {

    private final ProjectService projectService;

    @GetMapping
    @PreAuthorize("hasAuthority('project:audit')")
    public ApiResponse<PageResult<ProjectVO>> page(ProjectPageQuery query) {
        return ApiResponse.ok(projectService.adminPage(query));
    }

    @PutMapping("/{id}/audit")
    @PreAuthorize("hasAuthority('project:audit')")
    public ApiResponse<ProjectVO> audit(@PathVariable Long id, @Valid @RequestBody ProjectAuditForm form) {
        return ApiResponse.ok(projectService.audit(id, SecurityUtils.currentUser().userId(), form));
    }
}
