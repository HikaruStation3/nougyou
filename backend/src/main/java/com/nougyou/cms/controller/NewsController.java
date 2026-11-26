package com.nougyou.cms.controller;

import com.nougyou.cms.dto.NewsDetailVO;
import com.nougyou.cms.dto.NewsPageQuery;
import com.nougyou.cms.dto.NewsVO;
import com.nougyou.cms.service.NewsService;
import com.nougyou.common.api.ApiResponse;
import com.nougyou.common.page.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 扶贫资讯公开接口（API-027 列表 / API-028 详情），游客可匿名访问。
 */
@RestController
@RequestMapping({"/api/v1/news", "/api/news"})
@RequiredArgsConstructor
public class NewsController {

    private final NewsService newsService;

    @GetMapping
    public ApiResponse<PageResult<NewsVO>> page(NewsPageQuery query) {
        return ApiResponse.ok(newsService.page(query));
    }

    @GetMapping("/{id}")
    public ApiResponse<NewsDetailVO> detail(@PathVariable Long id) {
        return ApiResponse.ok(newsService.detail(id));
    }
}
