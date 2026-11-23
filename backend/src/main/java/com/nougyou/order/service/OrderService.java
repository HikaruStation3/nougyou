package com.nougyou.order.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nougyou.cart.entity.Cart;
import com.nougyou.cart.mapper.CartMapper;
import com.nougyou.common.api.ResultCode;
import com.nougyou.common.exception.BusinessException;
import com.nougyou.common.page.PageResult;
import com.nougyou.order.dto.OrderCreateForm;
import com.nougyou.order.dto.OrderItemVO;
import com.nougyou.order.dto.OrderPageQuery;
import com.nougyou.order.dto.OrderPayForm;
import com.nougyou.order.dto.OrderShipForm;
import com.nougyou.order.dto.OrderVO;
import com.nougyou.order.dto.row.ActiveEntrustRow;
import com.nougyou.order.dto.row.AfterSaleStatusRow;
import com.nougyou.order.entity.Order;
import com.nougyou.order.entity.OrderItem;
import com.nougyou.order.mapper.OrderItemMapper;
import com.nougyou.order.mapper.OrderMapper;
import com.nougyou.order.mapper.OrderReferenceMapper;
import com.nougyou.product.entity.Product;
import com.nougyou.product.mapper.ProductMapper;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * 订单服务（API-052 ~ API-060）。
 *
 * 库存策略：下单即预占库存（原子扣减，避免超卖），取消订单回滚库存，
 * 发货时只累加销量、不再二次扣减，因此「取消回滚」与「发货累加销量」不会互相冲突。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private static final int PAY_UNPAID = 0;
    private static final int PAY_PAID = 1;
    private static final int STATUS_UNPAID = 0;
    private static final int STATUS_UNSHIPPED = 1;
    private static final int STATUS_UNRECEIVED = 2;
    private static final int STATUS_FINISHED = 3;
    private static final int STATUS_CANCELED = 4;
    private static final int SHELF_ON = 1;
    private static final int MAX_PAGE_SIZE = 100;

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final OrderReferenceMapper referenceMapper;
    private final CartMapper cartMapper;
    private final ProductMapper productMapper;
    private final SysUserMapper userMapper;
    private final SysDeptMapper deptMapper;

    /* ============================================================
     * API-052 提交订单
     * ============================================================ */

    /**
     * 从购物车或立即购买下单，按商家自动拆单；任一商品校验失败则整单回滚。
     */
    @Transactional
    public List<OrderVO> create(Long buyerId, OrderCreateForm form) {
        List<ItemDraft> drafts = resolveDrafts(buyerId, form);

        // 先原子预占库存，失败即抛异常回滚整个事务
        for (ItemDraft draft : drafts) {
            int updated = referenceMapper.deductStock(draft.product().getId(), draft.quantity());
            if (updated == 0) {
                throw new BusinessException("「" + draft.product().getName() + "」库存不足，请调整数量后重试");
            }
        }

        Map<Long, List<ItemDraft>> grouped = drafts.stream()
                .collect(Collectors.groupingBy(ItemDraft::sellerId,
                        LinkedHashMap::new, Collectors.toList()));

        List<Order> createdOrders = new ArrayList<>();
        for (Map.Entry<Long, List<ItemDraft>> entry : grouped.entrySet()) {
            createdOrders.add(createOrder(buyerId, entry.getKey(), entry.getValue(), form));
        }

        // 购物车下单成功后清理已结算的行
        if (isCartSource(form.getSource())) {
            List<Long> cartIds = distinctIds(form.getCartIds());
            if (!cartIds.isEmpty()) {
                cartMapper.deleteBatchIds(cartIds);
            }
        }

        List<Order> saved = createdOrders.stream()
                .map(order -> orderMapper.selectById(order.getId()))
                .filter(Objects::nonNull)
                .toList();
        return assemble(saved, true);
    }

    private Order createOrder(Long buyerId, Long sellerId, List<ItemDraft> drafts, OrderCreateForm form) {
        BigDecimal totalAmount = drafts.stream()
                .map(draft -> draft.product().getPrice().multiply(BigDecimal.valueOf(draft.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Order order = new Order();
        order.setOrderNo(nextOrderNo());
        order.setBuyerId(buyerId);
        order.setFarmerId(sellerId);
        order.setTotalAmount(totalAmount);
        order.setFreight(BigDecimal.ZERO);
        order.setPayAmount(totalAmount);
        order.setPayStatus(PAY_UNPAID);
        order.setPayType(form.getPayType() == null ? 3 : form.getPayType());
        order.setOrderStatus(STATUS_UNPAID);
        order.setReceiverName(form.getReceiverName().trim());
        order.setReceiverPhone(form.getReceiverPhone().trim());
        order.setReceiverAddress(form.getReceiverAddress().trim());
        order.setRemark(StringUtils.hasText(form.getRemark()) ? form.getRemark().trim() : null);
        orderMapper.insert(order);

        for (ItemDraft draft : drafts) {
            Product product = draft.product();
            BigDecimal price = product.getPrice() == null ? BigDecimal.ZERO : product.getPrice();
            OrderItem item = new OrderItem();
            item.setOrderId(order.getId());
            item.setProductId(product.getId());
            item.setEntrustId(draft.entrustId());
            item.setProductName(product.getName());
            item.setProductCover(product.getCover());
            item.setPrice(price);
            item.setQuantity(draft.quantity());
            item.setSubtotal(price.multiply(BigDecimal.valueOf(draft.quantity())));
            orderItemMapper.insert(item);
        }
        return order;
    }

    private List<ItemDraft> resolveDrafts(Long buyerId, OrderCreateForm form) {
        List<ItemDraft> drafts = new ArrayList<>();
        if (isCartSource(form.getSource())) {
            List<Long> cartIds = distinctIds(form.getCartIds());
            if (cartIds.isEmpty()) {
                throw new BusinessException("请选择要结算的购物车商品");
            }
            List<Cart> carts = cartMapper.selectList(Wrappers.<Cart>lambdaQuery()
                    .eq(Cart::getBuyerId, buyerId)
                    .in(Cart::getId, cartIds));
            if (carts.size() != cartIds.size()) {
                throw new BusinessException("部分购物车记录不存在，请刷新后重试");
            }
            for (Cart cart : carts) {
                int quantity = cart.getQuantity() == null ? 0 : cart.getQuantity();
                drafts.add(toDraft(requireAvailableProduct(cart.getProductId(), quantity), quantity));
            }
            return drafts;
        }
        if ("buyNow".equalsIgnoreCase(form.getSource())) {
            if (form.getProductId() == null) {
                throw new BusinessException("立即购买必须指定商品");
            }
            int quantity = form.getQuantity() == null ? 1 : form.getQuantity();
            drafts.add(toDraft(requireAvailableProduct(form.getProductId(), quantity), quantity));
            return drafts;
        }
        throw new BusinessException("下单来源只能是 cart 或 buyNow");
    }

    /**
     * 组装下单明细：商品若存在唯一生效中的委托关系，订单归属受托方并记录委托关系 ID，
     * 这样受托方能在「商家订单」里发货，委托销售额也会计入对应统计。
     */
    private ItemDraft toDraft(Product product, int quantity) {
        ActiveEntrustRow entrust = referenceMapper.findSingleActiveEntrust(product.getId());
        Long sellerId = entrust == null ? product.getFarmerId() : entrust.getAgentUserId();
        Long entrustId = entrust == null ? null : entrust.getId();
        return new ItemDraft(product, quantity, sellerId, entrustId);
    }

    private boolean isCartSource(String source) {
        return "cart".equalsIgnoreCase(source == null ? "" : source.trim());
    }

    private Product requireAvailableProduct(Long productId, int quantity) {
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw new BusinessException("商品不存在或已删除");
        }
        if (!Integer.valueOf(SHELF_ON).equals(product.getShelfStatus())) {
            throw new BusinessException("「" + product.getName() + "」已下架，无法下单");
        }
        int stock = product.getStock() == null ? 0 : product.getStock();
        if (quantity < 1) {
            throw new BusinessException("购买数量至少为 1");
        }
        if (stock < quantity) {
            throw new BusinessException("「" + product.getName() + "」库存不足，当前库存 " + stock);
        }
        return product;
    }

    private List<Long> distinctIds(List<Long> ids) {
        return ids == null ? List.of()
                : ids.stream().filter(Objects::nonNull).distinct().toList();
    }

    /* ============================================================
     * API-053 / API-058 / API-060 列表查询
     * ============================================================ */

    /**
     * API-053 我的订单：仅返回当前买家的订单。
     */
    public PageResult<OrderVO> myPage(Long buyerId, OrderPageQuery query) {
        var wrapper = Wrappers.<Order>lambdaQuery().eq(Order::getBuyerId, buyerId);
        applyCommonFilters(wrapper, query);
        if (StringUtils.hasText(query.getKeyword())) {
            String keyword = query.getKeyword().trim();
            wrapper.and(w -> w.like(Order::getOrderNo, keyword)
                    .or().apply("EXISTS (SELECT 1 FROM order_item oi WHERE oi.order_id = orders.id "
                            + "AND oi.product_name LIKE CONCAT('%', {0}, '%'))", keyword));
        }
        return pageOf(wrapper, query);
    }

    /**
     * API-058 商家订单：仅返回当前商家店铺的订单。
     */
    public PageResult<OrderVO> farmerPage(Long farmerId, OrderPageQuery query) {
        var wrapper = Wrappers.<Order>lambdaQuery().eq(Order::getFarmerId, farmerId);
        applyCommonFilters(wrapper, query);
        if (StringUtils.hasText(query.getKeyword())) {
            String keyword = query.getKeyword().trim();
            wrapper.and(w -> w.like(Order::getOrderNo, keyword)
                    .or().apply("buyer_id IN (SELECT id FROM sys_user WHERE nickname LIKE CONCAT('%', {0}, '%') "
                            + "OR username LIKE CONCAT('%', {0}, '%'))", keyword));
        }
        return pageOf(wrapper, query);
    }

    /**
     * API-060 后台订单查询：全平台数据，支持订单号、买家、商家与时间范围。
     */
    public PageResult<OrderVO> adminPage(OrderPageQuery query) {
        var wrapper = Wrappers.<Order>lambdaQuery();
        applyCommonFilters(wrapper, query);
        if (StringUtils.hasText(query.getOrderNo())) {
            wrapper.eq(Order::getOrderNo, query.getOrderNo().trim());
        }
        if (query.getBuyerId() != null) {
            wrapper.eq(Order::getBuyerId, query.getBuyerId());
        }
        if (query.getFarmerId() != null) {
            wrapper.eq(Order::getFarmerId, query.getFarmerId());
        }
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.like(Order::getOrderNo, query.getKeyword().trim());
        }
        return pageOf(wrapper, query);
    }

    private void applyCommonFilters(com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Order> wrapper,
                                    OrderPageQuery query) {
        if (query.getOrderStatus() != null) {
            wrapper.eq(Order::getOrderStatus, query.getOrderStatus());
        }
        if (query.getStartTime() != null) {
            wrapper.ge(Order::getCreateTime, query.getStartTime());
        }
        if (query.getEndTime() != null) {
            wrapper.le(Order::getCreateTime, query.getEndTime());
        }
    }

    private PageResult<OrderVO> pageOf(
            com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Order> wrapper,
            OrderPageQuery query) {
        long pageNum = Math.max(query.getPageNum(), 1);
        long pageSize = Math.min(Math.max(query.getPageSize(), 1), MAX_PAGE_SIZE);
        wrapper.orderByDesc(Order::getCreateTime).orderByDesc(Order::getId);
        Page<Order> page = orderMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return new PageResult<>(assemble(page.getRecords(), true), page.getTotal());
    }

    /* ============================================================
     * API-054 订单详情
     * ============================================================ */

    /**
     * 买家、商家、管理员共用详情；非本人 / 非本店且无后台订单权限时拒绝。
     */
    public OrderVO detail(Long currentUserId, Long id) {
        Order order = requireOrder(id);
        boolean admin = SecurityUtils.hasAuthority("order:adminList");
        boolean owner = currentUserId.equals(order.getBuyerId()) || currentUserId.equals(order.getFarmerId());
        if (!admin && !owner) {
            throw new BusinessException(ResultCode.PERMISSION_DENIED, "无权查看该订单");
        }
        return assemble(List.of(order), false).get(0);
    }

    /* ============================================================
     * API-055 ~ API-057 买家操作
     * ============================================================ */

    /**
     * API-055 模拟支付：待付款订单置为已支付并进入待发货。
     */
    @Transactional
    public OrderVO pay(Long buyerId, Long id, OrderPayForm form) {
        Order order = requireBuyerOrder(buyerId, id);
        if (!Integer.valueOf(PAY_UNPAID).equals(order.getPayStatus())
                || !Integer.valueOf(STATUS_UNPAID).equals(order.getOrderStatus())) {
            throw new BusinessException("该订单当前状态不可支付");
        }
        order.setPayStatus(PAY_PAID);
        order.setPayType(form == null || form.getPayType() == null ? 3 : form.getPayType());
        order.setPayTime(LocalDateTime.now());
        order.setOrderStatus(STATUS_UNSHIPPED);
        orderMapper.updateById(order);
        return detail(buyerId, id);
    }

    /**
     * API-056 取消订单：仅待付款可取消，取消后回滚库存。
     */
    @Transactional
    public OrderVO cancel(Long buyerId, Long id, String reason) {
        Order order = requireBuyerOrder(buyerId, id);
        if (!Integer.valueOf(STATUS_UNPAID).equals(order.getOrderStatus())) {
            throw new BusinessException("仅待付款订单可取消");
        }
        order.setOrderStatus(STATUS_CANCELED);
        orderMapper.updateById(order);
        for (OrderItem item : itemsOf(id)) {
            referenceMapper.restoreStock(item.getProductId(),
                    item.getQuantity() == null ? 0 : item.getQuantity());
        }
        log.info("买家 {} 取消订单 {}，原因：{}", buyerId, order.getOrderNo(), reason);
        return detail(buyerId, id);
    }

    /**
     * API-057 确认收货：待收货订单置为已完成。
     */
    @Transactional
    public OrderVO confirm(Long buyerId, Long id) {
        Order order = requireBuyerOrder(buyerId, id);
        if (!Integer.valueOf(STATUS_UNRECEIVED).equals(order.getOrderStatus())) {
            throw new BusinessException("仅待收货订单可确认收货");
        }
        order.setOrderStatus(STATUS_FINISHED);
        order.setFinishTime(LocalDateTime.now());
        orderMapper.updateById(order);
        return detail(buyerId, id);
    }

    /* ============================================================
     * API-059 商家发货
     * ============================================================ */

    /**
     * 发货：仅本店已支付待发货订单可操作，同时累加商品销量。
     */
    @Transactional
    public OrderVO ship(Long farmerId, Long id, OrderShipForm form) {
        Order order = requireOrder(id);
        if (!farmerId.equals(order.getFarmerId())) {
            throw new BusinessException(ResultCode.PERMISSION_DENIED, "无权操作其他商家的订单");
        }
        if (!Integer.valueOf(STATUS_UNSHIPPED).equals(order.getOrderStatus())) {
            throw new BusinessException("仅待发货订单可发货");
        }
        order.setLogisticsCompany(form.getLogisticsCompany().trim());
        order.setLogisticsNo(form.getLogisticsNo().trim());
        order.setShipTime(LocalDateTime.now());
        order.setOrderStatus(STATUS_UNRECEIVED);
        orderMapper.updateById(order);
        for (OrderItem item : itemsOf(id)) {
            referenceMapper.increaseSaleCount(item.getProductId(),
                    item.getQuantity() == null ? 0 : item.getQuantity());
        }
        return detail(farmerId, id);
    }

    /* ============================================================
     * 内部方法
     * ============================================================ */

    private Order requireOrder(Long id) {
        Order order = orderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "订单不存在");
        }
        return order;
    }

    private Order requireBuyerOrder(Long buyerId, Long id) {
        Order order = requireOrder(id);
        if (!buyerId.equals(order.getBuyerId())) {
            throw new BusinessException(ResultCode.PERMISSION_DENIED, "无权操作他人的订单");
        }
        return order;
    }

    private List<OrderItem> itemsOf(Long orderId) {
        return orderItemMapper.selectList(Wrappers.<OrderItem>lambdaQuery()
                .eq(OrderItem::getOrderId, orderId)
                .orderByAsc(OrderItem::getId));
    }

    /**
     * 批量拼装订单出参，避免列表查询产生 N+1。
     */
    private List<OrderVO> assemble(List<Order> orders, boolean maskPhone) {
        if (orders == null || orders.isEmpty()) {
            return List.of();
        }
        List<Long> orderIds = orders.stream().map(Order::getId).toList();
        List<OrderItem> allItems = orderItemMapper.selectList(Wrappers.<OrderItem>lambdaQuery()
                .in(OrderItem::getOrderId, orderIds)
                .orderByAsc(OrderItem::getId));
        Map<Long, List<OrderItem>> itemMap = allItems.stream()
                .collect(Collectors.groupingBy(OrderItem::getOrderId));

        Map<Long, Integer> afterSaleStatus = afterSaleStatus(allItems);
        Map<Long, SysUser> users = usersOf(orders);
        Map<Long, String> shopNames = shopNames(users.values().stream()
                .map(SysUser::getDeptId).filter(Objects::nonNull).distinct().toList());

        List<OrderVO> result = new ArrayList<>();
        for (Order order : orders) {
            List<OrderItem> items = itemMap.getOrDefault(order.getId(), List.of());
            String buyerName = displayName(users.get(order.getBuyerId()));
            String farmerName = displayName(users.get(order.getFarmerId()));
            SysUser farmer = users.get(order.getFarmerId());
            String shopName = farmer == null || farmer.getDeptId() == null
                    ? null : shopNames.get(farmer.getDeptId());

            List<OrderItemVO> itemVOs = items.stream().map(item -> OrderItemVO.builder()
                    .id(String.valueOf(item.getId()))
                    .productId(String.valueOf(item.getProductId()))
                    .productName(item.getProductName())
                    .productCover(item.getProductCover())
                    .price(item.getPrice())
                    .quantity(item.getQuantity())
                    .subtotal(item.getSubtotal())
                    .entrustId(item.getEntrustId() == null ? null : String.valueOf(item.getEntrustId()))
                    .afterSaleStatus(afterSaleStatus.get(item.getId()))
                    .build()).toList();

            boolean afterSaleAllowed = Integer.valueOf(PAY_PAID).equals(order.getPayStatus())
                    && (Integer.valueOf(STATUS_UNRECEIVED).equals(order.getOrderStatus())
                    || Integer.valueOf(STATUS_FINISHED).equals(order.getOrderStatus()))
                    && items.stream().noneMatch(item -> {
                        Integer status = afterSaleStatus.get(item.getId());
                        return status != null && (status == 0 || status == 1);
                    });

            result.add(OrderVO.builder()
                    .id(String.valueOf(order.getId()))
                    .orderNo(order.getOrderNo())
                    .buyerId(String.valueOf(order.getBuyerId()))
                    .buyerName(buyerName)
                    .farmerId(String.valueOf(order.getFarmerId()))
                    .farmerName(farmerName)
                    .shopName(shopName)
                    .totalAmount(order.getTotalAmount())
                    .freight(order.getFreight())
                    .payAmount(order.getPayAmount())
                    .payStatus(order.getPayStatus())
                    .payType(order.getPayType())
                    .payTime(order.getPayTime())
                    .orderStatus(order.getOrderStatus())
                    .receiverName(order.getReceiverName())
                    .receiverPhone(maskPhone ? maskPhone(order.getReceiverPhone()) : order.getReceiverPhone())
                    .receiverAddress(order.getReceiverAddress())
                    .remark(order.getRemark())
                    .logisticsCompany(order.getLogisticsCompany())
                    .logisticsNo(order.getLogisticsNo())
                    .shipTime(order.getShipTime())
                    .finishTime(order.getFinishTime())
                    .createTime(order.getCreateTime())
                    .items(itemVOs)
                    .afterSaleAllowed(afterSaleAllowed)
                    .build());
        }
        return result;
    }

    private Map<Long, Integer> afterSaleStatus(List<OrderItem> items) {
        List<Long> itemIds = items.stream().map(OrderItem::getId).filter(Objects::nonNull).toList();
        if (itemIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, Integer> statusMap = new LinkedHashMap<>();
        for (AfterSaleStatusRow row : referenceMapper.findAfterSaleStatuses(itemIds)) {
            if (row.getOrderItemId() != null) {
                // 同一明细存在多条记录时，后写入的（id 更大）覆盖前面的
                statusMap.put(row.getOrderItemId(), row.getStatus());
            }
        }
        return statusMap;
    }

    private Map<Long, SysUser> usersOf(List<Order> orders) {
        List<Long> ids = new ArrayList<>();
        orders.forEach(order -> {
            ids.add(order.getBuyerId());
            ids.add(order.getFarmerId());
        });
        List<Long> distinct = ids.stream().filter(Objects::nonNull).distinct().toList();
        if (distinct.isEmpty()) {
            return Map.of();
        }
        return userMapper.selectBatchIds(distinct).stream()
                .collect(Collectors.toMap(SysUser::getId, user -> user, (a, b) -> a));
    }

    private Map<Long, String> shopNames(List<Long> deptIds) {
        if (deptIds == null || deptIds.isEmpty()) {
            return Map.of();
        }
        return deptMapper.selectBatchIds(deptIds).stream()
                .collect(Collectors.toMap(SysDept::getId, SysDept::getName, (a, b) -> a));
    }

    private String displayName(SysUser user) {
        if (user == null) {
            return null;
        }
        return StringUtils.hasText(user.getNickname()) ? user.getNickname() : user.getUsername();
    }

    /**
     * 手机号脱敏：138****8000。
     */
    private String maskPhone(String phone) {
        if (!StringUtils.hasText(phone) || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    /**
     * 订单号：ZS + yyyyMMdd + 4 位随机数，重复时重试。
     */
    private String nextOrderNo() {
        String date = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        for (int i = 0; i < 8; i++) {
            String orderNo = "ZS" + date + String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
            if (!orderMapper.exists(Wrappers.<Order>lambdaQuery().eq(Order::getOrderNo, orderNo))) {
                return orderNo;
            }
        }
        throw new BusinessException("订单号生成失败，请重试");
    }

    private record ItemDraft(Product product, int quantity, Long sellerId, Long entrustId) {
    }
}
