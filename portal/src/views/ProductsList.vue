<template>
  <div class="products-page">
    <section class="page-hero">
      <h1>农产品商城</h1>
      <p>
        选购钟山镇本地出产的优质农产品，支持农户商家直接发货，扶贫助农每一笔。
      </p>
    </section>

    <div class="content-wrap">
      <aside class="category-side">
        <el-card shadow="never" class="category-card">
          <div class="side-title">商品分类</div>
          <el-menu
            :default-active="String(query.categoryId || '')"
            class="category-menu"
            @select="handleCategorySelect"
          >
            <el-menu-item index="">全部分类</el-menu-item>
            <template v-for="item in visibleCategories" :key="item.id">
              <el-menu-item :index="item.id">
                <span class="cat-name">{{ item.name }}</span>
                <span class="cat-count">{{ item.productCount }}</span>
              </el-menu-item>
              <el-menu-item
                v-for="child in item.children"
                :key="child.id"
                :index="child.id"
                class="category-sub-item"
              >
                <span class="cat-name">{{ child.name }}</span>
                <span class="cat-count">{{ child.productCount }}</span>
              </el-menu-item>
            </template>
          </el-menu>
        </el-card>
      </aside>

      <section class="product-main">
        <el-card shadow="never" class="filter-card">
          <el-form inline @submit.prevent>
            <el-form-item label="商品名称">
              <el-input
                v-model="query.keyword"
                placeholder="搜索商品 / 产地"
                clearable
                style="width: 200px"
                @keyup.enter="handleSearch"
              />
            </el-form-item>
            <el-form-item label="价格">
              <el-input-number
                v-model="query.minPrice"
                :min="0"
                :precision="2"
                :controls="false"
                placeholder="最低"
                style="width: 100px"
              />
              <span class="price-sep">-</span>
              <el-input-number
                v-model="query.maxPrice"
                :min="0"
                :precision="2"
                :controls="false"
                placeholder="最高"
                style="width: 100px"
              />
            </el-form-item>
            <el-form-item label="扶贫产品">
              <el-select
                v-model="query.isPoverty"
                placeholder="全部"
                clearable
                style="width: 120px"
              >
                <el-option label="扶贫产品" :value="1" />
                <el-option label="普通商品" :value="0" />
              </el-select>
            </el-form-item>
            <el-form-item label="排序">
              <el-select v-model="query.sortBy" style="width: 140px">
                <el-option label="综合排序" value="new" />
                <el-option label="价格从低到高" value="price_asc" />
                <el-option label="价格从高到低" value="price_desc" />
                <el-option label="销量优先" value="sale_desc" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" icon="search" @click="handleSearch"
                >查询</el-button
              >
              <el-button icon="refresh" @click="handleReset">重置</el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <div v-loading="loading" class="product-grid">
          <el-empty
            v-if="!loading && !list.length"
            description="暂无在售农产品"
          />
          <el-card
            v-for="item in list"
            :key="item.id"
            shadow="hover"
            class="product-card"
            @click="goDetail(item.id)"
          >
            <div class="product-cover">
              <el-image
                v-if="item.cover"
                :src="item.cover"
                fit="cover"
                class="cover-img"
              >
                <template #error>
                  <div class="cover-placeholder">暂无封面</div>
                </template>
              </el-image>
              <div v-else class="cover-placeholder">暂无封面</div>
              <el-tag
                v-if="item.isPoverty === 1"
                class="poverty-tag"
                type="danger"
                effect="dark"
                round
              >
                扶贫产品
              </el-tag>
            </div>
            <div class="product-body">
              <h3 class="product-title">{{ item.name }}</h3>
              <div class="product-meta">
                <span class="meta-item">
                  <el-icon><Shop /></el-icon
                  >{{ item.shopName || item.farmerName || "钟山镇农户" }}
                </span>
                <span class="meta-item">
                  <el-icon><LocationFilled /></el-icon
                  >{{ item.origin || "产地待完善" }}
                </span>
              </div>
              <div class="product-footer">
                <div class="price-box">
                  <span class="price">￥{{ money(item.price) }}</span>
                  <span class="unit">/ {{ item.unit || "件" }}</span>
                  <span v-if="item.originalPrice" class="origin-price"
                    >￥{{ money(item.originalPrice) }}</span
                  >
                </div>
                <el-button
                  type="primary"
                  size="small"
                  round
                  @click.stop="handleAddToCart(item)"
                >
                  加入购物车
                </el-button>
              </div>
              <div class="stock-line">
                库存 {{ item.stock ?? 0 }} {{ item.unit || "件" }} · 已售
                {{ item.saleCount || 0 }}
              </div>
            </div>
          </el-card>
        </div>

        <div class="pagination-wrap">
          <el-pagination
            v-model:current-page="query.pageNum"
            v-model:page-size="query.pageSize"
            :total="total"
            :page-sizes="[9, 12, 24]"
            layout="total, sizes, prev, pager, next"
            background
            @size-change="loadData"
            @current-change="loadData"
          />
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { LocationFilled, Shop } from "@element-plus/icons-vue";
import { getCategories, getProducts } from "../api/product";
import { addToCart } from "../api/cart";
import { notifyCartChanged } from "../utils/cartBus";
import { money } from "../utils/format";
import { useUserStore } from "../store/user";

const router = useRouter();
const store = useUserStore();

const loading = ref(false);
const list = ref([]);
const total = ref(0);
const categories = ref([]);

/**
 * 只展示有在售商品的分类：二级分类为空会让买家点进去必然空列表，
 * productCount 由后端按已上架商品统计，一级分类已包含子分类数量。
 */
const visibleCategories = computed(() =>
  categories.value
    .map((item) => ({
      ...item,
      children: (item.children || []).filter(
        (child) => (child.productCount || 0) > 0
      ),
    }))
    .filter((item) => (item.productCount || 0) > 0)
);

const query = reactive({
  pageNum: 1,
  pageSize: 9,
  categoryId: "",
  keyword: "",
  minPrice: undefined,
  maxPrice: undefined,
  isPoverty: undefined,
  sortBy: "new",
});

async function loadCategories() {
  try {
    categories.value = await getCategories({ onlyEnabled: true });
  } catch {
    categories.value = [];
  }
}

async function loadData() {
  loading.value = true;
  try {
    const params = { ...query };
    if (!params.categoryId) delete params.categoryId;
    if (params.minPrice === undefined) delete params.minPrice;
    if (params.maxPrice === undefined) delete params.maxPrice;
    if (params.isPoverty === undefined) delete params.isPoverty;
    const page = await getProducts(params);
    list.value = page.list || [];
    total.value = page.total || 0;
  } catch {
    list.value = [];
    total.value = 0;
  } finally {
    loading.value = false;
  }
}

function handleCategorySelect(categoryId) {
  query.categoryId = categoryId || "";
  query.pageNum = 1;
  loadData();
}

function handleSearch() {
  query.pageNum = 1;
  loadData();
}

function handleReset() {
  query.categoryId = "";
  query.keyword = "";
  query.minPrice = undefined;
  query.maxPrice = undefined;
  query.isPoverty = undefined;
  query.sortBy = "new";
  query.pageNum = 1;
  loadData();
}

async function handleAddToCart(item) {
  if (!store.isLoggedIn()) {
    ElMessage.warning("请先登录后再进行购买操作");
    router.push({ name: "login", query: { redirect: "/products" } });
    return;
  }
  if (!item.stock) {
    ElMessage.warning("该商品暂时缺货");
    return;
  }
  await addToCart({ productId: item.id, quantity: 1 });
  notifyCartChanged();
  ElMessage.success("已加入购物车");
}

function goDetail(id) {
  router.push(`/products/${id}`);
}

onMounted(() => {
  loadCategories();
  loadData();
});
</script>

<style scoped>
.products-page {
  max-width: 1180px;
  margin: 0 auto;
  padding: 32px 20px 48px;
}

.page-hero {
  text-align: center;
  margin-bottom: 24px;
}

.page-hero h1 {
  margin: 0 0 10px;
  font-size: 30px;
  color: #1f3d2b;
}

.page-hero p {
  margin: 0;
  color: #5d7668;
}

.content-wrap {
  display: flex;
  gap: 20px;
  align-items: flex-start;
}

.category-side {
  width: 190px;
  flex-shrink: 0;
}

.category-card {
  border-radius: 12px;
}

.side-title {
  font-weight: 600;
  color: #1f3d2b;
  padding: 4px 12px 10px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.category-menu {
  border-right: none;
}

/* 二级分类：缩进显示，字号略小，与一级分类区分 */
.category-menu .category-sub-item {
  padding-left: 36px !important;
  font-size: 13px;
  color: var(--el-text-color-regular);
}

.cat-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.cat-count {
  margin-left: auto;
  padding-left: 8px;
  font-size: 12px;
  color: #8a9d92;
}

.product-main {
  flex: 1;
  min-width: 0;
}

.filter-card {
  border-radius: 12px;
  margin-bottom: 20px;
}

.price-sep {
  margin: 0 6px;
  color: #8a9d92;
}

.product-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 18px;
  min-height: 200px;
}

.product-card {
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  transition:
    transform 0.2s ease,
    box-shadow 0.2s ease;
}

.product-card:hover {
  transform: translateY(-4px);
}

.product-cover {
  position: relative;
  height: 160px;
  margin: -20px -20px 0;
  background: #eef3f0;
  overflow: hidden;
}

.cover-img {
  width: 100%;
  height: 100%;
}

.cover-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #8a9d92;
  font-size: 13px;
}

.poverty-tag {
  position: absolute;
  left: 10px;
  top: 10px;
}

.product-body {
  padding-top: 14px;
}

.product-title {
  margin: 0 0 8px;
  font-size: 16px;
  color: #1f3d2b;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.product-meta {
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 13px;
  color: #5d7668;
  margin-bottom: 10px;
}

.meta-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.product-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.price-box {
  display: flex;
  align-items: baseline;
  gap: 4px;
}

.price {
  font-size: 18px;
  font-weight: 700;
  color: #e05b4c;
}

.unit {
  font-size: 12px;
  color: #8a9d92;
}

.origin-price {
  font-size: 12px;
  color: #b0b8b3;
  text-decoration: line-through;
}

.stock-line {
  margin-top: 8px;
  font-size: 12px;
  color: #8a9d92;
}

.pagination-wrap {
  display: flex;
  justify-content: center;
  margin-top: 28px;
}
</style>
