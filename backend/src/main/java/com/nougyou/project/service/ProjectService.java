package com.nougyou.project.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nougyou.common.exception.BusinessException;
import com.nougyou.common.page.PageResult;
import com.nougyou.project.dto.ProjectAuditForm;
import com.nougyou.project.dto.ProjectForm;
import com.nougyou.project.dto.ProjectPageQuery;
import com.nougyou.project.dto.ProjectShelfForm;
import com.nougyou.project.dto.ProjectVO;
import com.nougyou.project.entity.FarmProject;
import com.nougyou.project.mapper.FarmProjectMapper;
import com.nougyou.system.entity.SysDept;
import com.nougyou.system.entity.SysUser;
import com.nougyou.system.mapper.SysDeptMapper;
import com.nougyou.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private static final String PROJECT_TYPE = "农家乐";
    private static final int AUDIT_PENDING = 0;
    private static final int AUDIT_APPROVED = 1;
    private static final int AUDIT_REJECTED = 2;
    private static final int SHELF_OFF = 0;
    private static final int SHELF_ON = 1;

    private final FarmProjectMapper projectMapper;
    private final SysUserMapper userMapper;
    private final SysDeptMapper deptMapper;

    /**
     * API-033 公开列表：仅展示审核通过且已上架的农家乐项目。
     */
    public PageResult<ProjectVO> page(ProjectPageQuery query) {
        var wrapper = Wrappers.<FarmProject>lambdaQuery()
                .eq(FarmProject::getProjectType, StringUtils.hasText(query.getProjectType()) ? query.getProjectType().trim() : PROJECT_TYPE)
                .eq(FarmProject::getAuditStatus, AUDIT_APPROVED)
                .eq(FarmProject::getShelfStatus, SHELF_ON);
        if (StringUtils.hasText(query.getAddress())) {
            wrapper.like(FarmProject::getAddress, query.getAddress().trim());
        }
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.like(FarmProject::getTitle, query.getKeyword().trim());
        }
        wrapper.orderByDesc(FarmProject::getViewCount).orderByDesc(FarmProject::getId);

        Page<FarmProject> page = projectMapper.selectPage(new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
        return toPageResult(page);
    }

    /**
     * API-034 公开详情：仅审核通过且已上架的项目可查看，并累加浏览量。
     */
    @Transactional
    public ProjectVO detail(Long id) {
        FarmProject project = projectMapper.selectById(id);
        if (project == null || !Integer.valueOf(AUDIT_APPROVED).equals(project.getAuditStatus())
                || !Integer.valueOf(SHELF_ON).equals(project.getShelfStatus())) {
            throw new BusinessException("项目不存在或未上架");
        }
        project.setViewCount((project.getViewCount() == null ? 0 : project.getViewCount()) + 1);
        projectMapper.updateById(project);
        return toVO(project, farmerInfo(List.of(project.getFarmerId())));
    }

    /**
     * API-035 农户商家申请农家乐项目，提交后进入待审核。
     */
    @Transactional
    public ProjectVO create(Long farmerId, ProjectForm form) {
        requireNotBlank(form.getTitle(), "项目名称不能为空");
        requireNotBlank(form.getCover(), "封面图不能为空");
        requireNotBlank(form.getAddress(), "项目地址不能为空");
        requireNotBlank(form.getContactPhone(), "联系电话不能为空");

        SysUser farmer = userMapper.selectById(farmerId);
        if (farmer == null) {
            throw new BusinessException("商家用户不存在");
        }

        FarmProject project = new FarmProject();
        project.setFarmerId(farmerId);
        project.setTitle(form.getTitle().trim());
        project.setProjectType(StringUtils.hasText(form.getProjectType()) ? form.getProjectType().trim() : PROJECT_TYPE);
        project.setCover(form.getCover().trim());
        project.setImages(joinImages(form.getImages()));
        project.setAddress(form.getAddress().trim());
        project.setIntro(form.getIntro());
        project.setPriceDesc(form.getPriceDesc());
        project.setCapacity(form.getCapacity());
        project.setContactPhone(form.getContactPhone().trim());
        project.setBusinessHours(form.getBusinessHours());
        project.setAuditStatus(AUDIT_PENDING);
        project.setShelfStatus(SHELF_ON);
        project.setViewCount(0);
        projectMapper.insert(project);
        return toVO(project, farmerInfo(List.of(farmerId)));
    }

    /**
     * API-036 商家查看自己提交的项目及审核状态。
     */
    public PageResult<ProjectVO> myPage(Long farmerId, ProjectPageQuery query) {
        var wrapper = Wrappers.<FarmProject>lambdaQuery().eq(FarmProject::getFarmerId, farmerId);
        if (query.getAuditStatus() != null) {
            wrapper.eq(FarmProject::getAuditStatus, query.getAuditStatus());
        }
        wrapper.orderByDesc(FarmProject::getId);
        Page<FarmProject> page = projectMapper.selectPage(new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
        return toPageResult(page);
    }

    /**
     * API-037 编辑项目信息；resubmit 为 true 时重置为待审核，供驳回后重新提交。
     */
    @Transactional
    public ProjectVO update(Long farmerId, Long id, ProjectForm form) {
        FarmProject project = requireOwned(farmerId, id);
        if (StringUtils.hasText(form.getTitle())) {
            project.setTitle(form.getTitle().trim());
        }
        if (form.getProjectType() != null) {
            project.setProjectType(StringUtils.hasText(form.getProjectType()) ? form.getProjectType().trim() : PROJECT_TYPE);
        }
        if (form.getCover() != null) {
            project.setCover(form.getCover());
        }
        if (form.getImages() != null) {
            project.setImages(joinImages(form.getImages()));
        }
        if (form.getAddress() != null) {
            project.setAddress(form.getAddress());
        }
        if (form.getIntro() != null) {
            project.setIntro(form.getIntro());
        }
        if (form.getPriceDesc() != null) {
            project.setPriceDesc(form.getPriceDesc());
        }
        if (form.getCapacity() != null) {
            project.setCapacity(form.getCapacity());
        }
        if (form.getContactPhone() != null) {
            project.setContactPhone(form.getContactPhone());
        }
        if (form.getBusinessHours() != null) {
            project.setBusinessHours(form.getBusinessHours());
        }
        if (Boolean.TRUE.equals(form.getResubmit())) {
            project.setAuditStatus(AUDIT_PENDING);
            project.setAuditUserId(null);
            project.setAuditTime(null);
            project.setAuditRemark(null);
        }
        projectMapper.updateById(project);
        return toVO(project, farmerInfo(List.of(farmerId)));
    }

    /**
     * API-038 商家自行上下架，仅审核通过的项目允许操作。
     */
    @Transactional
    public ProjectVO shelf(Long farmerId, Long id, ProjectShelfForm form) {
        Integer target = form.getShelfStatus();
        if (target == null || (target != SHELF_OFF && target != SHELF_ON)) {
            throw new BusinessException("目标状态非法，仅支持 0 下架 / 1 上架");
        }
        FarmProject project = requireOwned(farmerId, id);
        if (!Integer.valueOf(AUDIT_APPROVED).equals(project.getAuditStatus())) {
            throw new BusinessException("仅审核通过的项目可上下架");
        }
        project.setShelfStatus(target);
        projectMapper.updateById(project);
        return toVO(project, farmerInfo(List.of(farmerId)));
    }

    /**
     * 管理员审核队列：查看全部项目（API-039 的配套列表，便于审核操作）。
     */
    public PageResult<ProjectVO> adminPage(ProjectPageQuery query) {
        var wrapper = Wrappers.<FarmProject>lambdaQuery();
        if (query.getAuditStatus() != null) {
            wrapper.eq(FarmProject::getAuditStatus, query.getAuditStatus());
        }
        if (StringUtils.hasText(query.getKeyword())) {
            String keyword = query.getKeyword().trim();
            wrapper.and(w -> w.like(FarmProject::getTitle, keyword).or().like(FarmProject::getAddress, keyword));
        }
        if (StringUtils.hasText(query.getAddress())) {
            wrapper.like(FarmProject::getAddress, query.getAddress().trim());
        }
        wrapper.orderByAsc(FarmProject::getAuditStatus).orderByDesc(FarmProject::getId);
        Page<FarmProject> page = projectMapper.selectPage(new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
        return toPageResult(page);
    }

    /**
     * API-039 管理员审核农家乐项目，通过后才会在前台展示。
     */
    @Transactional
    public ProjectVO audit(Long id, Long adminUserId, ProjectAuditForm form) {
        Integer status = form.getAuditStatus();
        if (status == null || (status != AUDIT_APPROVED && status != AUDIT_REJECTED)) {
            throw new BusinessException("审核结果非法，仅支持 1 通过 / 2 驳回");
        }
        if (status == AUDIT_REJECTED && !StringUtils.hasText(form.getAuditRemark())) {
            throw new BusinessException("驳回时审核意见不能为空");
        }
        FarmProject project = requireProject(id);
        project.setAuditStatus(status);
        project.setAuditUserId(adminUserId);
        project.setAuditTime(LocalDateTime.now());
        project.setAuditRemark(form.getAuditRemark());
        projectMapper.updateById(project);
        return toVO(project, farmerInfo(List.of(project.getFarmerId())));
    }

    private FarmProject requireProject(Long id) {
        FarmProject project = projectMapper.selectById(id);
        if (project == null) {
            throw new BusinessException("项目不存在");
        }
        return project;
    }

    private FarmProject requireOwned(Long farmerId, Long id) {
        FarmProject project = requireProject(id);
        if (!farmerId.equals(project.getFarmerId())) {
            throw new BusinessException("无权操作他人的项目");
        }
        return project;
    }

    private PageResult<ProjectVO> toPageResult(Page<FarmProject> page) {
        Map<Long, FarmerInfo> infoMap = farmerInfo(
                page.getRecords().stream().map(FarmProject::getFarmerId).filter(Objects::nonNull).distinct().toList());
        List<ProjectVO> items = page.getRecords().stream()
                .map(p -> toVO(p, infoMap))
                .toList();
        return new PageResult<>(items, page.getTotal());
    }

    private record FarmerInfo(String farmerName, String shopName) {
    }

    private Map<Long, FarmerInfo> farmerInfo(List<Long> farmerIds) {
        if (farmerIds == null || farmerIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, SysUser> users = userMapper.selectBatchIds(farmerIds).stream()
                .collect(Collectors.toMap(SysUser::getId, u -> u, (a, b) -> a));
        List<Long> deptIds = users.values().stream()
                .map(SysUser::getDeptId).filter(Objects::nonNull).distinct().toList();
        Map<Long, String> deptNames = deptIds.isEmpty() ? Map.of() : deptMapper.selectBatchIds(deptIds).stream()
                .collect(Collectors.toMap(SysDept::getId, SysDept::getName, (a, b) -> a));
        return users.entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> {
                    SysUser u = e.getValue();
                    String name = StringUtils.hasText(u.getNickname()) ? u.getNickname() : u.getUsername();
                    String shop = u.getDeptId() == null ? null : deptNames.get(u.getDeptId());
                    return new FarmerInfo(name, shop);
                }));
    }

    private ProjectVO toVO(FarmProject project, Map<Long, FarmerInfo> infoMap) {
        FarmerInfo info = infoMap.getOrDefault(project.getFarmerId(), new FarmerInfo(null, null));
        return ProjectVO.builder()
                .id(String.valueOf(project.getId()))
                .farmerId(project.getFarmerId() == null ? null : String.valueOf(project.getFarmerId()))
                .farmerName(info.farmerName())
                .shopName(info.shopName())
                .title(project.getTitle())
                .projectType(project.getProjectType())
                .cover(project.getCover())
                .images(splitImages(project.getImages()))
                .address(project.getAddress())
                .intro(project.getIntro())
                .priceDesc(project.getPriceDesc())
                .capacity(project.getCapacity())
                .contactPhone(project.getContactPhone())
                .businessHours(project.getBusinessHours())
                .auditStatus(project.getAuditStatus())
                .auditRemark(project.getAuditRemark())
                .shelfStatus(project.getShelfStatus())
                .viewCount(project.getViewCount())
                .createTime(project.getCreateTime())
                .build();
    }

    private List<String> splitImages(String images) {
        if (!StringUtils.hasText(images)) {
            return List.of();
        }
        return Arrays.stream(images.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
    }

    private String joinImages(List<String> images) {
        if (images == null || images.isEmpty()) {
            return null;
        }
        String joined = images.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .collect(Collectors.joining(","));
        return joined.isEmpty() ? null : joined;
    }

    private void requireNotBlank(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(message);
        }
    }
}
