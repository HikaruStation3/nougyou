<template>
  <div class="detail-page">
    <div class="back-bar">
      <el-button text @click="router.back()">
        <el-icon><ArrowLeft /></el-icon> 返回商城
      </el-button>
    </div>

    <el-skeleton v-if="loading" :rows="6" animated />

    <template v-else-if="product">
      <el-card shadow="never" class="detail-card">
        <div class="detail-main">
          <div class="detail-cover">
            <el-image
              v-if="product.cover"
              :src="product.cover"
              :preview-src-list="galleryList"
              fit="cover"
              class="cover-img"
              preview-teleported
            />
            <div v-else class="cover-empty">暂无主图</div>
          </div>

          <div class="detail-info">
            <div class="title-row">
              <h1>{{ product.name }}</h1>
              <el-tag v-if="product.isPoverty === 1" type="danger" effect="dark"
                >扶贫产品</el-tag
              >
            </div>

            <el-alert
              v-if="offShelf"
              title="该商品已下架，暂时无法购买"
              type="warning"
              :closable="false"
              show-icon
              class="off-shelf-alert"
            />

            <div class="price-row">
              <span class="price">￥{{ money(product.price) }}</span>
              <span class="unit">/ {{ product.unit || "件" }}</span>
              <span v-if="product.originalPrice" class="origin-price"
                >原价 ￥{{ money(product.originalPrice) }}</span
              >
            </div>

            <el-descriptions :column="1" border class="info-desc">
              <el-descriptions-item label="商家">
                {{ product.shopName || product.farmerName || "-" }}
              </el-descriptions-item>
              <el-descriptions-item label="产地">{{
                product.origin || "-"
              }}</el-descriptions-item>
              <el-descriptions-item label="分类">
                {{ product.categoryName || "-" }}
              </el-descriptions-item>
              <el-descriptions-item label="库存">
                {{ product.stock ?? 0 }} {{ product.unit || "件" }}
              </el-descriptions-item>
              <el-descriptions-item label="销量">{{
                product.saleCount || 0
              }}</el-descriptions-item>
            </el-descriptions>

            <div class="action-row">
              <span class="action-label">购买数量</span>
              <el-input-number
                v-model="quantity"
                :min="1"
                :max="maxQuantity"
                style="width: 150px"
              />
              <el-button
                type="primary"
                :icon="ShoppingCart"
                :loading="adding"
                :disabled="maxQuantity < 1 || offShelf"
                @click="handleAddToCart"
              >
                加入购物车
              </el-button>
              <el-button
                type="warning"
                plain
                :disabled="maxQuantity < 1 || offShelf"
                @click="handleBuyNow"
              >
                立即购买
              </el-button>
            </div>
            <div v-if="offShelf" class="stock-warn">
              该商品已下架，恢复上架后即可购买
            </div>
            <div v-else-if="maxQuantity < 1" class="stock-warn">
              商品库存不足，暂时无法购买
            </div>
          </div>
        </div>

        <el-card v-if="product.description" shadow="never" class="intro-card">
          <template #header>商品介绍</template>
          <div class="intro-text">{{ product.description }}</div>
        </el-card>

        <el-card v-if="galleryList.length" shadow="never" class="gallery-card">
          <template #header>商品图集</template>
          <div class="gallery-grid">
            <el-image
              v-for="(img, idx) in galleryList"
              :key="idx"
              :src="img"
              :preview-src-list="galleryList"
              :initial-index="idx"
              fit="cover"
              class="gallery-img"
              preview-teleported
            />
          </div>
        </el-card>
      </el-card>
    </template>

    <el-empty v-else description="商品不存在或已下架">
      <el-button type="primary" @click="router.push('/products')"
        >返回商城</el-button
      >
    </el-empty>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { ArrowLeft, ShoppingCart } from "@element-plus/icons-vue";
import { getProduct } from "../api/product";
import { addToCart } from "../api/cart";
import { notifyCartChanged } from "../utils/cartBus";
import { money } from "../utils/format";
import { useUserStore } from "../store/user";

const route = useRoute();
const router = useRouter();
const store = useUserStore();

const loading = ref(false);
const adding = ref(false);
const product = ref(null);
const quantity = ref(1);

const galleryList = computed(() => {
  if (!product.value) return [];
  return [product.value.cover, ...(product.value.images || [])].filter(Boolean);
});

const maxQuantity = computed(() => {
  if (!product.value) return 0;
  return Math.max(product.value.stock || 0, 0);
});

/** 商品被商家下架后，详情页仍可访问，但禁止购买 */
const offShelf = computed(() => product.value?.shelfStatus === 0);

async function loadDetail() {
  loading.value = true;
  try {
    product.value = await getProduct(route.params.id);
    quantity.value = 1;
  } catch {
    product.value = null;
  } finally {
    loading.value = false;
  }
}

function requireLogin() {
  if (store.isLoggedIn()) return true;
  ElMessage.warning("请先登录后再进行购买操作");
  router.push({
    name: "login",
    query: { redirect: `/products/${route.params.id}` },
  });
  return false;
}

async function handleAddToCart() {
  if (offShelf.value) {
    ElMessage.warning("该商品已下架，暂时无法购买");
    return;
  }
  if (!requireLogin()) return;
  adding.value = true;
  try {
    await addToCart({ productId: product.value.id, quantity: quantity.value });
    notifyCartChanged();
    ElMessage.success("已加入购物车");
  } finally {
    adding.value = false;
  }
}

async function handleBuyNow() {
  if (offShelf.value) {
    ElMessage.warning("该商品已下架，暂时无法购买");
    return;
  }
  if (!requireLogin()) return;
  router.push({
    path: "/checkout",
    query: { productId: product.value.id, quantity: quantity.value },
  });
}

onMounted(loadDetail);
</script>

<style scoped>
.off-shelf-alert {
  margin-bottom: 14px;
}

.detail-page {
  max-width: 1000px;
  margin: 0 auto;
  padding: 24px 20px 48px;
}

.back-bar {
  margin-bottom: 12px;
}

.detail-card {
  border-radius: 12px;
}

.detail-main {
  display: flex;
  gap: 28px;
  flex-wrap: wrap;
}

.detail-cover {
  width: 420px;
  max-width: 100%;
  height: 420px;
  background: #eef3f0;
  border-radius: 12px;
  overflow: hidden;
}

.cover-img {
  width: 100%;
  height: 100%;
}

.cover-empty {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #8a9d92;
}

.detail-info {
  flex: 1;
  min-width: 320px;
}

.title-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.title-row h1 {
  margin: 0 0 12px;
  font-size: 24px;
  color: #1f3d2b;
}

.price-row {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin-bottom: 16px;
}

.price {
  font-size: 28px;
  font-weight: 700;
  color: #e05b4c;
}

.unit {
  font-size: 13px;
  color: #8a9d92;
}

.origin-price {
  font-size: 13px;
  color: #b0b8b3;
  text-decoration: line-through;
}

.info-desc :deep(.el-descriptions__label) {
  width: 90px;
  color: #5d7668;
}

.action-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 20px;
  flex-wrap: wrap;
}

.action-label {
  font-size: 14px;
  color: #5d7668;
}

.stock-warn {
  margin-top: 10px;
  font-size: 13px;
  color: #f56c6c;
}

.intro-card,
.gallery-card {
  margin-top: 20px;
  border-radius: 12px;
}

.intro-text {
  white-space: pre-wrap;
  line-height: 1.8;
  color: #3a4a42;
}

.gallery-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 12px;
}

.gallery-img {
  width: 100%;
  height: 140px;
  border-radius: 8px;
  cursor: pointer;
}
</style>
