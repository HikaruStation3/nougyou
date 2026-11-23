package com.nougyou.entrust.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nougyou.common.api.ResultCode;
import com.nougyou.common.exception.BusinessException;
import com.nougyou.common.page.PageResult;
import com.nougyou.entrust.dto.EntrustAgentVO;
import com.nougyou.entrust.dto.EntrustConfirmForm;
import com.nougyou.entrust.dto.EntrustCreateForm;
import com.nougyou.entrust.dto.EntrustQuery;
import com.nougyou.entrust.dto.EntrustStatVO;
import com.nougyou.entrust.dto.EntrustVO;
import com.nougyou.entrust.dto.SettlementVO;
import com.nougyou.entrust.dto.row.SettlementSumRow;
import com.nougyou.entrust.entity.EntrustSale;
import com.nougyou.entrust.entity.EntrustSettlement;
import com.nougyou.entrust.mapper.EntrustReferenceMapper;
import com.nougyou.entrust.mapper.EntrustSaleMapper;
import com.nougyou.entrust.mapper.EntrustSettlementMapper;
import com.nougyou.product.entity.Product;
import com.nougyou.product.mapper.ProductMapper;
import com.nougyou.security.SecurityUtils;
import com.nougyou.system.entity.SysDept;
import com.nougyou.system.entity.SysRole;
import com.nougyou.system.entity.SysUser;
import com.nougyou.system.mapper.SysDeptMapper;
import com.nougyou.system.mapper.SysRoleMapper;
import com.nougyou.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * 委托销售服务（API-087 ~ API-095）。
 * 委托方与受托方查询同一份关系与统计视图，保证 FR-024 / FR-025 的数据双向一致。
 */
@Service
@RequiredArgsConstructor
public class EntrustService {

    private static final int STATUS_PENDING = 0;
    private static final int STATUS_ACTIVE = 1;
    private static final int STATUS_TERMINATED = 2;
    private static final int STATUS_REJECTED = 3;
    private static final int SETTLEMENT_SETTLED = 1;
    private static final int MAX_PAGE_SIZE = 100;

    private final EntrustSaleMapper entrustMapper;
    private final EntrustSettlementMapper settlementMapper;
    private final EntrustReferenceMapper referenceMapper;
    private final ProductMapper productMapper;
    private final SysUserMapper userMapper;
    private final SysDeptMapper deptMapper;
    private final SysRoleMapper roleMapper;

    /* ============================================================
     * API-087 建立委托关系
     * ============================================================ */

    @Transactional
    public EntrustVO create(Long currentUserId, EntrustCreateForm form) {
        Product product = productMapper.selectById(form.getProductId());
        if (product == null) {
            throw new BusinessException("被委托商品不存在或已删除");
        }
        if (!currentUserId.equals(product.getFarmerId())) {
            throw new BusinessException(ResultCode.PERMISSION_DENIED, "只能委托自己名下的商品");
        }
        if (currentUserId.equals(form.getAgentUserId())) {
            throw new BusinessException("不能委托给自己");
        }
        SysUser agent = userMapper.selectById(form.getAgentUserId());
        if (agent == null) {
            throw new BusinessException("受托方不存在");
        }
        Long farmerRoleId = farmerRoleId();
        if (farmerRoleId != null && !farmerRoleId.equals(agent.getRoleId())) {
            throw new BusinessException("受托方必须为已认证的农户商家");
        }
        if (form.getEndDate() != null && form.getEndDate().isBefore(form.getStartDate())) {
            throw new BusinessException("生效结束日期不能早于开始日期");
        }
        boolean duplicated = entrustMapper.exists(Wrappers.<EntrustSale>lambdaQuery()
                .eq(EntrustSale::getProductId, form.getProductId())
                .in(EntrustSale::getStatus, STATUS_PENDING, STATUS_ACTIVE));
        if (duplicated) {
            throw new BusinessException("该商品已有待确认或生效中的委托关系，请先终止后再委托");
        }

        EntrustSale entrust = new EntrustSale();
        entrust.setEntrustNo(nextEntrustNo());
        entrust.setEntrustUserId(currentUserId);
        entrust.setAgentUserId(form.getAgentUserId());
        entrust.setProductId(form.getProductId());
        entrust.setSplitRatio(form.getSplitRatio());
        entrust.setSettleCycle(form.getSettleCycle());
        entrust.setAgreementDesc(form.getAgreementDesc());
        entrust.setStartDate(form.getStartDate());
        entrust.setEndDate(form.getEndDate());
        entrust.setStatus(STATUS_PENDING);
        entrustMapper.insert(entrust);
        return assemble(List.of(entrustMapper.selectById(entrust.getId()))).get(0);
    }

    /* ============================================================
     * API-088 / API-089 / 后台列表
     * ============================================================ */

    /**
     * API-088 我委托出去的商品。
     */
    public PageResult<EntrustVO> sentPage(Long currentUserId, EntrustQuery query) {
        return page(Wrappers.<EntrustSale>lambdaQuery().eq(EntrustSale::getEntrustUserId, currentUserId), query);
    }

    /**
     * API-089 我代销的商品。
     */
    public PageResult<EntrustVO> receivedPage(Long currentUserId, EntrustQuery query) {
        return page(Wrappers.<EntrustSale>lambdaQuery().eq(EntrustSale::getAgentUserId, currentUserId), query);
    }

    /**
     * 后台委托关系列表：供「委托结算」页选择关系，接口文档未列，属后台查询扩展。
     */
    public PageResult<EntrustVO> adminPage(EntrustQuery query) {
        var wrapper = Wrappers.<EntrustSale>lambdaQuery();
        if (StringUtils.hasText(query.getKeyword())) {
            String keyword = query.getKeyword().trim();
            wrapper.and(w -> w.like(EntrustSale::getEntrustNo, keyword)
                    .or().apply("product_id IN (SELECT id FROM product WHERE name LIKE CONCAT('%', {0}, '%'))", keyword));
        }
        return page(wrapper, query);
    }

    private PageResult<EntrustVO> page(
            com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<EntrustSale> wrapper,
            EntrustQuery query) {
        if (query.getStatus() != null) {
            wrapper.eq(EntrustSale::getStatus, query.getStatus());
        }
        long pageNum = Math.max(query.getPageNum(), 1);
        long pageSize = Math.min(Math.max(query.getPageSize(), 1), MAX_PAGE_SIZE);
        wrapper.orderByDesc(EntrustSale::getCreateTime).orderByDesc(EntrustSale::getId);
        Page<EntrustSale> page = entrustMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return new PageResult<>(assemble(page.getRecords()), page.getTotal());
    }

    /* ============================================================
     * API-090 委托关系详情
     * ============================================================ */

    public EntrustVO detail(Long currentUserId, Long id) {
        EntrustSale entrust = requireEntrust(id);
        requirePartyOrAdmin(currentUserId, entrust);
        return assemble(List.of(entrust)).get(0);
    }

    /* ============================================================
     * API-091 受托方确认 / 拒绝
     * ============================================================ */

    @Transactional
    public EntrustVO confirm(Long currentUserId, Long id, EntrustConfirmForm form) {
        EntrustSale entrust = requireEntrust(id);
        if (!currentUserId.equals(entrust.getAgentUserId())) {
            throw new BusinessException(ResultCode.PERMISSION_DENIED, "只有受托方可以确认该委托");
        }
        if (!Integer.valueOf(STATUS_PENDING).equals(entrust.getStatus())) {
            throw new BusinessException("该委托关系已处理，无需重复确认");
        }
        if (Boolean.TRUE.equals(form.getAgree())) {
            entrust.setStatus(STATUS_ACTIVE);
            entrust.setConfirmTime(LocalDateTime.now());
            if (entrust.getStartDate() == null) {
                entrust.setStartDate(LocalDate.now());
            }
        } else {
            entrust.setStatus(STATUS_REJECTED);
            entrust.setAgreementDesc(appendDesc(entrust.getAgreementDesc(),
                    "[受托方拒绝] " + (StringUtils.hasText(form.getRemark()) ? form.getRemark().trim() : "未填写说明")));
        }
        entrustMapper.updateById(entrust);
        return assemble(List.of(entrust)).get(0);
    }

    /* ============================================================
     * API-092 终止委托关系
     * ============================================================ */

    @Transactional
    public EntrustVO terminate(Long currentUserId, Long id, String reason) {
        EntrustSale entrust = requireEntrust(id);
        requirePartyOrAdmin(currentUserId, entrust);
        if (Integer.valueOf(STATUS_TERMINATED).equals(entrust.getStatus())
                || Integer.valueOf(STATUS_REJECTED).equals(entrust.getStatus())) {
            throw new BusinessException("该委托关系已结束，无需重复终止");
        }
        entrust.setStatus(STATUS_TERMINATED);
        entrust.setEndDate(LocalDate.now());
        if (StringUtils.hasText(reason)) {
            entrust.setAgreementDesc(appendDesc(entrust.getAgreementDesc(), "[终止] " + reason.trim()));
        }
        entrustMapper.updateById(entrust);
        return assemble(List.of(entrust)).get(0);
    }

    /* ============================================================
     * API-093 委托销售数据统计
     * ============================================================ */

    public EntrustStatVO stat(Long currentUserId, Long id) {
        EntrustSale entrust = requireEntrust(id);
        requirePartyOrAdmin(currentUserId, entrust);
        EntrustStatVO stat = referenceMapper.findStat(id);
        if (stat == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "委托统计不存在");
        }
        return stat;
    }

    /* ============================================================
     * API-094 结算单列表
     * ============================================================ */

    public PageResult<SettlementVO> settlementPage(Long currentUserId, Long id, String settleMonth,
                                                   long pageNum, long pageSize) {
        EntrustSale entrust = requireEntrust(id);
        requirePartyOrAdmin(currentUserId, entrust);
        var wrapper = Wrappers.<EntrustSettlement>lambdaQuery().eq(EntrustSettlement::getEntrustId, id);
        if (StringUtils.hasText(settleMonth)) {
            wrapper.eq(EntrustSettlement::getSettleMonth, settleMonth.trim());
        }
        wrapper.orderByDesc(EntrustSettlement::getSettleMonth).orderByDesc(EntrustSettlement::getId);
        Page<EntrustSettlement> page = settlementMapper.selectPage(
                new Page<>(Math.max(pageNum, 1), Math.min(Math.max(pageSize, 1), MAX_PAGE_SIZE)), wrapper);
        String productName = productName(entrust.getProductId());
        List<SettlementVO> list = page.getRecords().stream()
                .map(item -> toSettlementVO(item, entrust, productName))
                .toList();
        return new PageResult<>(list, page.getTotal());
    }

    /* ============================================================
     * API-095 生成月度结算单
     * ============================================================ */

    /**
     * 按分成比例生成月度结算单；同一关系同一月份只允许生成一次。
     * 生成即视为平台已结算，后续如需打款流程可再把 status 置回 0。
     */
    @Transactional
    public SettlementVO settle(Long id, String settleMonth) {
        EntrustSale entrust = requireEntrust(id);
        boolean exists = settlementMapper.exists(Wrappers.<EntrustSettlement>lambdaQuery()
                .eq(EntrustSettlement::getEntrustId, id)
                .eq(EntrustSettlement::getSettleMonth, settleMonth));
        if (exists) {
            throw new BusinessException("该委托关系 " + settleMonth + " 的结算单已生成，请勿重复结算");
        }

        SettlementSumRow sale = referenceMapper.sumMonthSale(id, settleMonth);
        SettlementSumRow refund = referenceMapper.sumMonthRefund(id, settleMonth);
        int saleQty = sale == null || sale.getQty() == null ? 0 : sale.getQty();
        BigDecimal saleAmount = sale == null || sale.getAmount() == null ? BigDecimal.ZERO : sale.getAmount();
        int refundQty = refund == null || refund.getQty() == null ? 0 : refund.getQty();
        BigDecimal refundAmount = refund == null || refund.getAmount() == null ? BigDecimal.ZERO : refund.getAmount();

        BigDecimal net = saleAmount.subtract(refundAmount);
        BigDecimal ratio = entrust.getSplitRatio() == null ? BigDecimal.ZERO : entrust.getSplitRatio();
        BigDecimal agentIncome = net.multiply(ratio)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal entrustIncome = net.subtract(agentIncome);

        EntrustSettlement settlement = new EntrustSettlement();
        settlement.setEntrustId(id);
        settlement.setSettleMonth(settleMonth);
        settlement.setSaleQty(saleQty);
        settlement.setSaleAmount(saleAmount);
        settlement.setRefundQty(refundQty);
        settlement.setRefundAmount(refundAmount);
        settlement.setEntrustIncome(entrustIncome);
        settlement.setAgentIncome(agentIncome);
        settlement.setStatus(SETTLEMENT_SETTLED);
        settlement.setSettleTime(LocalDateTime.now());
        settlementMapper.insert(settlement);
        return toSettlementVO(settlementMapper.selectById(settlement.getId()), entrust,
                productName(entrust.getProductId()));
    }

    /* ============================================================
     * 可选受托方
     * ============================================================ */

    /**
     * 建立委托关系时可选的本村农户商家（不含自己）。
     */
    public List<EntrustAgentVO> agents(Long currentUserId) {
        Long farmerRoleId = farmerRoleId();
        if (farmerRoleId == null) {
            return List.of();
        }
        List<SysUser> farmers = userMapper.selectList(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getRoleId, farmerRoleId)
                .eq(SysUser::getStatus, 1)
                .ne(SysUser::getId, currentUserId)
                .orderByAsc(SysUser::getId));
        if (farmers.isEmpty()) {
            return List.of();
        }
        List<Long> deptIds = farmers.stream().map(SysUser::getDeptId).filter(Objects::nonNull).distinct().toList();
        Map<Long, String> shopNames = deptIds.isEmpty() ? Map.of()
                : deptMapper.selectBatchIds(deptIds).stream()
                .collect(Collectors.toMap(SysDept::getId, SysDept::getName, (a, b) -> a));
        return farmers.stream().map(user -> EntrustAgentVO.builder()
                .id(String.valueOf(user.getId()))
                .name(displayName(user))
                .shopName(user.getDeptId() == null ? null : shopNames.get(user.getDeptId()))
                .build()).toList();
    }

    /* ============================================================
     * 内部方法
     * ============================================================ */

    private EntrustSale requireEntrust(Long id) {
        EntrustSale entrust = entrustMapper.selectById(id);
        if (entrust == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "委托关系不存在");
        }
        return entrust;
    }

    /**
     * 委托双方可操作；管理员（拥有 entrust:settle）可查看与终止。
     */
    private void requirePartyOrAdmin(Long currentUserId, EntrustSale entrust) {
        boolean party = currentUserId.equals(entrust.getEntrustUserId())
                || currentUserId.equals(entrust.getAgentUserId());
        if (!party && !SecurityUtils.hasAuthority("entrust:settle")) {
            throw new BusinessException(ResultCode.PERMISSION_DENIED, "无权访问该委托关系");
        }
    }

    private List<EntrustVO> assemble(List<EntrustSale> entrusts) {
        if (entrusts == null || entrusts.isEmpty()) {
            return List.of();
        }
        List<Long> productIds = entrusts.stream().map(EntrustSale::getProductId)
                .filter(Objects::nonNull).distinct().toList();
        Map<Long, Product> products = productIds.isEmpty() ? Map.of()
                : productMapper.selectBatchIds(productIds).stream()
                .collect(Collectors.toMap(Product::getId, product -> product, (a, b) -> a));
        List<Long> userIds = new ArrayList<>();
        entrusts.forEach(entrust -> {
            userIds.add(entrust.getEntrustUserId());
            userIds.add(entrust.getAgentUserId());
        });
        Map<Long, SysUser> users = userMapper.selectBatchIds(userIds.stream()
                        .filter(Objects::nonNull).distinct().toList()).stream()
                .collect(Collectors.toMap(SysUser::getId, user -> user, (a, b) -> a));

        return entrusts.stream().map(entrust -> {
            Product product = products.get(entrust.getProductId());
            return EntrustVO.builder()
                    .id(String.valueOf(entrust.getId()))
                    .entrustNo(entrust.getEntrustNo())
                    .entrustUserId(String.valueOf(entrust.getEntrustUserId()))
                    .entrustUserName(displayName(users.get(entrust.getEntrustUserId())))
                    .agentUserId(String.valueOf(entrust.getAgentUserId()))
                    .agentUserName(displayName(users.get(entrust.getAgentUserId())))
                    .productId(String.valueOf(entrust.getProductId()))
                    .productName(product == null ? null : product.getName())
                    .productCover(product == null ? null : product.getCover())
                    .splitRatio(entrust.getSplitRatio())
                    .settleCycle(entrust.getSettleCycle())
                    .agreementDesc(entrust.getAgreementDesc())
                    .startDate(entrust.getStartDate())
                    .endDate(entrust.getEndDate())
                    .status(entrust.getStatus())
                    .confirmTime(entrust.getConfirmTime())
                    .createTime(entrust.getCreateTime())
                    .build();
        }).toList();
    }

    private SettlementVO toSettlementVO(EntrustSettlement settlement, EntrustSale entrust, String productName) {
        return SettlementVO.builder()
                .id(String.valueOf(settlement.getId()))
                .entrustId(String.valueOf(settlement.getEntrustId()))
                .entrustNo(entrust.getEntrustNo())
                .productName(productName)
                .settleMonth(settlement.getSettleMonth())
                .saleQty(settlement.getSaleQty())
                .saleAmount(settlement.getSaleAmount())
                .refundQty(settlement.getRefundQty())
                .refundAmount(settlement.getRefundAmount())
                .entrustIncome(settlement.getEntrustIncome())
                .agentIncome(settlement.getAgentIncome())
                .status(settlement.getStatus())
                .settleTime(settlement.getSettleTime())
                .build();
    }

    private String productName(Long productId) {
        Product product = productMapper.selectById(productId);
        return product == null ? null : product.getName();
    }

    private String displayName(SysUser user) {
        if (user == null) {
            return null;
        }
        return StringUtils.hasText(user.getNickname()) ? user.getNickname() : user.getUsername();
    }

    private Long farmerRoleId() {
        SysRole role = roleMapper.selectOne(Wrappers.<SysRole>lambdaQuery().eq(SysRole::getCode, "FARMER").last("limit 1"));
        return role == null ? null : role.getId();
    }

    /**
     * 追加说明并限制在字段长度内，用于记录拒绝 / 终止原因。
     */
    private String appendDesc(String origin, String suffix) {
        String merged = StringUtils.hasText(origin) ? origin + "；" + suffix : suffix;
        return merged.length() > 500 ? merged.substring(0, 500) : merged;
    }

    /**
     * 委托编号沿用种子数据风格：WT + yyyyMMdd + 4 位随机数。
     */
    private String nextEntrustNo() {
        String date = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        for (int i = 0; i < 8; i++) {
            String no = "WT" + date + String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
            if (!entrustMapper.exists(Wrappers.<EntrustSale>lambdaQuery().eq(EntrustSale::getEntrustNo, no))) {
                return no;
            }
        }
        throw new BusinessException("委托编号生成失败，请重试");
    }
}
