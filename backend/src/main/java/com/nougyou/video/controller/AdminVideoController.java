package com.nougyou.video.controller;

import com.nougyou.common.api.ApiResponse;
import com.nougyou.common.page.PageResult;
import com.nougyou.security.SecurityUtils;
import com.nougyou.video.dto.AdminVideoQuery;
import com.nougyou.video.dto.VideoAuditForm;
import com.nougyou.video.dto.VideoVO;
import com.nougyou.video.service.VideoService;
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
 * 管理端短视频审核（API-086），附带审核工作台所需的列表查询。
 */
@RestController
@RequestMapping({"/api/v1/admin/videos", "/api/admin/videos"})
@RequiredArgsConstructor
public class AdminVideoController {

    private final VideoService videoService;

    /** 管理端短视频列表：待审核优先 */
    @GetMapping
    @PreAuthorize("hasAuthority('video:adminList')")
    public ApiResponse<PageResult<VideoVO>> page(AdminVideoQuery query) {
        return ApiResponse.ok(videoService.adminPage(query));
    }

    /** API-086 审核短视频 */
    @PutMapping("/{id}/audit")
    @PreAuthorize("hasAuthority('video:audit')")
    public ApiResponse<VideoVO> audit(@PathVariable Long id, @Valid @RequestBody VideoAuditForm form) {
        return ApiResponse.ok(videoService.audit(id, SecurityUtils.currentUser().userId(), form));
    }
}
