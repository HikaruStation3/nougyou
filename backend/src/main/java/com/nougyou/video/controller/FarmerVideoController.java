package com.nougyou.video.controller;

import com.nougyou.common.api.ApiResponse;
import com.nougyou.common.page.PageResult;
import com.nougyou.security.SecurityUtils;
import com.nougyou.video.dto.MyVideoQuery;
import com.nougyou.video.dto.VideoPublishForm;
import com.nougyou.video.dto.VideoVO;
import com.nougyou.video.service.VideoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 农户商家自助发布短视频（API-077 ~ 079）。
 */
@RestController
@RequestMapping({"/api/v1/farmer/videos", "/api/farmer/videos"})
@RequiredArgsConstructor
public class FarmerVideoController {

    private final VideoService videoService;

    /** API-078 我的短视频：含待审核与驳回记录 */
    @GetMapping
    @PreAuthorize("hasAuthority('video:my')")
    public ApiResponse<PageResult<VideoVO>> myVideos(MyVideoQuery query) {
        return ApiResponse.ok(videoService.myVideos(SecurityUtils.currentUser().userId(), query));
    }

    /** API-077 发布短视频：发布后进入待审核 */
    @PostMapping
    @PreAuthorize("hasAuthority('video:add')")
    public ApiResponse<VideoVO> publish(@Valid @RequestBody VideoPublishForm form) {
        return ApiResponse.ok(videoService.publish(SecurityUtils.currentUser().userId(), form));
    }

    /** API-079 删除短视频 */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('video:delete')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        videoService.deleteVideo(id, SecurityUtils.currentUser().userId());
        return ApiResponse.ok();
    }
}
