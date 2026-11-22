package com.nougyou.product.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nougyou.common.exception.BusinessException;
import com.nougyou.common.page.PageResult;
import com.nougyou.product.dto.CategoryForm;
import com.nougyou.product.dto.CategoryVO;
import com.nougyou.product.dto.ProductDetailVO;
import com.nougyou.product.dto.ProductForm;
import com.nougyou.product.dto.ProductPageQuery;
import com.nougyou.product.dto.ProductShelfForm;
import com.nougyou.product.dto.ProductVO;
import com.nougyou.product.entity.Product;
import com.nougyou.product.entity.ProductCategory;
import com.nougyou.product.mapper.ProductCategoryMapper;
import com.nougyou.product.mapper.ProductMapper;
import com.nougyou.product.mapper.ProductReferenceMapper;
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
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private static final int SHELF_OFF = 0;
    private static final int SHELF_ON = 1;
    private static final int CATEGORY_ENABLED = 1;
    private static final Long ROOT_PARENT_ID = 0L;

    private final ProductMapper productMapper;
    private final ProductCategoryMapper categoryMapper;
    private final ProductReferenceMapper referenceMapper;
    private final SysUserMapper userMapper;
    private final SysDeptMapper deptMapper;

    /* ============================================================
     * API-040 农产品分类树
     * ============================================================ */

    /**
     * 返回一级 + 二级分类树；onlyEnabled 为 true 时只返回启用分类。
     */
    public List<CategoryVO> categoryTree(Boolean onlyEnabled) {
        boolean enabledOnly = !Boolean.FALSE.equals(onlyEnabled);
        List<ProductCategory> categories = categoryMapper.selectList(
                Wrappers.<ProductCategory>lambdaQuery()
                        .eq(enabledOnly, ProductCategory::getStatus, CATEGORY_ENABLED)
                        .orderByAsc(ProductCategory::getSort)
                        .orderByAsc(ProductCategory::getId));
        return buildCategoryTree(categories);
    }

    private List<CategoryVO> buildCategoryTree(List<ProductCategory> categories) {
        Map<Long, List<ProductCategory>> childrenMap = categories.stream()
                .filter(category -> category.getParentId() != null && !ROOT_PARENT_ID.equals(category.getParentId()))
                .collect(Collectors.groupingBy(ProductCategory::getParentId));
        return categories.stream()
                .filter(category -> category.getParentId() == null || ROOT_PARENT_ID.equals(category.getParentId()))
                .map(root -> toCategoryVO(root, childrenMap))
                .toList();
    }

    private CategoryVO toCategoryVO(ProductCategory category, Map<Long, List<ProductCategory>> childrenMap) {
        List<CategoryVO> children = childrenMap.getOrDefault(category.getId(), List.of()).stream()
                .map(child -> toCategoryVO(child, childrenMap))
                .toList();
        return CategoryVO.builder()
                .id(String.valueOf(category.getId()))
                .name(category.getName())
                .parentId(String.valueOf(category.getParentId() == null ? ROOT_PARENT_ID : category.getParentId()))
                .icon(category.getIcon())
                .sort(category.getSort())
                .status(category.getStatus())
                .children(children)
                .build();
    }

    /* ============================================================
     * API-041 农产品列表
     * ============================================================ */

    /**
     * 公开列表：仅展示已上架商品，支持分类、关键字、价格区间与排序筛选。
     */
    public PageResult<ProductVO> page(ProductPageQuery query) {
        var wrapper = Wrappers.<Product>lambdaQuery()
                .eq(Product::getShelfStatus, SHELF_ON);
        applyFilters(wrapper, query);
        applySort(wrapper, query.getSortBy());
        Page<Product> page = productMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
        return toPageResult(page);
    }

    /**
     * 商家自己的商品列表：包含已下架商品。
     */
    public PageResult<ProductVO> myPage(Long farmerId, ProductPageQuery query) {
        var wrapper = Wrappers.<Product>lambdaQuery()
                .eq(Product::getFarmerId, farmerId);
        applyFilters(wrapper, query);
        applySort(wrapper, query.getSortBy());
        Page<Product> page = productMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
        return toPageResult(page);
    }

    /**
     * 管理端商品列表：可查看已下架商品。
     */
    public PageResult<ProductVO> adminPage(ProductPageQuery query) {
        var wrapper = Wrappers.<Product>lambdaQuery();
        applyFilters(wrapper, query);
        applySort(wrapper, query.getSortBy());
        Page<Product> page = productMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
        return toPageResult(page);
    }

    private void applyFilters(com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Product> wrapper,
                              ProductPageQuery query) {
        if (query.getCategoryId() != null) {
            List<Long> categoryIds = categoryIdsWithChildren(query.getCategoryId());
            wrapper.in(Product::getCategoryId, categoryIds);
        }
        if (StringUtils.hasText(query.getKeyword())) {
            String keyword = query.getKeyword().trim();
            wrapper.and(w -> w.like(Product::getName, keyword).or().like(Product::getOrigin, keyword));
        }
        if (query.getMinPrice() != null) {
            wrapper.ge(Product::getPrice, query.getMinPrice());
        }
        if (query.getMaxPrice() != null) {
            wrapper.le(Product::getPrice, query.getMaxPrice());
        }
        if (query.getIsPoverty() != null) {
            wrapper.eq(Product::getIsPoverty, query.getIsPoverty());
        }
    }

    /**
     * 传入父分类时同时包含其子分类下的商品。
     */
    private List<Long> categoryIdsWithChildren(Long categoryId) {
        List<Long> ids = new ArrayList<>();
        ids.add(categoryId);
        List<ProductCategory> children = categoryMapper.selectList(
                Wrappers.<ProductCategory>lambdaQuery().eq(ProductCategory::getParentId, categoryId));
        children.forEach(child -> ids.add(child.getId()));
        return ids;
    }

    private void applySort(com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Product> wrapper,
                           String sortBy) {
        String sort = StringUtils.hasText(sortBy) ? sortBy.trim() : "new";
        switch (sort) {
            case "price_asc" -> wrapper.orderByAsc(Product::getPrice).orderByDesc(Product::getId);
            case "price_desc" -> wrapper.orderByDesc(Product::getPrice).orderByDesc(Product::getId);
            case "sale_desc" -> wrapper.orderByDesc(Product::getSaleCount).orderByDesc(Product::getId);
            default -> wrapper.orderByDesc(Product::getCreateTime).orderByDesc(Product::getId);
        }
    }

    /* ============================================================
     * API-042 农产品详情
     * ============================================================ */

    /**
     * 公开详情：返回图集、详情与委托代销关系 ID。
     */
    public ProductDetailVO detail(Long id) {
        Product product = requireProduct(id);
        return toDetailVO(product);
    }

    /**
     * 商家查看自己的商品详情（含已下架）。
     */
    public ProductDetailVO myDetail(Long farmerId, Long id) {
        Product product = requireOwned(farmerId, id);
        return toDetailVO(product);
    }

    /* ============================================================
     * API-043 ~ API-046 商家商品维护
     * ============================================================ */

    /**
     * API-043 上架农产品，商品归属当前商家。
     */
    @Transactional
    public ProductVO create(Long farmerId, ProductForm form) {
        requireNotBlank(form.getName(), "商品名称不能为空");
        requireNotBlank(form.getCover(), "封面图不能为空");
        requireNotBlank(form.getUnit(), "计量单位不能为空");
        if (form.getCategoryId() == null) {
            throw new BusinessException("商品分类不能为空");
        }
        if (form.getPrice() == null || form.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("售价必须大于 0");
        }
        if (form.getStock() == null) {
            throw new BusinessException("库存不能为空");
        }
        requireEnabledCategory(form.getCategoryId());
        validatePrice(form.getPrice(), form.getOriginalPrice());

        Product product = new Product();
        product.setFarmerId(farmerId);
        product.setCategoryId(form.getCategoryId());
        product.setName(form.getName().trim());
        product.setCover(form.getCover().trim());
        product.setImages(joinImages(form.getImages()));
        product.setPrice(form.getPrice());
        product.setOriginalPrice(form.getOriginalPrice());
        product.setStock(form.getStock());
        product.setUnit(form.getUnit().trim());
        product.setWeight(form.getWeight());
        product.setOrigin(form.getOrigin());
        product.setIsPoverty(form.getIsPoverty() == null ? 0 : form.getIsPoverty());
        product.setDescription(form.getDescription());
        product.setSaleCount(0);
        product.setShelfStatus(form.getShelfStatus() == null ? SHELF_ON : form.getShelfStatus());
        productMapper.insert(product);
        // 回查一次以获取数据库生成的 create_time
        Product saved = productMapper.selectById(product.getId());
        return toVO(saved == null ? product : saved);
    }

    /**
     * API-044 编辑农产品，仅更新请求体中提供的字段。
     */
    @Transactional
    public ProductVO update(Long farmerId, Long id, ProductForm form) {
        Product product = requireOwned(farmerId, id);
        if (form.getCategoryId() != null) {
            requireEnabledCategory(form.getCategoryId());
            product.setCategoryId(form.getCategoryId());
        }
        if (StringUtils.hasText(form.getName())) {
            product.setName(form.getName().trim());
        }
        if (StringUtils.hasText(form.getCover())) {
            product.setCover(form.getCover().trim());
        }
        if (form.getImages() != null) {
            product.setImages(joinImages(form.getImages()));
        }
        if (form.getPrice() != null) {
            product.setPrice(form.getPrice());
        }
        if (form.getOriginalPrice() != null) {
            product.setOriginalPrice(form.getOriginalPrice());
        }
        if (form.getStock() != null) {
            product.setStock(form.getStock());
        }
        if (StringUtils.hasText(form.getUnit())) {
            product.setUnit(form.getUnit().trim());
        }
        if (form.getWeight() != null) {
            product.setWeight(form.getWeight());
        }
        if (form.getOrigin() != null) {
            product.setOrigin(form.getOrigin());
        }
        if (form.getIsPoverty() != null) {
            product.setIsPoverty(form.getIsPoverty());
        }
        if (form.getDescription() != null) {
            product.setDescription(form.getDescription());
        }
        validatePrice(product.getPrice(), product.getOriginalPrice());
        productMapper.updateById(product);
        return toVO(product);
    }

    /**
     * API-045 商品上下架，下架不删除记录，历史订单仍可关联。
     */
    @Transactional
    public ProductVO shelf(Long farmerId, Long id, ProductShelfForm form) {
        Integer target = form.getShelfStatus();
        if (target == null || (target != SHELF_OFF && target != SHELF_ON)) {
            throw new BusinessException("目标状态非法，仅支持 0 下架 / 1 上架");
        }
        Product product = requireOwned(farmerId, id);
        product.setShelfStatus(target);
        productMapper.updateById(product);
        return toVO(product);
    }

    /**
     * API-046 删除农产品；已产生订单明细的商品只允许下架。
     */
    @Transactional
    public void delete(Long farmerId, Long id) {
        Product product = requireOwned(farmerId, id);
        if (referenceMapper.countOrderItems(id) > 0) {
            throw new BusinessException("该商品已产生订单记录，只能下架不能删除");
        }
        productMapper.deleteById(id);
    }

    /* ============================================================
     * 分类维护（管理端配套）
     * ============================================================ */

    /**
     * 新增分类。
     */
    @Transactional
    public CategoryVO createCategory(CategoryForm form) {
        requireNotBlank(form.getName(), "分类名称不能为空");
        ProductCategory category = new ProductCategory();
        category.setName(form.getName().trim());
        applyCategoryForm(category, form);
        if (category.getParentId() == null) {
            category.setParentId(ROOT_PARENT_ID);
        }
        requireValidParent(category.getParentId(), null);
        if (category.getSort() == null) {
            category.setSort(0);
        }
        if (category.getStatus() == null) {
            category.setStatus(CATEGORY_ENABLED);
        }
        categoryMapper.insert(category);
        return toCategoryVO(category, Map.of());
    }

    /**
     * 编辑分类。
     */
    @Transactional
    public CategoryVO updateCategory(Long id, CategoryForm form) {
        ProductCategory category = categoryMapper.selectById(id);
        if (category == null) {
            throw new BusinessException("分类不存在");
        }
        if (StringUtils.hasText(form.getName())) {
            category.setName(form.getName().trim());
        }
        applyCategoryForm(category, form);
        requireValidParent(category.getParentId(), id);
        categoryMapper.updateById(category);
        return toCategoryVO(category, Map.of());
    }

    /**
     * 校验父分类：只能是已存在的一级分类，且不能把自己挂到自己下面。
     */
    private void requireValidParent(Long parentId, Long selfId) {
        if (parentId == null || ROOT_PARENT_ID.equals(parentId)) {
            return;
        }
        if (parentId.equals(selfId)) {
            throw new BusinessException("不能将分类挂到自身之下");
        }
        ProductCategory parent = categoryMapper.selectById(parentId);
        if (parent == null) {
            throw new BusinessException("父级分类不存在");
        }
        if (parent.getParentId() != null && !ROOT_PARENT_ID.equals(parent.getParentId())) {
            throw new BusinessException("最多支持两级分类，请选择一级分类作为父级");
        }
    }

    /**
     * 删除分类：分类下仍有商品时不允许删除。
     */
    @Transactional
    public void deleteCategory(Long id) {
        ProductCategory category = categoryMapper.selectById(id);
        if (category == null) {
            throw new BusinessException("分类不存在");
        }
        long productCount = productMapper.selectCount(
                Wrappers.<Product>lambdaQuery().eq(Product::getCategoryId, id));
        if (productCount > 0) {
            throw new BusinessException("该分类下仍有商品，无法删除");
        }
        long childCount = categoryMapper.selectCount(
                Wrappers.<ProductCategory>lambdaQuery().eq(ProductCategory::getParentId, id));
        if (childCount > 0) {
            throw new BusinessException("该分类下仍有子分类，无法删除");
        }
        categoryMapper.deleteById(id);
    }

    private void applyCategoryForm(ProductCategory category, CategoryForm form) {
        if (form.getParentId() != null) {
            category.setParentId(form.getParentId());
        }
        if (form.getIcon() != null) {
            category.setIcon(form.getIcon());
        }
        if (form.getSort() != null) {
            category.setSort(form.getSort());
        }
        if (form.getStatus() != null) {
            category.setStatus(form.getStatus());
        }
    }

    /* ============================================================
     * 内部辅助
     * ============================================================ */

    private Product requireProduct(Long id) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new BusinessException("商品不存在或已下架");
        }
        return product;
    }

    private Product requireOwned(Long farmerId, Long id) {
        Product product = requireProduct(id);
        if (!farmerId.equals(product.getFarmerId())) {
            throw new BusinessException("无权操作他人的商品");
        }
        return product;
    }

    private void requireEnabledCategory(Long categoryId) {
        ProductCategory category = categoryMapper.selectById(categoryId);
        if (category == null) {
            throw new BusinessException("商品分类不存在");
        }
        if (!Integer.valueOf(CATEGORY_ENABLED).equals(category.getStatus())) {
            throw new BusinessException("商品分类已禁用，请选择其他分类");
        }
    }

    private void validatePrice(BigDecimal price, BigDecimal originalPrice) {
        if (price == null || originalPrice == null) {
            return;
        }
        if (originalPrice.compareTo(price) < 0) {
            throw new BusinessException("原价不能低于售价");
        }
    }

    private PageResult<ProductVO> toPageResult(Page<Product> page) {
        List<Product> records = page.getRecords();
        Map<Long, String> categoryNames = categoryNames(
                records.stream().map(Product::getCategoryId).filter(Objects::nonNull).distinct().toList());
        Map<Long, FarmerInfo> farmerInfos = farmerInfos(
                records.stream().map(Product::getFarmerId).filter(Objects::nonNull).distinct().toList());
        List<ProductVO> items = records.stream()
                .map(product -> toVO(product, categoryNames, farmerInfos))
                .toList();
        return new PageResult<>(items, page.getTotal());
    }

    private ProductVO toVO(Product product) {
        return toVO(product, categoryNames(List.of(product.getCategoryId())),
                farmerInfos(List.of(product.getFarmerId())));
    }

    private ProductVO toVO(Product product, Map<Long, String> categoryNames, Map<Long, FarmerInfo> farmerInfos) {
        FarmerInfo info = farmerInfos.get(product.getFarmerId());
        return ProductVO.builder()
                .id(String.valueOf(product.getId()))
                .farmerId(product.getFarmerId() == null ? null : String.valueOf(product.getFarmerId()))
                .farmerName(info == null ? null : info.farmerName())
                .shopName(info == null ? null : info.shopName())
                .categoryId(product.getCategoryId() == null ? null : String.valueOf(product.getCategoryId()))
                .categoryName(categoryNames.get(product.getCategoryId()))
                .name(product.getName())
                .cover(product.getCover())
                .price(product.getPrice())
                .originalPrice(product.getOriginalPrice())
                .stock(product.getStock())
                .unit(product.getUnit())
                .weight(product.getWeight())
                .origin(product.getOrigin())
                .isPoverty(product.getIsPoverty())
                .saleCount(product.getSaleCount())
                .shelfStatus(product.getShelfStatus())
                .createTime(product.getCreateTime())
                .build();
    }

    private ProductDetailVO toDetailVO(Product product) {
        ProductVO vo = toVO(product);
        Long entrustId = referenceMapper.findActiveEntrustId(product.getId());
        return ProductDetailVO.builder()
                .id(vo.getId())
                .farmerId(vo.getFarmerId())
                .farmerName(vo.getFarmerName())
                .shopName(vo.getShopName())
                .categoryId(vo.getCategoryId())
                .categoryName(vo.getCategoryName())
                .name(vo.getName())
                .cover(vo.getCover())
                .price(vo.getPrice())
                .originalPrice(vo.getOriginalPrice())
                .stock(vo.getStock())
                .unit(vo.getUnit())
                .weight(vo.getWeight())
                .origin(vo.getOrigin())
                .isPoverty(vo.getIsPoverty())
                .saleCount(vo.getSaleCount())
                .shelfStatus(vo.getShelfStatus())
                .createTime(vo.getCreateTime() == null ? null : vo.getCreateTime().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                .images(splitImages(product.getImages()))
                .description(product.getDescription())
                .entrustId(entrustId == null ? null : String.valueOf(entrustId))
                .build();
    }

    private Map<Long, String> categoryNames(List<Long> categoryIds) {
        List<Long> ids = categoryIds == null ? List.of() : categoryIds.stream().filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return categoryMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(ProductCategory::getId, ProductCategory::getName, (a, b) -> a));
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
