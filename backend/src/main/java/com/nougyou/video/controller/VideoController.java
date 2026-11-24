package com.nougyou.video.controller;

import com.nougyou.common.api.ApiResponse;
import com.nougyou.common.page.PageResult;
import com.nougyou.security.SecurityUtils;
import com.nougyou.video.dto.VideoCommentForm;
import com.nougyou.video.dto.VideoCommentVO;
import com.nougyou.video.dto.VideoPageQuery;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 短视频观看与互动（API-075 / 076 / 080 ~ 083）。
 * 列表、详情、评论列表对游客开放；点赞、评论、删除需要登录。
 */
@RestController
@RequestMapping({"/api/v1/videos", "/api/videos"})
@RequiredArgsConstructor
public class VideoController {

    private final VideoService videoService;

    /** API-075 短视频列表：审核通过的视频，游客可浏览 */
    @GetMapping
    public ApiResponse<PageResult<VideoVO>> page(VideoPageQuery query) {
        return ApiResponse.ok(videoService.publicPage(query, SecurityUtils.currentUserIdOrNull()));
    }

    /** API-076 短视频详情：含当前用户的点赞与关注状态 */
    @GetMapping("/{id}")
    public ApiResponse<VideoVO> detail(@PathVariable Long id) {
        return ApiResponse.ok(videoService.detail(id, SecurityUtils.currentUserIdOrNull()));
    }

    /** 播放量上报：播放页真正开始播放时调用 */
    @PostMapping("/{id}/view")
    public ApiResponse<Void> view(@PathVariable Long id) {
        videoService.recordView(id);
        return ApiResponse.ok();
    }

    /** API-080 点赞 / 取消点赞 */
    @PostMapping("/{id}/like")
    @PreAuthorize("hasAuthority('video:like')")
    public ApiResponse<VideoVO> like(@PathVariable Long id) {
        return ApiResponse.ok(videoService.toggleLike(id, SecurityUtils.currentUser().userId()));
    }

    /** API-081 评论列表：一级评论分页，回复归并在 children 中 */
    @GetMapping("/{id}/comments")
    public ApiResponse<PageResult<VideoCommentVO>> comments(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize) {
        return ApiResponse.ok(videoService.comments(
                id, pageNum, pageSize, SecurityUtils.currentUserIdOrNull()));
    }

    /** API-082 发表评论或回复 */
    @PostMapping("/{id}/comments")
    @PreAuthorize("hasAuthority('video:comment')")
    public ApiResponse<VideoCommentVO> comment(@PathVariable Long id,
                                               @Valid @RequestBody VideoCommentForm form) {
        return ApiResponse.ok(videoService.addComment(id, SecurityUtils.currentUser().userId(), form));
    }

    /** API-083 删除评论：本人 / 视频作者 / 管理员 */
    @DeleteMapping("/comments/{id}")
    @PreAuthorize("hasAuthority('video:commentDelete')")
    public ApiResponse<Void> deleteComment(@PathVariable Long id) {
        videoService.deleteComment(id, SecurityUtils.currentUser().userId());
        return ApiResponse.ok();
    }
}
