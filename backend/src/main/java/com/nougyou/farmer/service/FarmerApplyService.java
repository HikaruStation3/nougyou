package com.nougyou.farmer.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nougyou.common.exception.BusinessException;
import com.nougyou.common.page.PageResult;
import com.nougyou.farmer.dto.FarmerApplyAuditForm;
import com.nougyou.farmer.dto.FarmerApplyForm;
import com.nougyou.farmer.dto.FarmerApplyQuery;
import com.nougyou.farmer.dto.FarmerApplyVO;
import com.nougyou.farmer.entity.FarmerApply;
import com.nougyou.farmer.mapper.FarmerApplyMapper;
import com.nougyou.system.entity.SysRole;
import com.nougyou.system.entity.SysUser;
import com.nougyou.system.mapper.SysRoleMapper;
import com.nougyou.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FarmerApplyService {

    private static final int STATUS_PENDING = 0;
    private static final int STATUS_APPROVED = 1;
    private static final int STATUS_REJECTED = 2;
    private static final String ROLE_FARMER = "FARMER";
    private static final DateTimeFormatter APPLY_NO_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final FarmerApplyMapper applyMapper;
    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;

    /**
     * 买家提交入驻申请，已有待审核申请或已是农户商家时不允许重复提交。
     */
    @Transactional
    public FarmerApplyVO submit(Long userId, FarmerApplyForm form) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (isFarmer(user)) {
            throw new BusinessException("您已是农户商家，无需重复申请");
        }
        boolean pending = applyMapper.exists(Wrappers.<FarmerApply>lambdaQuery()
                .eq(FarmerApply::getUserId, userId)
                .eq(FarmerApply::getAuditStatus, STATUS_PENDING));
        if (pending) {
            throw new BusinessException("您已有待审核的入驻申请，请耐心等待审核结果");
        }

        FarmerApply apply = new FarmerApply();
        apply.setApplyNo(generateApplyNo());
        apply.setUserId(userId);
        apply.setRealName(form.getRealName().trim());
        apply.setIdCard(form.getIdCard().trim());
        apply.setPhone(form.getPhone().trim());
        apply.setVillage(form.getVillage().trim());
        apply.setPovertyCardNo(form.getPovertyCardNo());
        apply.setPovertyLevel(form.getPovertyLevel());
        apply.setHouseholdNum(form.getHouseholdNum());
        apply.setMainProduct(form.getMainProduct());
        apply.setIdCardFront(form.getIdCardFront());
        apply.setIdCardBack(form.getIdCardBack());
        apply.setPovertyProof(form.getPovertyProof());
        apply.setApplyDesc(form.getApplyDesc());
        apply.setAuditStatus(STATUS_PENDING);
        applyMapper.insert(apply);
        return toVO(apply, user.getUsername(), null);
    }

    /**
     * 当前用户查看自己提交过的申请与审核进度。
     */
    public List<FarmerApplyVO> my(Long userId) {
        SysUser user = userMapper.selectById(userId);
        String username = user == null ? null : user.getUsername();
        Map<Long, String> auditNames = auditUserNames(userMapper.selectList(null));

        return applyMapper.selectList(Wrappers.<FarmerApply>lambdaQuery()
                        .eq(FarmerApply::getUserId, userId)
                        .orderByDesc(FarmerApply::getId))
                .stream()
                .map(apply -> toVO(apply, username, auditNames.get(apply.getAuditUserId())))
                .toList();
    }

    /**
     * 管理员按审核状态分页查询入驻申请。
     */
    public PageResult<FarmerApplyVO> page(FarmerApplyQuery query) {
        var wrapper = Wrappers.<FarmerApply>lambdaQuery();
        if (query.getAuditStatus() != null) {
            wrapper.eq(FarmerApply::getAuditStatus, query.getAuditStatus());
        }
        if (StringUtils.hasText(query.getKeywords())) {
            String keywords = query.getKeywords().trim();
            wrapper.and(w -> w.like(FarmerApply::getRealName, keywords)
                    .or().like(FarmerApply::getPhone, keywords)
                    .or().like(FarmerApply::getApplyNo, keywords)
                    .or().like(FarmerApply::getVillage, keywords));
        }
        wrapper.orderByAsc(FarmerApply::getAuditStatus).orderByDesc(FarmerApply::getId);

        Page<FarmerApply> page = applyMapper.selectPage(new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
        List<Long> userIds = page.getRecords().stream()
                .flatMap(apply -> java.util.stream.Stream.of(apply.getUserId(), apply.getAuditUserId()))
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, String> usernames = userIds.isEmpty() ? Map.of() : userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(SysUser::getId, SysUser::getUsername, (a, b) -> a));

        List<FarmerApplyVO> items = page.getRecords().stream()
                .map(apply -> toVO(apply, usernames.get(apply.getUserId()), usernames.get(apply.getAuditUserId())))
                .toList();
        return new PageResult<>(items, page.getTotal());
    }

    public FarmerApplyVO detail(Long id) {
        FarmerApply apply = requireApply(id);
        SysUser applicant = userMapper.selectById(apply.getUserId());
        SysUser auditor = apply.getAuditUserId() == null ? null : userMapper.selectById(apply.getAuditUserId());
        return toVO(apply, applicant == null ? null : applicant.getUsername(),
                auditor == null ? null : auditor.getUsername());
    }

    /**
     * 审核入驻申请：通过后在同一事务内将用户角色升级为农户商家。
     */
    @Transactional
    public FarmerApplyVO audit(Long id, Long auditUserId, FarmerApplyAuditForm form) {
        FarmerApply apply = requireApply(id);
        if (!Integer.valueOf(STATUS_PENDING).equals(apply.getAuditStatus())) {
            throw new BusinessException("该申请已审核，不能重复操作");
        }
        boolean approved = Boolean.TRUE.equals(form.getApproved());
        if (approved) {
            SysUser applicant = userMapper.selectById(apply.getUserId());
            if (applicant == null) {
                throw new BusinessException("申请用户不存在");
            }
            SysRole farmerRole = roleMapper.selectOne(Wrappers.<SysRole>lambdaQuery()
                    .eq(SysRole::getCode, ROLE_FARMER));
            if (farmerRole == null) {
                throw new BusinessException("农户商家角色不存在");
            }
            applicant.setRoleId(farmerRole.getId());
            userMapper.updateById(applicant);
        }

        apply.setAuditStatus(approved ? STATUS_APPROVED : STATUS_REJECTED);
        apply.setAuditUserId(auditUserId);
        apply.setAuditTime(LocalDateTime.now());
        apply.setAuditRemark(form.getRemark());
        applyMapper.updateById(apply);

        SysUser applicant = userMapper.selectById(apply.getUserId());
        SysUser auditor = userMapper.selectById(auditUserId);
        return toVO(apply, applicant == null ? null : applicant.getUsername(),
                auditor == null ? null : auditor.getUsername());
    }

    private FarmerApply requireApply(Long id) {
        FarmerApply apply = applyMapper.selectById(id);
        if (apply == null) {
            throw new BusinessException("入驻申请不存在");
        }
        return apply;
    }

    private boolean isFarmer(SysUser user) {
        if (user.getRoleId() == null) {
            return false;
        }
        SysRole role = roleMapper.selectById(user.getRoleId());
        return role != null && ROLE_FARMER.equals(role.getCode());
    }

    private String generateApplyNo() {
        return "SQ" + LocalDateTime.now().format(APPLY_NO_FORMAT)
                + String.format("%03d", ThreadLocalRandom.current().nextInt(1000));
    }

    private Map<Long, String> auditUserNames(List<SysUser> users) {
        return users.stream().collect(Collectors.toMap(SysUser::getId, SysUser::getUsername, (a, b) -> a));
    }

    private FarmerApplyVO toVO(FarmerApply apply, String username, String auditUsername) {
        return FarmerApplyVO.builder()
                .id(String.valueOf(apply.getId()))
                .applyNo(apply.getApplyNo())
                .userId(String.valueOf(apply.getUserId()))
                .username(username)
                .realName(apply.getRealName())
                .idCard(apply.getIdCard())
                .phone(apply.getPhone())
                .village(apply.getVillage())
                .povertyCardNo(apply.getPovertyCardNo())
                .povertyLevel(apply.getPovertyLevel())
                .householdNum(apply.getHouseholdNum())
                .mainProduct(apply.getMainProduct())
                .idCardFront(apply.getIdCardFront())
                .idCardBack(apply.getIdCardBack())
                .povertyProof(apply.getPovertyProof())
                .applyDesc(apply.getApplyDesc())
                .auditStatus(apply.getAuditStatus())
                .auditStatusText(statusText(apply.getAuditStatus()))
                .auditUserId(apply.getAuditUserId() == null ? null : String.valueOf(apply.getAuditUserId()))
                .auditUsername(auditUsername)
                .auditTime(apply.getAuditTime())
                .auditRemark(apply.getAuditRemark())
                .createTime(apply.getCreateTime())
                .build();
    }

    private String statusText(Integer status) {
        if (status == null) {
            return "";
        }
        return switch (status) {
            case STATUS_PENDING -> "待审核";
            case STATUS_APPROVED -> "已通过";
            case STATUS_REJECTED -> "已驳回";
            default -> "未知";
        };
    }
}
