package com.nougyou.home.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.nougyou.cms.entity.CmsNews;
import com.nougyou.cms.entity.CmsPolicy;
import com.nougyou.cms.mapper.CmsNewsMapper;
import com.nougyou.cms.mapper.CmsPolicyMapper;
import com.nougyou.home.dto.HomeNewsVO;
import com.nougyou.home.dto.HomePolicyVO;
import com.nougyou.home.dto.HomeVO;
import com.nougyou.product.service.ProductService;
import com.nougyou.project.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 首页聚合（API-111 / FR-002）。
 * 首页是只读聚合，不建统计表：政策与资讯直接读 CMS 表，农家乐与商品复用各自模块的公开查询口径，
 * 保证首页展示与列表页、详情页看到的是同一批数据。
 */
@Service
@RequiredArgsConstructor
public class HomeService {

    private static final int PUBLISHED = 1;
    private static final int POLICY_LIMIT = 5;
    private static final int NEWS_LIMIT = 8;
    private static final int PROJECT_LIMIT = 4;
    private static final int HOT_PRODUCT_LIMIT = 8;

    private static final Map<Integer, String> NEWS_TYPE_NAMES = Map.of(
            1, "景点宣传",
            2, "旅游推介",
            3, "乡镇历史",
            4, "地域文化",
            5, "人文地理"
    );

    private final CmsPolicyMapper policyMapper;
    private final CmsNewsMapper newsMapper;
    private final ProjectService projectService;
    private final ProductService productService;

    public HomeVO home() {
        return HomeVO.builder()
                .policies(latestPolicies())
                .news(latestNews())
                .projects(projectService.recommendList(PROJECT_LIMIT))
                .hotProducts(productService.hotList(HOT_PRODUCT_LIMIT))
                .build();
    }

    private List<HomePolicyVO> latestPolicies() {
        List<CmsPolicy> records = policyMapper.selectList(Wrappers.<CmsPolicy>lambdaQuery()
                .eq(CmsPolicy::getPublishStatus, PUBLISHED)
                .orderByDesc(CmsPolicy::getPublishTime)
                .orderByDesc(CmsPolicy::getId)
                .last("LIMIT " + POLICY_LIMIT));
        return records.stream().map(this::toPolicyVO).toList();
    }

    private List<HomeNewsVO> latestNews() {
        List<CmsNews> records = newsMapper.selectList(Wrappers.<CmsNews>lambdaQuery()
                .eq(CmsNews::getPublishStatus, PUBLISHED)
                .orderByDesc(CmsNews::getPublishTime)
                .orderByDesc(CmsNews::getId)
                .last("LIMIT " + NEWS_LIMIT));
        return records.stream().map(this::toNewsVO).toList();
    }

    private HomePolicyVO toPolicyVO(CmsPolicy policy) {
        return HomePolicyVO.builder()
                .id(String.valueOf(policy.getId()))
                .title(policy.getTitle())
                .policyNo(policy.getPolicyNo())
                .issueDept(policy.getIssueDept())
                .cover(policy.getCover())
                .summary(policy.getSummary())
                .publishStatus(policy.getPublishStatus())
                .publishTime(policy.getPublishTime())
                .viewCount(policy.getViewCount())
                .authorId(policy.getAuthorId() == null ? null : String.valueOf(policy.getAuthorId()))
                .createTime(policy.getCreateTime())
                .build();
    }

    private HomeNewsVO toNewsVO(CmsNews news) {
        return HomeNewsVO.builder()
                .id(String.valueOf(news.getId()))
                .title(news.getTitle())
                .newsType(news.getNewsType())
                .newsTypeName(NEWS_TYPE_NAMES.getOrDefault(news.getNewsType(), "其他"))
                .cover(news.getCover())
                .summary(news.getSummary())
                .location(news.getLocation())
                .publishStatus(news.getPublishStatus())
                .publishTime(news.getPublishTime())
                .viewCount(news.getViewCount())
                .createTime(news.getCreateTime())
                .build();
    }
}
