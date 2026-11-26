package com.nougyou.notice.controller;

import com.nougyou.common.api.ApiResponse;
import com.nougyou.common.page.PageResult;
import com.nougyou.notice.dto.NoticeDetailVO;
import com.nougyou.notice.dto.NoticeForm;
import com.nougyou.notice.dto.NoticePageQuery;
import com.nougyou.notice.dto.NoticeVO;
import com.nougyou.notice.service.NoticeService;
import com.nougyou.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 通知公告：管理端发布管理 + 全站用户的「我的通知」。
 * <p>
 * 管理接口要求 sys:notice:* 权限点；我的通知、详情、全部已读只要求登录。
 */
@RestController
@RequestMapping({"/api/v1/notices", "/api/notices"})
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService noticeService;

    /** 管理端分页：含草稿与已撤回，支持标题/发布状态筛选 */
    @GetMapping
    @PreAuthorize("hasAuthority('sys:notice:menu')")
    public ApiResponse<PageResult<NoticeVO>> page(NoticePageQuery query) {
        return ApiResponse.ok(noticeService.adminPage(query));
    }

    /** 我的通知：已发布且发给我（全体或指定）的通知，可按未读筛选 */
    @GetMapping("/my")
    public ApiResponse<PageResult<NoticeVO>> my(NoticePageQuery query) {
        return ApiResponse.ok(noticeService.myPage(query, SecurityUtils.currentUser().userId()));
    }

    /** 编辑回显 */
    @GetMapping("/{id}/form")
    @PreAuthorize("hasAuthority('sys:notice:menu')")
    public ApiResponse<NoticeForm> form(@PathVariable Long id) {
        return ApiResponse.ok(noticeService.form(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('sys:notice:create')")
    public ApiResponse<Long> create(@Valid @RequestBody NoticeForm form) {
        return ApiResponse.ok(noticeService.create(form, SecurityUtils.currentUser().userId()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('sys:notice:update')")
    public ApiResponse<Void> update(@PathVariable Long id, @Valid @RequestBody NoticeForm form) {
        noticeService.update(id, form);
        return ApiResponse.ok();
    }

    /** 批量删除，多个 ID 用英文逗号分隔 */
    @DeleteMapping("/{ids}")
    @PreAuthorize("hasAuthority('sys:notice:delete')")
    public ApiResponse<Void> delete(@PathVariable String ids) {
        noticeService.delete(ids);
        return ApiResponse.ok();
    }

    @PutMapping("/{id}/publish")
    @PreAuthorize("hasAuthority('sys:notice:publish')")
    public ApiResponse<Void> publish(@PathVariable Long id) {
        noticeService.publish(id, SecurityUtils.currentUser().userId());
        return ApiResponse.ok();
    }

    @PutMapping("/{id}/revoke")
    @PreAuthorize("hasAuthority('sys:notice:revoke')")
    public ApiResponse<Void> revoke(@PathVariable Long id) {
        noticeService.revoke(id);
        return ApiResponse.ok();
    }

    /** 详情：管理员看全部；普通用户只能看发给自己的已发布通知，查看即已读 */
    @GetMapping("/{id}/detail")
    public ApiResponse<NoticeDetailVO> detail(@PathVariable Long id) {
        Long userId = SecurityUtils.currentUser().userId();
        boolean admin = SecurityUtils.hasAuthority("sys:notice:menu");
        return ApiResponse.ok(noticeService.detail(id, userId, admin));
    }

    /** 一键全部已读 */
    @PutMapping("/read-all")
    public ApiResponse<Void> readAll() {
        noticeService.readAll(SecurityUtils.currentUser().userId());
        return ApiResponse.ok();
    }
}
