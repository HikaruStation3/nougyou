package com.nougyou.cms.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nougyou.cms.dto.NewsDetailVO;
import com.nougyou.cms.dto.NewsForm;
import com.nougyou.cms.dto.NewsPageQuery;
import com.nougyou.cms.dto.NewsVO;
import com.nougyou.cms.entity.CmsNews;
import com.nougyou.cms.mapper.CmsNewsMapper;
import com.nougyou.common.exception.BusinessException;
import com.nougyou.common.page.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 扶贫资讯：前台公开浏览（API-027 列表 / API-028 详情）+ 后台维护（API-029~032）。
 * 五个栏目对应 FR-004：景点宣传、旅游推介、乡镇历史、地域文化、人文地理。
 */
@Service
@RequiredArgsConstructor
public class NewsService {

    private static final int DRAFT = 0;
    private static final int PUBLISHED = 1;
    private static final int OFFLINE = 2;
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

    // ==================== 管理端（API-029~032） ====================

    /** 管理端分页：包含草稿与已下架，支持标题/摘要、栏目与状态筛选 */
    public PageResult<NewsVO> adminPage(NewsPageQuery query) {
        long pageNum = Math.max(1, query.getPageNum());
        long pageSize = Math.min(Math.max(1, query.getPageSize()), MAX_PAGE_SIZE);

        var wrapper = Wrappers.<CmsNews>lambdaQuery();
        if (StringUtils.hasText(query.getKeyword())) {
            String keyword = query.getKeyword().trim();
            wrapper.and(w -> w.like(CmsNews::getTitle, keyword)
                    .or().like(CmsNews::getSummary, keyword));
        }
        if (query.getNewsType() != null) {
            wrapper.eq(CmsNews::getNewsType, query.getNewsType());
        }
        if (query.getPublishStatus() != null) {
            wrapper.eq(CmsNews::getPublishStatus, query.getPublishStatus());
        }
        wrapper.orderByDesc(CmsNews::getCreateTime).orderByDesc(CmsNews::getId);

        Page<CmsNews> page = newsMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        List<NewsVO> items = page.getRecords().stream().map(this::toVO).toList();
        return new PageResult<>(items, page.getTotal());
    }

    /** 编辑回显 */
    public NewsForm form(Long id) {
        CmsNews news = require(id);
        NewsForm form = new NewsForm();
        form.setId(news.getId());
        form.setTitle(news.getTitle());
        form.setNewsType(news.getNewsType());
        form.setCover(news.getCover());
        form.setSummary(news.getSummary());
        form.setContent(news.getContent());
        form.setLocation(news.getLocation());
        return form;
    }

    /** 新增：一律先落草稿，确认无误后再由 API-032 发布 */
    @Transactional
    public Long create(NewsForm form, Long authorId) {
        CmsNews news = new CmsNews();
        applyForm(news, form);
        news.setPublishStatus(DRAFT);
        news.setViewCount(0);
        news.setAuthorId(authorId);
        newsMapper.insert(news);
        return news.getId();
    }

    @Transactional
    public void update(Long id, NewsForm form) {
        CmsNews news = require(id);
        applyForm(news, form);
        newsMapper.updateById(news);
    }

    /** 批量删除，多个 ID 用英文逗号分隔；逻辑删除，前台立刻查不到 */
    @Transactional
    public void delete(String ids) {
        List<Long> idList = parseIds(ids);
        if (idList.isEmpty()) {
            throw new BusinessException("请选择要删除的资讯");
        }
        newsMapper.deleteByIds(idList);
    }

    /** 发布 / 下架：status=1 发布（已下架可重新上架），status=2 下架 */
    @Transactional
    public void publish(Long id, Integer status, Long operatorId) {
        CmsNews news = require(id);
        int target = status == null ? PUBLISHED : status;
        if (target != PUBLISHED && target != OFFLINE) {
            throw new BusinessException("发布状态只能是 1（发布）或 2（下架）");
        }
        if (target == OFFLINE && !Integer.valueOf(PUBLISHED).equals(news.getPublishStatus())) {
            throw new BusinessException("只有已发布的资讯可以下架");
        }
        if (target == PUBLISHED && Integer.valueOf(PUBLISHED).equals(news.getPublishStatus())) {
            throw new BusinessException("该资讯已发布");
        }

        var update = Wrappers.<CmsNews>lambdaUpdate()
                .eq(CmsNews::getId, id)
                .set(CmsNews::getPublishStatus, target);
        if (target == PUBLISHED) {
            // 首次发布记录发布时间；再次上架保留原发布时间，避免前台排序跳动
            if (news.getPublishTime() == null) {
                update.set(CmsNews::getPublishTime, LocalDateTime.now());
            }
            if (news.getAuthorId() == null) {
                update.set(CmsNews::getAuthorId, operatorId);
            }
        }
        newsMapper.update(null, update);
    }

    private void applyForm(CmsNews news, NewsForm form) {
        news.setTitle(form.getTitle().trim());
        news.setNewsType(form.getNewsType());
        news.setCover(trimToNull(form.getCover()));
        news.setSummary(trimToNull(form.getSummary()));
        news.setContent(form.getContent());
        news.setLocation(trimToNull(form.getLocation()));
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private CmsNews require(Long id) {
        CmsNews news = newsMapper.selectById(id);
        if (news == null) {
            throw new BusinessException("资讯不存在");
        }
        return news;
    }

    private List<Long> parseIds(String ids) {
        if (!StringUtils.hasText(ids)) {
            return List.of();
        }
        List<Long> result = new ArrayList<>();
        for (String part : ids.split(",")) {
            String value = part.trim();
            if (value.isEmpty()) {
                continue;
            }
            try {
                result.add(Long.valueOf(value));
            } catch (NumberFormatException ex) {
                throw new BusinessException("资讯ID格式不正确：" + value);
            }
        }
        return result;
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
