package com.nougyou.cms.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nougyou.cms.dto.PolicyDetailVO;
import com.nougyou.cms.dto.PolicyForm;
import com.nougyou.cms.dto.PolicyPageQuery;
import com.nougyou.cms.dto.PolicyVO;
import com.nougyou.cms.entity.CmsPolicy;
import com.nougyou.cms.mapper.CmsPolicyMapper;
import com.nougyou.common.exception.BusinessException;
import com.nougyou.common.page.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 扶贫政策：前台公开浏览（API-021 列表 / API-022 详情）+ 后台维护（API-023~026）。
 * 只暴露 publish_status = 1 的记录，草稿与已下架内容前台一律不可见。
 */
@Service
@RequiredArgsConstructor
public class PolicyService {

    private static final int DRAFT = 0;
    private static final int PUBLISHED = 1;
    private static final int OFFLINE = 2;
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

    // ==================== 管理端（API-023~026） ====================

    /** 管理端分页：包含草稿与已下架，支持标题/文号、发布单位与状态筛选 */
    public PageResult<PolicyVO> adminPage(PolicyPageQuery query) {
        long pageNum = Math.max(1, query.getPageNum());
        long pageSize = Math.min(Math.max(1, query.getPageSize()), MAX_PAGE_SIZE);

        var wrapper = Wrappers.<CmsPolicy>lambdaQuery();
        if (StringUtils.hasText(query.getKeyword())) {
            String keyword = query.getKeyword().trim();
            wrapper.and(w -> w.like(CmsPolicy::getTitle, keyword)
                    .or().like(CmsPolicy::getPolicyNo, keyword));
        }
        if (StringUtils.hasText(query.getIssueDept())) {
            wrapper.eq(CmsPolicy::getIssueDept, query.getIssueDept().trim());
        }
        if (query.getPublishStatus() != null) {
            wrapper.eq(CmsPolicy::getPublishStatus, query.getPublishStatus());
        }
        wrapper.orderByDesc(CmsPolicy::getCreateTime).orderByDesc(CmsPolicy::getId);

        Page<CmsPolicy> page = policyMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        List<PolicyVO> items = page.getRecords().stream().map(this::toVO).toList();
        return new PageResult<>(items, page.getTotal());
    }

    /** 编辑回显 */
    public PolicyForm form(Long id) {
        CmsPolicy policy = require(id);
        PolicyForm form = new PolicyForm();
        form.setId(policy.getId());
        form.setTitle(policy.getTitle());
        form.setPolicyNo(policy.getPolicyNo());
        form.setIssueDept(policy.getIssueDept());
        form.setCover(policy.getCover());
        form.setSummary(policy.getSummary());
        form.setContent(policy.getContent());
        return form;
    }

    /** 新增：一律先落草稿，确认无误后再由 API-026 发布 */
    @Transactional
    public Long create(PolicyForm form, Long authorId) {
        CmsPolicy policy = new CmsPolicy();
        applyForm(policy, form);
        policy.setPublishStatus(DRAFT);
        policy.setViewCount(0);
        policy.setAuthorId(authorId);
        policyMapper.insert(policy);
        return policy.getId();
    }

    @Transactional
    public void update(Long id, PolicyForm form) {
        CmsPolicy policy = require(id);
        applyForm(policy, form);
        policyMapper.updateById(policy);
    }

    /** 批量删除，多个 ID 用英文逗号分隔；逻辑删除，前台立刻查不到 */
    @Transactional
    public void delete(String ids) {
        List<Long> idList = parseIds(ids);
        if (idList.isEmpty()) {
            throw new BusinessException("请选择要删除的政策");
        }
        policyMapper.deleteByIds(idList);
    }

    /** 发布 / 下架：status=1 发布（已下架可重新上架），status=2 下架 */
    @Transactional
    public void publish(Long id, Integer status, Long operatorId) {
        CmsPolicy policy = require(id);
        int target = status == null ? PUBLISHED : status;
        if (target != PUBLISHED && target != OFFLINE) {
            throw new BusinessException("发布状态只能是 1（发布）或 2（下架）");
        }
        if (target == OFFLINE && !Integer.valueOf(PUBLISHED).equals(policy.getPublishStatus())) {
            throw new BusinessException("只有已发布的政策可以下架");
        }
        if (target == PUBLISHED && Integer.valueOf(PUBLISHED).equals(policy.getPublishStatus())) {
            throw new BusinessException("该政策已发布");
        }

        var update = Wrappers.<CmsPolicy>lambdaUpdate()
                .eq(CmsPolicy::getId, id)
                .set(CmsPolicy::getPublishStatus, target);
        if (target == PUBLISHED) {
            // 首次发布记录发布时间；再次上架保留原发布时间，避免前台排序跳动
            if (policy.getPublishTime() == null) {
                update.set(CmsPolicy::getPublishTime, LocalDateTime.now());
            }
            if (policy.getAuthorId() == null) {
                update.set(CmsPolicy::getAuthorId, operatorId);
            }
        }
        policyMapper.update(null, update);
    }

    private void applyForm(CmsPolicy policy, PolicyForm form) {
        policy.setTitle(form.getTitle().trim());
        policy.setPolicyNo(trimToNull(form.getPolicyNo()));
        policy.setIssueDept(trimToNull(form.getIssueDept()));
        policy.setCover(trimToNull(form.getCover()));
        policy.setSummary(trimToNull(form.getSummary()));
        policy.setContent(form.getContent());
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private CmsPolicy require(Long id) {
        CmsPolicy policy = policyMapper.selectById(id);
        if (policy == null) {
            throw new BusinessException("政策不存在");
        }
        return policy;
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
                throw new BusinessException("政策ID格式不正确：" + value);
            }
        }
        return result;
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
