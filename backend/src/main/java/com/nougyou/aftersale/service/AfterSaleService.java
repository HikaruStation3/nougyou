package com.nougyou.aftersale.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nougyou.aftersale.dto.AfterSaleCreateForm;
import com.nougyou.aftersale.dto.AfterSaleHandleForm;
import com.nougyou.aftersale.dto.AfterSalePageQuery;
import com.nougyou.aftersale.dto.AfterSaleVO;
import com.nougyou.aftersale.entity.AfterSale;
import com.nougyou.aftersale.mapper.AfterSaleMapper;
import com.nougyou.aftersale.mapper.AfterSaleReferenceMapper;
import com.nougyou.common.api.ResultCode;
import com.nougyou.common.exception.BusinessException;
import com.nougyou.common.page.PageResult;
import com.nougyou.order.entity.Order;
import com.nougyou.order.entity.OrderItem;
import com.nougyou.order.mapper.OrderItemMapper;
import com.nougyou.order.mapper.OrderMapper;
import com.nougyou.security.SecurityUtils;
import com.nougyou.system.entity.SysDept;
import com.nougyou.system.entity.SysUser;
import com.nougyou.system.mapper.SysDeptMapper;
import com.nougyou.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * 售后服务（API-061 ~ API-066）。
 *
 * 状态流转：0 待处理 →（商家受理退款）→ 2 已完成；0 →（商家拒绝）→ 3 已驳回；
 * 3 已驳回 →（管理员介入改判退款）→ 2 已完成。
 *
 * 订单侧只联动支付状态：售后退款完成后累计退款额达到订单实付，才把 pay_status 置为 2（已退款），
 * 部分退款订单仍是已支付。订单业务状态保持不变，售后进度以售后单状态为准，避免待收货订单
 * 因售后被误置为已完成而丢失「确认收货」入口。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AfterSaleService {

    private static final int STATUS_PENDING = 0;
    private static final int STATUS_PROCESSING = 1;
    private static final int STATUS_DONE = 2;
    private static final int STATUS_REJECTED = 3;

    private static final int TYPE_REFUND = 1;
    private static final int TYPE_RETURN = 2;

    private static final int PAY_PAID = 1;
    private static final int PAY_REFUNDED = 2;
    private static final int ORDER_UNPAID = 0;
    private static final int ORDER_CANCELED = 4;

    private static final int MAX_PAGE_SIZE = 100;

    private final AfterSaleMapper afterSaleMapper;
    private final AfterSaleReferenceMapper referenceMapper;
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final SysUserMapper userMapper;
    private final SysDeptMapper deptMapper;

    /* ============================================================
     * API-061 提交售后申请
     * ============================================================ */

    /**
     * 买家对已支付订单中的某个商品提交售后；同一明细同时只允许一条进行中的售后。
     */
    @Transactional
    public AfterSaleVO create(Long buyerId, AfterSaleCreateForm form) {
        Order order = requireOrder(form.getOrderId());
        if (!buyerId.equals(order.getBuyerId())) {
            throw new BusinessException(ResultCode.PERMISSION_DENIED, "无权对该订单申请售后");
        }
        if (!Integer.valueOf(PAY_PAID).equals(order.getPayStatus())) {
            throw new BusinessException("仅已支付订单可申请售后");
        }
        if (Integer.valueOf(ORDER_UNPAID).equals(order.getOrderStatus())
                || Integer.valueOf(ORDER_CANCELED).equals(order.getOrderStatus())) {
            throw new BusinessException("该订单当前状态不可申请售后");
        }

        OrderItem item = orderItemMapper.selectById(form.getOrderItemId());
        if (item == null || !order.getId().equals(item.getOrderId())) {
            throw new BusinessException("订单明细不存在或不属于该订单");
        }
        boolean processing = afterSaleMapper.exists(Wrappers.<AfterSale>lambdaQuery()
                .eq(AfterSale::getOrderItemId, item.getId())
                .in(AfterSale::getStatus, STATUS_PENDING, STATUS_PROCESSING));
        if (processing) {
            throw new BusinessException("该商品已有处理中的售后，请等待处理完成");
        }

        int saleType = form.getSaleType();
        BigDecimal refundAmount = normalizeRefund(saleType, form.getRefundAmount(), item);

        AfterSale afterSale = new AfterSale();
        afterSale.setAfterSaleNo(nextAfterSaleNo());
        afterSale.setOrderId(order.getId());
        afterSale.setOrderItemId(item.getId());
        afterSale.setBuyerId(buyerId);
        afterSale.setFarmerId(order.getFarmerId());
        afterSale.setSaleType(saleType);
        afterSale.setReason(trimToNull(form.getReason()));
        afterSale.setDescription(trimToNull(form.getDescription()));
        afterSale.setImages(joinImages(form.getImages()));
        afterSale.setRefundAmount(refundAmount);
        afterSale.setStatus(STATUS_PENDING);
        afterSaleMapper.insert(afterSale);

        log.info("买家 {} 对订单 {} 提交售后 {}，类型 {}，申请退款 {}",
                buyerId, order.getOrderNo(), afterSale.getAfterSaleNo(), saleType, refundAmount);
        return detail(buyerId, afterSale.getId());
    }

    /**
     * 退款金额规则：仅退款 / 退货退款必须填写且不超过该明细小计；
     * 换货与咨询不涉及退款，统一记 0。
     */
    private BigDecimal normalizeRefund(int saleType, BigDecimal requested, OrderItem item) {
        if (saleType != TYPE_REFUND && saleType != TYPE_RETURN) {
            return BigDecimal.ZERO;
        }
        if (requested == null || requested.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("请填写退款金额");
        }
        BigDecimal subtotal = item.getSubtotal() == null ? BigDecimal.ZERO : item.getSubtotal();
        if (requested.compareTo(subtotal) > 0) {
            throw new BusinessException("退款金额不能超过该商品小计 ￥" + subtotal);
        }
        return requested.setScale(2, java.math.RoundingMode.HALF_UP);
    }

    /* ============================================================
     * API-062 / API-064 / 后台列表
     * ============================================================ */

    /** API-062 我的售后列表 */
    public PageResult<AfterSaleVO> myPage(Long buyerId, AfterSalePageQuery query) {
        var wrapper = Wrappers.<AfterSale>lambdaQuery().eq(AfterSale::getBuyerId, buyerId);
        return pageOf(wrapper, query);
    }

    /** API-064 商家售后列表 */
    public PageResult<AfterSaleVO> farmerPage(Long farmerId, AfterSalePageQuery query) {
        var wrapper = Wrappers.<AfterSale>lambdaQuery().eq(AfterSale::getFarmerId, farmerId);
        return pageOf(wrapper, query);
    }

    /**
     * 后台售后列表，作为 API-066 平台介入的前置查询：管理员按状态筛出「已驳回」等需要终裁的单据。
     */
    public PageResult<AfterSaleVO> adminPage(AfterSalePageQuery query) {
        return pageOf(Wrappers.<AfterSale>lambdaQuery(), query);
    }

    private PageResult<AfterSaleVO> pageOf(LambdaQueryWrapper<AfterSale> wrapper, AfterSalePageQuery query) {
        if (query.getStatus() != null) {
            wrapper.eq(AfterSale::getStatus, query.getStatus());
        }
        if (query.getStartTime() != null) {
            wrapper.ge(AfterSale::getCreateTime, query.getStartTime());
        }
        if (query.getEndTime() != null) {
            wrapper.le(AfterSale::getCreateTime, query.getEndTime());
        }
        if (StringUtils.hasText(query.getKeyword())) {
            String keyword = query.getKeyword().trim();
            wrapper.and(w -> w.like(AfterSale::getAfterSaleNo, keyword)
                    .or().apply("order_id IN (SELECT id FROM orders WHERE order_no LIKE CONCAT('%', {0}, '%'))",
                            keyword)
                    .or().apply("order_item_id IN (SELECT id FROM order_item "
                            + "WHERE product_name LIKE CONCAT('%', {0}, '%'))", keyword));
        }
        long pageNum = Math.max(query.getPageNum(), 1);
        long pageSize = Math.min(Math.max(query.getPageSize(), 1), MAX_PAGE_SIZE);
        wrapper.orderByDesc(AfterSale::getCreateTime).orderByDesc(AfterSale::getId);
        Page<AfterSale> page = afterSaleMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return new PageResult<>(assemble(page.getRecords()), page.getTotal());
    }

    /* ============================================================
     * API-063 售后详情
     * ============================================================ */
    /**
     * 订单详情页按明细展示售后进度：返回该订单下全部售后单（含历史记录）。
     * 归属校验与详情一致，买家、本店商家、管理员可查。
     */
    public List<AfterSaleVO> byOrder(Long currentUserId, Long orderId) {
        Order order = requireOrder(orderId);
        boolean admin = SecurityUtils.hasAuthority("afterSale:intervene");
        boolean owner = currentUserId.equals(order.getBuyerId()) || currentUserId.equals(order.getFarmerId());
        if (!admin && !owner) {
            throw new BusinessException(ResultCode.PERMISSION_DENIED, "无权查看该订单的售后");
        }
        List<AfterSale> records = afterSaleMapper.selectList(Wrappers.<AfterSale>lambdaQuery()
                .eq(AfterSale::getOrderId, orderId)
                .orderByDesc(AfterSale::getCreateTime)
                .orderByDesc(AfterSale::getId));
        return assemble(records);
    }

    /**
     * 买家本人、本店商家、具备平台介入权限的管理员可查看。
     */
    public AfterSaleVO detail(Long currentUserId, Long id) {
        AfterSale afterSale = requireAfterSale(id);
        boolean admin = SecurityUtils.hasAuthority("afterSale:intervene");
        boolean owner = currentUserId.equals(afterSale.getBuyerId())
                || currentUserId.equals(afterSale.getFarmerId());
        if (!admin && !owner) {
            throw new BusinessException(ResultCode.PERMISSION_DENIED, "无权查看该售后");
        }
        return assemble(List.of(afterSale)).get(0);
    }

    /* ============================================================
     * API-065 商家处理 / API-066 平台介入
     * ============================================================ */

    /**
     * 商家受理并退款（handleResult=1）或拒绝（handleResult=3）。
     */
    @Transactional
    public AfterSaleVO handle(Long farmerId, Long id, AfterSaleHandleForm form) {
        AfterSale afterSale = requireAfterSale(id);
        if (!farmerId.equals(afterSale.getFarmerId())) {
            throw new BusinessException(ResultCode.PERMISSION_DENIED, "无权处理其他商家的售后");
        }
        if (!Integer.valueOf(STATUS_PENDING).equals(afterSale.getStatus())
                && !Integer.valueOf(STATUS_PROCESSING).equals(afterSale.getStatus())) {
            throw new BusinessException("该售后已处理，请刷新后查看");
        }
        if (Integer.valueOf(1).equals(form.getHandleResult())) {
            accept(afterSale, farmerId, form);
        } else if (Integer.valueOf(3).equals(form.getHandleResult())) {
            reject(afterSale, farmerId, form);
        } else {
            throw new BusinessException("处理结果只能是 1 受理退款 或 3 拒绝");
        }
        return detail(farmerId, id);
    }

    /**
     * 平台介入：商家已拒绝的单据由管理员终裁，可改判退款或维持驳回。
     */
    @Transactional
    public AfterSaleVO intervene(Long adminId, Long id, AfterSaleHandleForm form) {
        AfterSale afterSale = requireAfterSale(id);
        if (!Integer.valueOf(STATUS_REJECTED).equals(afterSale.getStatus())) {
            throw new BusinessException("仅商家已拒绝的售后可申请平台介入");
        }
        if (!StringUtils.hasText(form.getHandleRemark())) {
            throw new BusinessException("请填写裁定说明");
        }
        if (Integer.valueOf(1).equals(form.getHandleResult())) {
            accept(afterSale, adminId, form);
        } else if (Integer.valueOf(3).equals(form.getHandleResult())) {
            // 维持驳回：只补记裁定人与说明，状态不变
            afterSale.setHandleUserId(adminId);
            afterSale.setHandleTime(LocalDateTime.now());
            afterSale.setHandleRemark(form.getHandleRemark().trim());
            afterSaleMapper.updateById(afterSale);
            log.info("管理员 {} 维持驳回售后 {}", adminId, afterSale.getAfterSaleNo());
        } else {
            throw new BusinessException("处理结果只能是 1 支持买家退款 或 3 驳回申请");
        }
        return detail(adminId, id);
    }

    /** 受理并退款：写入实际退款额、退货退款回滚库存、按累计退款更新订单支付状态 */
    private void accept(AfterSale afterSale, Long handleUserId, AfterSaleHandleForm form) {
        OrderItem item = orderItemMapper.selectById(afterSale.getOrderItemId());
        if (item == null) {
            throw new BusinessException("售后对应的订单明细不存在");
        }
        BigDecimal requested = afterSale.getRefundAmount() == null
                ? BigDecimal.ZERO : afterSale.getRefundAmount();
        BigDecimal actual = form.getRefundAmount() == null ? requested : form.getRefundAmount();
        if (actual.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("退款金额不能为负数");
        }
        boolean refundType = Integer.valueOf(TYPE_REFUND).equals(afterSale.getSaleType())
                || Integer.valueOf(TYPE_RETURN).equals(afterSale.getSaleType());
        if (refundType && actual.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("请填写实际退款金额");
        }
        if (requested.compareTo(BigDecimal.ZERO) > 0 && actual.compareTo(requested) > 0) {
            throw new BusinessException("退款金额不能超过买家申请金额 ￥" + requested);
        }
        BigDecimal subtotal = item.getSubtotal() == null ? BigDecimal.ZERO : item.getSubtotal();
        if (actual.compareTo(subtotal) > 0) {
            throw new BusinessException("退款金额不能超过该商品小计 ￥" + subtotal);
        }

        afterSale.setRefundAmount(actual.setScale(2, java.math.RoundingMode.HALF_UP));
        afterSale.setStatus(STATUS_DONE);
        afterSale.setHandleUserId(handleUserId);
        afterSale.setHandleTime(LocalDateTime.now());
        afterSale.setHandleRemark(trimToNull(form.getHandleRemark()));
        afterSaleMapper.updateById(afterSale);

        // 退货退款：商品退回仓库，恢复库存；仅退款不涉及实物回流
        if (Integer.valueOf(TYPE_RETURN).equals(afterSale.getSaleType())) {
            int quantity = item.getQuantity() == null ? 0 : item.getQuantity();
            if (quantity > 0 && item.getProductId() != null) {
                referenceMapper.restoreStock(item.getProductId(), quantity);
            }
        }
        refreshOrderPayStatus(afterSale.getOrderId());
        log.info("售后 {} 已受理退款 ￥{}，处理人 {}", afterSale.getAfterSaleNo(), actual, handleUserId);
    }

    private void reject(AfterSale afterSale, Long handleUserId, AfterSaleHandleForm form) {
        if (!StringUtils.hasText(form.getHandleRemark())) {
            throw new BusinessException("拒绝售后时请填写处理意见");
        }
        afterSale.setStatus(STATUS_REJECTED);
        afterSale.setHandleUserId(handleUserId);
        afterSale.setHandleTime(LocalDateTime.now());
        afterSale.setHandleRemark(form.getHandleRemark().trim());
        afterSaleMapper.updateById(afterSale);
        log.info("售后 {} 被 {} 驳回：{}", afterSale.getAfterSaleNo(), handleUserId,
                form.getHandleRemark());
    }

    /** 累计退款达到订单实付时标记为已退款，部分退款保持已支付 */
    private void refreshOrderPayStatus(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            return;
        }
        BigDecimal refunded = referenceMapper.sumRefunded(orderId);
        if (refunded == null) {
            refunded = BigDecimal.ZERO;
        }
        BigDecimal payAmount = order.getPayAmount() == null ? BigDecimal.ZERO : order.getPayAmount();
        int payStatus = refunded.compareTo(payAmount) >= 0 && payAmount.compareTo(BigDecimal.ZERO) > 0
                ? PAY_REFUNDED : PAY_PAID;
        if (!Integer.valueOf(payStatus).equals(order.getPayStatus())) {
            order.setPayStatus(payStatus);
            orderMapper.updateById(order);
            log.info("订单 {} 累计退款 ￥{}，支付状态更新为 {}", order.getOrderNo(), refunded, payStatus);
        }
    }

    /* ============================================================
     * 内部方法
     * ============================================================ */

    private AfterSale requireAfterSale(Long id) {
        AfterSale afterSale = afterSaleMapper.selectById(id);
        if (afterSale == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "售后记录不存在");
        }
        return afterSale;
    }

    private Order requireOrder(Long id) {
        Order order = orderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "订单不存在");
        }
        return order;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
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

    private List<String> splitImages(String images) {
        if (!StringUtils.hasText(images)) {
            return List.of();
        }
        return Arrays.stream(images.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
    }

    /**
     * 批量拼装，避免列表 N+1：一次取订单、明细、用户与店铺名。
     */
    private List<AfterSaleVO> assemble(List<AfterSale> records) {
        if (records == null || records.isEmpty()) {
            return List.of();
        }
        List<Long> orderIds = records.stream().map(AfterSale::getOrderId)
                .filter(Objects::nonNull).distinct().toList();
        Map<Long, Order> orders = orderIds.isEmpty() ? Map.of()
                : orderMapper.selectBatchIds(orderIds).stream()
                .collect(Collectors.toMap(Order::getId, order -> order, (a, b) -> a));

        List<Long> itemIds = records.stream().map(AfterSale::getOrderItemId)
                .filter(Objects::nonNull).distinct().toList();
        Map<Long, OrderItem> items = itemIds.isEmpty() ? Map.of()
                : orderItemMapper.selectBatchIds(itemIds).stream()
                .collect(Collectors.toMap(OrderItem::getId, item -> item, (a, b) -> a));

        List<Long> userIds = new ArrayList<>();
        records.forEach(record -> {
            userIds.add(record.getBuyerId());
            userIds.add(record.getFarmerId());
            userIds.add(record.getHandleUserId());
        });
        List<Long> distinctUserIds = userIds.stream().filter(Objects::nonNull).distinct().toList();
        Map<Long, SysUser> users = distinctUserIds.isEmpty() ? Map.of()
                : userMapper.selectBatchIds(distinctUserIds).stream()
                .collect(Collectors.toMap(SysUser::getId, user -> user, (a, b) -> a));

        List<Long> deptIds = users.values().stream().map(SysUser::getDeptId)
                .filter(Objects::nonNull).distinct().toList();
        Map<Long, String> shopNames = new LinkedHashMap<>();
        if (!deptIds.isEmpty()) {
            for (SysDept dept : deptMapper.selectBatchIds(deptIds)) {
                shopNames.put(dept.getId(), dept.getName());
            }
        }

        List<AfterSaleVO> result = new ArrayList<>();
        for (AfterSale record : records) {
            Order order = orders.get(record.getOrderId());
            OrderItem item = items.get(record.getOrderItemId());
            SysUser buyer = users.get(record.getBuyerId());
            SysUser farmer = users.get(record.getFarmerId());
            String shopName = farmer == null || farmer.getDeptId() == null
                    ? null : shopNames.get(farmer.getDeptId());
            result.add(AfterSaleVO.builder()
                    .id(record.getId())
                    .afterSaleNo(record.getAfterSaleNo())
                    .orderId(record.getOrderId())
                    .orderNo(order == null ? null : order.getOrderNo())
                    .orderItemId(record.getOrderItemId())
                    .productName(item == null ? null : item.getProductName())
                    .productCover(item == null ? null : item.getProductCover())
                    .quantity(item == null ? null : item.getQuantity())
                    .itemSubtotal(item == null ? null : item.getSubtotal())
                    .buyerId(record.getBuyerId())
                    .buyerName(displayName(buyer))
                    .farmerId(record.getFarmerId())
                    .farmerName(displayName(farmer))
                    .shopName(shopName)
                    .saleType(record.getSaleType())
                    .reason(record.getReason())
                    .description(record.getDescription())
                    .images(splitImages(record.getImages()))
                    .refundAmount(record.getRefundAmount())
                    .status(record.getStatus())
                    .handleUserId(record.getHandleUserId())
                    .handleUserName(displayName(users.get(record.getHandleUserId())))
                    .handleTime(record.getHandleTime())
                    .handleRemark(record.getHandleRemark())
                    .createTime(record.getCreateTime())
                    .build());
        }
        return result;
    }

    private String displayName(SysUser user) {
        if (user == null) {
            return null;
        }
        return StringUtils.hasText(user.getNickname()) ? user.getNickname() : user.getUsername();
    }

    /** 售后单号：SH + yyyyMMdd + 4 位随机数，重复时重试 */
    private String nextAfterSaleNo() {
        String date = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        for (int i = 0; i < 8; i++) {
            String no = "SH" + date + String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
            if (!afterSaleMapper.exists(Wrappers.<AfterSale>lambdaQuery()
                    .eq(AfterSale::getAfterSaleNo, no))) {
                return no;
            }
        }
        throw new BusinessException("售后单号生成失败，请重试");
    }
}
