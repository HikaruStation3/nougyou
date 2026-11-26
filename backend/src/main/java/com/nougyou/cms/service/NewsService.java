package com.nougyou.cms.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nougyou.cms.dto.NewsDetailVO;
import com.nougyou.cms.dto.NewsPageQuery;
import com.nougyou.cms.dto.NewsVO;
import com.nougyou.cms.entity.CmsNews;
import com.nougyou.cms.mapper.CmsNewsMapper;
import com.nougyou.common.exception.BusinessException;
import com.nougyou.common.page.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

/**
 * 扶贫资讯公开浏览（API-027 列表 / API-028 详情）。
 * 五个栏目对应 FR-004：景点宣传、旅游推介、乡镇历史、地域文化、人文地理。
 */
@Service
@RequiredArgsConstructor
public class NewsService {

    private static final int PUBLISHED = 1;
    private static final long MAX_PAGE_SIZE = 100;

    /** 栏目字典，与 cms_news.news_type 注释、前端标签保持一致 */
    public static final Map<Integer, String> NEWS_TYPES = Map.of(
            1, "景点宣传",
            2, "旅游推介",
            3, "乡镇历史",
            4, "地域文化",
            5, "人文地理");

    private final CmsNewsMapper newsMapper;

    public PageResult<NewsVO> page(NewsPageQuery query) {
        long pageNum = Math.max(1, query.getPageNum());
        long pageSize = Math.min(Math.max(1, query.getPageSize()), MAX_PAGE_SIZE);

        var wrapper = Wrappers.<CmsNews>lambdaQuery()
                .eq(CmsNews::getPublishStatus, PUBLISHED);
        if (query.getNewsType() != null) {
            wrapper.eq(CmsNews::getNewsType, query.getNewsType());
        }
        if (StringUtils.hasText(query.getKeyword())) {
            String keyword = query.getKeyword().trim();
            wrapper.and(w -> w.like(CmsNews::getTitle, keyword)
                    .or().like(CmsNews::getSummary, keyword));
        }
        wrapper.orderByDesc(CmsNews::getPublishTime).orderByDesc(CmsNews::getId);

        Page<CmsNews> page = newsMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        List<NewsVO> items = page.getRecords().stream().map(this::toVO).toList();
        return new PageResult<>(items, page.getTotal());
    }

    /**
     * 详情：累加浏览量，返回正文与关联地点。
     */
    public NewsDetailVO detail(Long id) {
        CmsNews news = newsMapper.selectById(id);
        if (news == null || !Integer.valueOf(PUBLISHED).equals(news.getPublishStatus())) {
            throw new BusinessException("资讯不存在或已下架");
        }
        newsMapper.update(null, Wrappers.<CmsNews>lambdaUpdate()
                .eq(CmsNews::getId, id)
                .setSql("view_count = view_count + 1"));

        int viewCount = (news.getViewCount() == null ? 0 : news.getViewCount()) + 1;
        return NewsDetailVO.builder()
                .id(news.getId())
                .title(news.getTitle())
                .newsType(news.getNewsType())
                .newsTypeName(typeName(news.getNewsType()))
                .cover(news.getCover())
                .summary(news.getSummary())
                .location(news.getLocation())
                .publishStatus(news.getPublishStatus())
                .publishTime(news.getPublishTime())
                .viewCount(viewCount)
                .createTime(news.getCreateTime())
                .content(news.getContent())
                .build();
    }

    private NewsVO toVO(CmsNews news) {
        return NewsVO.builder()
                .id(news.getId())
                .title(news.getTitle())
                .newsType(news.getNewsType())
                .newsTypeName(typeName(news.getNewsType()))
                .cover(news.getCover())
                .summary(news.getSummary())
                .location(news.getLocation())
                .publishStatus(news.getPublishStatus())
                .publishTime(news.getPublishTime())
                .viewCount(news.getViewCount())
                .createTime(news.getCreateTime())
                .build();
    }

    private String typeName(Integer newsType) {
        return newsType == null ? "乡镇资讯" : NEWS_TYPES.getOrDefault(newsType, "乡镇资讯");
    }
}
