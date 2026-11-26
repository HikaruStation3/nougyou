package com.nougyou.cms.controller;

import com.nougyou.cms.dto.NewsForm;
import com.nougyou.cms.dto.NewsPageQuery;
import com.nougyou.cms.dto.NewsVO;
import com.nougyou.cms.service.NewsService;
import com.nougyou.common.api.ApiResponse;
import com.nougyou.common.page.PageResult;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 扶贫资讯后台维护（API-029 新增 / API-030 编辑 / API-031 删除 / API-032 发布下架）。
 * 列表与回显是配套的支撑接口，权限统一用 cms:news:*。
 */
@RestController
@RequestMapping({"/api/v1/admin/news", "/api/admin/news"})
@RequiredArgsConstructor
public class AdminNewsController {

    private final NewsService newsService;

    /** 管理端分页：含草稿与已下架 */
    @GetMapping
    @PreAuthorize("hasAuthority('cms:news:menu')")
    public ApiResponse<PageResult<NewsVO>> page(NewsPageQuery query) {
        return ApiResponse.ok(newsService.adminPage(query));
    }

    /** 编辑回显 */
    @GetMapping("/{id}/form")
    @PreAuthorize("hasAuthority('cms:news:menu')")
    public ApiResponse<NewsForm> form(@PathVariable Long id) {
        return ApiResponse.ok(newsService.form(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('cms:news:create')")
    public ApiResponse<Long> create(@Valid @RequestBody NewsForm form) {
        return ApiResponse.ok(newsService.create(form, SecurityUtils.currentUser().userId()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('cms:news:update')")
    public ApiResponse<Void> update(@PathVariable Long id, @Valid @RequestBody NewsForm form) {
        newsService.update(id, form);
        return ApiResponse.ok();
    }

    /** 批量删除，多个 ID 用英文逗号分隔 */
    @DeleteMapping("/{ids}")
    @PreAuthorize("hasAuthority('cms:news:delete')")
    public ApiResponse<Void> delete(@PathVariable String ids) {
        newsService.delete(ids);
        return ApiResponse.ok();
    }

    /** 发布 / 下架：status=1 发布，status=2 下架，默认发布 */
    @PutMapping("/{id}/publish")
    @PreAuthorize("hasAuthority('cms:news:publish')")
    public ApiResponse<Void> publish(@PathVariable Long id,
                                     @RequestParam(defaultValue = "1") Integer status) {
        newsService.publish(id, status, SecurityUtils.currentUser().userId());
        return ApiResponse.ok();
    }
}
