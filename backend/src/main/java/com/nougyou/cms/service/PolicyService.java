package com.nougyou.cms.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nougyou.cms.dto.PolicyDetailVO;
import com.nougyou.cms.dto.PolicyPageQuery;
import com.nougyou.cms.dto.PolicyVO;
import com.nougyou.cms.entity.CmsPolicy;
import com.nougyou.cms.mapper.CmsPolicyMapper;
import com.nougyou.common.exception.BusinessException;
import com.nougyou.common.page.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 扶贫政策公开浏览（API-021 列表 / API-022 详情）。
 * 只暴露 publish_status = 1 的记录，草稿与已下架内容前台一律不可见。
 */
@Service
@RequiredArgsConstructor
public class PolicyService {

    private static final int PUBLISHED = 1;
    private static final long MAX_PAGE_SIZE = 100;

    private final CmsPolicyMapper policyMapper;

    public PageResult<PolicyVO> page(PolicyPageQuery query) {
        long pageNum = Math.max(1, query.getPageNum());
        long pageSize = Math.min(Math.max(1, query.getPageSize()), MAX_PAGE_SIZE);

        var wrapper = Wrappers.<CmsPolicy>lambdaQuery()
                .eq(CmsPolicy::getPublishStatus, PUBLISHED);
        if (StringUtils.hasText(query.getKeyword())) {
            String keyword = query.getKeyword().trim();
            wrapper.and(w -> w.like(CmsPolicy::getTitle, keyword)
                    .or().like(CmsPolicy::getPolicyNo, keyword));
        }
        if (StringUtils.hasText(query.getIssueDept())) {
            wrapper.eq(CmsPolicy::getIssueDept, query.getIssueDept().trim());
        }
        wrapper.orderByDesc(CmsPolicy::getPublishTime).orderByDesc(CmsPolicy::getId);

        Page<CmsPolicy> page = policyMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        List<PolicyVO> items = page.getRecords().stream().map(this::toVO).toList();
        return new PageResult<>(items, page.getTotal());
    }

    /**
     * 详情：累加浏览量，返回正文。未发布视为不存在，避免草稿被直接访问。
     */
    public PolicyDetailVO detail(Long id) {
        CmsPolicy policy = policyMapper.selectById(id);
        if (policy == null || !Integer.valueOf(PUBLISHED).equals(policy.getPublishStatus())) {
            throw new BusinessException("政策不存在或已下架");
        }
        policyMapper.update(null, Wrappers.<CmsPolicy>lambdaUpdate()
                .eq(CmsPolicy::getId, id)
                .setSql("view_count = view_count + 1"));

        int viewCount = (policy.getViewCount() == null ? 0 : policy.getViewCount()) + 1;
        return PolicyDetailVO.builder()
                .id(policy.getId())
                .title(policy.getTitle())
                .policyNo(policy.getPolicyNo())
                .issueDept(policy.getIssueDept())
                .cover(policy.getCover())
                .summary(policy.getSummary())
                .publishStatus(policy.getPublishStatus())
                .publishTime(policy.getPublishTime())
                .viewCount(viewCount)
                .authorId(policy.getAuthorId())
                .createTime(policy.getCreateTime())
                .content(policy.getContent())
                .build();
    }

    private PolicyVO toVO(CmsPolicy policy) {
        return PolicyVO.builder()
                .id(policy.getId())
                .title(policy.getTitle())
                .policyNo(policy.getPolicyNo())
                .issueDept(policy.getIssueDept())
                .cover(policy.getCover())
                .summary(policy.getSummary())
                .publishStatus(policy.getPublishStatus())
                .publishTime(policy.getPublishTime())
                .viewCount(policy.getViewCount())
                .authorId(policy.getAuthorId())
                .createTime(policy.getCreateTime())
                .build();
    }
}
