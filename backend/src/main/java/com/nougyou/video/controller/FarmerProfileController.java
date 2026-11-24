package com.nougyou.video.controller;

import com.nougyou.common.api.ApiResponse;
import com.nougyou.security.SecurityUtils;
import com.nougyou.video.dto.FarmerProfileVO;
import com.nougyou.video.service.VideoService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 商家主页与关注（API-084 / 085）。
 */
@RestController
@RequestMapping({"/api/v1/farmers", "/api/farmers"})
@RequiredArgsConstructor
public class FarmerProfileController {

    private final VideoService videoService;

    /** API-085 商家主页：资料、在售商品与已发布视频 */
    @GetMapping("/{id}")
    public ApiResponse<FarmerProfileVO> profile(@PathVariable Long id) {
        return ApiResponse.ok(videoService.farmerProfile(id, SecurityUtils.currentUserIdOrNull()));
    }

    /** API-084 关注 / 取消关注 */
    @PostMapping("/{id}/follow")
    @PreAuthorize("hasAuthority('video:follow')")
    public ApiResponse<FarmerProfileVO> follow(@PathVariable Long id) {
        return ApiResponse.ok(videoService.toggleFollow(id, SecurityUtils.currentUser().userId()));
    }
}
