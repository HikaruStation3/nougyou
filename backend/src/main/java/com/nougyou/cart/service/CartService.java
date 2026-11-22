package com.nougyou.cart.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.nougyou.cart.dto.CartAddForm;
import com.nougyou.cart.dto.CartUpdateForm;
import com.nougyou.cart.dto.CartVO;
import com.nougyou.cart.entity.Cart;
import com.nougyou.cart.mapper.CartMapper;
import com.nougyou.common.exception.BusinessException;
import com.nougyou.product.entity.Product;
import com.nougyou.product.mapper.ProductMapper;
import com.nougyou.system.entity.SysDept;
import com.nougyou.system.entity.SysUser;
import com.nougyou.system.mapper.SysDeptMapper;
import com.nougyou.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartService {

    private static final int SHELF_ON = 1;
    /** 同一商品单次加购与购物车内累计数量的上限（与接口文档 1-999 约束一致） */
    private static final int MAX_QUANTITY = 999;

    private final CartMapper cartMapper;
    private final ProductMapper productMapper;
    private final SysUserMapper userMapper;
    private final SysDeptMapper deptMapper;

    /**
     * API-047 购物车列表：按商家分组返回，前端展示店铺小计。
     */
    public List<CartVO> list(Long buyerId) {
        List<Cart> carts = cartMapper.selectList(Wrappers.<Cart>lambdaQuery()
                .eq(Cart::getBuyerId, buyerId)
                .orderByAsc(Cart::getId));
        if (carts.isEmpty()) {
            return List.of();
        }

        List<Long> productIds = carts.stream().map(Cart::getProductId).filter(Objects::nonNull).distinct().toList();
        Map<Long, Product> products = productIds.isEmpty() ? Map.of()
                : productMapper.selectBatchIds(productIds).stream()
                .collect(Collectors.toMap(Product::getId, product -> product, (a, b) -> a));
        Map<Long, FarmerInfo> farmerInfos = farmerInfos(products.values().stream()
                .map(Product::getFarmerId).filter(Objects::nonNull).distinct().toList());

        return carts.stream()
                .map(cart -> toVO(cart, products.get(cart.getProductId()), farmerInfos))
                .toList();
    }

    /**
     * API-048 加入购物车：同一商品重复加购时累加数量，不新增行。
     */
    @Transactional
    public CartVO add(Long buyerId, CartAddForm form) {
        Product product = productMapper.selectById(form.getProductId());
        if (product == null) {
            throw new BusinessException("商品不存在");
        }
        if (!Integer.valueOf(SHELF_ON).equals(product.getShelfStatus())) {
            throw new BusinessException("商品已下架，无法加入购物车");
        }
        int stock = product.getStock() == null ? 0 : product.getStock();
        if (stock <= 0) {
            throw new BusinessException("商品库存不足");
        }

        Cart cart = cartMapper.selectOne(Wrappers.<Cart>lambdaQuery()
                .eq(Cart::getBuyerId, buyerId)
                .eq(Cart::getProductId, form.getProductId())
                .last("limit 1"));

        int quantity = form.getQuantity();
        if (cart == null) {
            if (quantity > stock) {
                throw new BusinessException("购买数量超过商品库存，当前库存 " + stock);
            }
            cart = new Cart();
            cart.setBuyerId(buyerId);
            cart.setProductId(form.getProductId());
            cart.setQuantity(quantity);
            cart.setSelected(1);
            cartMapper.insert(cart);
        } else {
            int merged = (cart.getQuantity() == null ? 0 : cart.getQuantity()) + quantity;
            if (merged > MAX_QUANTITY) {
                throw new BusinessException("同一商品最多购买 " + MAX_QUANTITY + " 件");
            }
            if (merged > stock) {
                throw new BusinessException("购买数量超过商品库存，当前库存 " + stock);
            }
            cart.setQuantity(merged);
            cartMapper.updateById(cart);
        }

        Map<Long, FarmerInfo> farmerInfos = farmerInfos(List.of(product.getFarmerId()));
        return toVO(cart, product, farmerInfos);
    }

    /**
     * API-049 修改购物车：改数量或勾选状态。
     */
    @Transactional
    public CartVO update(Long buyerId, Long id, CartUpdateForm form) {
        Cart cart = requireOwned(buyerId, id);
        Product product = productMapper.selectById(cart.getProductId());
        if (product == null) {
            throw new BusinessException("商品不存在");
        }
        if (form.getQuantity() != null) {
            int stock = product.getStock() == null ? 0 : product.getStock();
            if (form.getQuantity() > stock) {
                throw new BusinessException("购买数量超过商品库存，当前库存 " + stock);
            }
            cart.setQuantity(form.getQuantity());
        }
        if (form.getSelected() != null) {
            cart.setSelected(form.getSelected());
        }
        cartMapper.updateById(cart);
        Map<Long, FarmerInfo> farmerInfos = farmerInfos(List.of(product.getFarmerId()));
        return toVO(cart, product, farmerInfos);
    }

    /**
     * API-050 删除购物车行。
     */
    @Transactional
    public void remove(Long buyerId, Long id) {
        Cart cart = requireOwned(buyerId, id);
        cartMapper.deleteById(cart.getId());
    }

    /**
     * API-051 批量删除 / 清空购物车：传 ids 删除指定行，不传则清空全部。
     */
    @Transactional
    public void removeBatch(Long buyerId, List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            cartMapper.delete(Wrappers.<Cart>lambdaQuery().eq(Cart::getBuyerId, buyerId));
            return;
        }
        cartMapper.delete(Wrappers.<Cart>lambdaQuery()
                .eq(Cart::getBuyerId, buyerId)
                .in(Cart::getId, ids));
    }

    private Cart requireOwned(Long buyerId, Long id) {
        Cart cart = cartMapper.selectById(id);
        if (cart == null) {
            throw new BusinessException("购物车记录不存在");
        }
        if (!buyerId.equals(cart.getBuyerId())) {
            throw new BusinessException("无权操作他人的购物车");
        }
        return cart;
    }

    private CartVO toVO(Cart cart, Product product, Map<Long, FarmerInfo> farmerInfos) {
        if (product == null) {
            // 商品已逻辑删除，仍保留购物车行但置为失效
            return CartVO.builder()
                    .id(String.valueOf(cart.getId()))
                    .productId(String.valueOf(cart.getProductId()))
                    .productName("商品已下架")
                    .price(BigDecimal.ZERO)
                    .stock(0)
                    .quantity(cart.getQuantity())
                    .selected(cart.getSelected())
                    .subtotal(BigDecimal.ZERO)
                    .invalid(true)
                    .build();
        }
        FarmerInfo info = farmerInfos.get(product.getFarmerId());
        BigDecimal price = product.getPrice() == null ? BigDecimal.ZERO : product.getPrice();
        int stock = product.getStock() == null ? 0 : product.getStock();
        int quantity = cart.getQuantity() == null ? 0 : cart.getQuantity();
        boolean invalid = !Integer.valueOf(SHELF_ON).equals(product.getShelfStatus()) || stock < quantity;
        return CartVO.builder()
                .id(String.valueOf(cart.getId()))
                .productId(String.valueOf(product.getId()))
                .productName(product.getName())
                .cover(product.getCover())
                .price(price)
                .stock(stock)
                .unit(product.getUnit())
                .quantity(quantity)
                .selected(cart.getSelected())
                .subtotal(price.multiply(BigDecimal.valueOf(quantity)))
                .invalid(invalid)
                .farmerId(product.getFarmerId() == null ? null : String.valueOf(product.getFarmerId()))
                .farmerName(info == null ? null : info.farmerName())
                .shopName(info == null ? null : info.shopName())
                .build();
    }

    private record FarmerInfo(String farmerName, String shopName) {
    }

    private Map<Long, FarmerInfo> farmerInfos(List<Long> farmerIds) {
        List<Long> ids = farmerIds == null ? List.of() : farmerIds.stream().filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        Map<Long, SysUser> users = userMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(SysUser::getId, user -> user, (a, b) -> a));
        List<Long> deptIds = users.values().stream()
                .map(SysUser::getDeptId).filter(Objects::nonNull).distinct().toList();
        Map<Long, String> deptNames = deptIds.isEmpty() ? Map.of() : deptMapper.selectBatchIds(deptIds).stream()
                .collect(Collectors.toMap(SysDept::getId, SysDept::getName, (a, b) -> a));
        Map<Long, FarmerInfo> result = new LinkedHashMap<>();
        users.forEach((id, user) -> {
            String name = StringUtils.hasText(user.getNickname()) ? user.getNickname() : user.getUsername();
            String shop = user.getDeptId() == null ? null : deptNames.get(user.getDeptId());
            result.put(id, new FarmerInfo(name, shop));
        });
        return result;
    }

    /**
     * 按商家分组统计小计，供前端展示店铺小计。
     */
    public Map<String, BigDecimal> groupSubtotal(List<CartVO> items) {
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        for (CartVO item : items) {
            if (Boolean.TRUE.equals(item.getInvalid()) || item.getSelected() == null || item.getSelected() != 1) {
                continue;
            }
            String key = item.getFarmerId() == null ? "unknown" : item.getFarmerId();
            result.merge(key, item.getSubtotal(), BigDecimal::add);
        }
        return result;
    }

    /**
     * 清理买家购物车中指定商品的行（下单成功后调用）。
     */
    @Transactional
    public void removeByProductIds(Long buyerId, List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return;
        }
        List<Long> ids = new ArrayList<>(productIds);
        cartMapper.delete(Wrappers.<Cart>lambdaQuery()
                .eq(Cart::getBuyerId, buyerId)
                .in(Cart::getProductId, ids));
    }
}
