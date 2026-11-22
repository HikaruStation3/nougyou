<template>
  <div class="cart-page">
    <section class="page-hero">
      <h1>我的购物车</h1>
      <p>确认商品与数量后即可提交订单，支持按店铺分别结算。</p>
    </section>

    <el-card v-loading="loading" shadow="never" class="cart-card">
      <template v-if="groups.length">
        <div class="cart-toolbar">
          <el-checkbox
            :model-value="allSelected"
            :indeterminate="someSelected"
            @change="handleToggleAll"
          >
            全选
          </el-checkbox>
          <div class="toolbar-actions">
            <el-button
              type="danger"
              plain
              size="small"
              @click="handleBatchRemove"
              >删除选中</el-button
            >
            <el-button type="danger" plain size="small" @click="handleClear"
              >清空购物车</el-button
            >
          </div>
        </div>

        <div v-for="group in groups" :key="group.key" class="shop-group">
          <div class="shop-head">
            <el-icon><Shop /></el-icon>
            <span class="shop-name">{{ group.shopName }}</span>
            <span class="shop-sub">{{ group.farmerName || "钟山镇农户" }}</span>
            <span class="shop-subtotal">
              小计：￥{{ group.subtotal.toFixed(2) }}
            </span>
          </div>

          <el-table :data="group.items" border>
            <el-table-column width="60" align="center">
              <template #default="{ row }">
                <el-checkbox
                  :model-value="row.selected === 1"
                  :disabled="row.invalid"
                  @change="(val) => handleSelect(row, val)"
                />
              </template>
            </el-table-column>
            <el-table-column label="商品" min-width="280">
              <template #default="{ row }">
                <div class="goods-cell">
                  <el-image
                    v-if="row.cover"
                    :src="row.cover"
                    fit="cover"
                    class="goods-thumb"
                    preview-teleported
                    :preview-src-list="[row.cover]"
                  />
                  <div class="goods-info">
                    <div class="goods-name">{{ row.productName }}</div>
                    <div v-if="row.invalid" class="goods-invalid">
                      商品已下架或库存不足，请调整数量或删除
                    </div>
                  </div>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="单价" width="110" align="right">
              <template #default="{ row }">￥{{ money(row.price) }}</template>
            </el-table-column>
            <el-table-column label="数量" width="150" align="center">
              <template #default="{ row }">
                <el-input-number
                  :model-value="row.quantity"
                  :min="1"
                  :max="row.stock || 1"
                  size="small"
                  @change="(val) => handleQuantityChange(row, val)"
                />
              </template>
            </el-table-column>
            <el-table-column label="小计" width="120" align="right">
              <template #default="{ row }">
                <span class="subtotal"
                  >￥{{ (row.subtotal ?? 0).toFixed(2) }}</span
                >
              </template>
            </el-table-column>
            <el-table-column label="操作" width="90" align="center">
              <template #default="{ row }">
                <el-button type="danger" link @click="handleRemove(row)"
                  >删除</el-button
                >
              </template>
            </el-table-column>
          </el-table>
        </div>

        <div class="cart-footer">
          <div class="footer-tip">已选 {{ selectedCount }} 件商品</div>
          <div class="footer-total">
            合计：
            <span class="total-price">￥{{ totalAmount.toFixed(2) }}</span>
          </div>
          <el-button
            type="primary"
            size="large"
            :disabled="selectedCount === 0"
            @click="handleCheckout"
          >
            去结算
          </el-button>
        </div>
      </template>

      <el-empty
        v-else-if="!loading"
        :description="
          isFarmer ? '当前账号为农户商家，购物车仅对买家开放' : '购物车还是空的'
        "
      >
        <el-button
          type="primary"
          @click="router.push(isFarmer ? '/merchant/products' : '/products')"
          >{{ isFarmer ? "去管理我的农产品" : "去逛逛商城" }}</el-button
        >
      </el-empty>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { ElMessage, ElMessageBox } from "element-plus";
import { Shop } from "@element-plus/icons-vue";
import {
  getCart,
  removeCartItem,
  removeCartItems,
  updateCartItem,
} from "../api/cart";
import { notifyCartChanged } from "../utils/cartBus";
import { money } from "../utils/format";
import { useUserStore } from "../store/user";

const router = useRouter();
const store = useUserStore();

const loading = ref(false);
const cartList = ref([]);

const groups = computed(() => {
  const map = new Map();
  cartList.value.forEach((item) => {
    const key = item.farmerId || "unknown";
    if (!map.has(key)) {
      map.set(key, {
        key,
        farmerId: item.farmerId,
        farmerName: item.farmerName,
        shopName: item.shopName || item.farmerName || "钟山镇农户",
        items: [],
        subtotal: 0,
      });
    }
    const group = map.get(key);
    group.items.push(item);
    if (!item.invalid && item.selected === 1) {
      group.subtotal += Number(item.subtotal || 0);
    }
  });
  return Array.from(map.values());
});

const selectedItems = computed(() =>
  cartList.value.filter((item) => item.selected === 1 && !item.invalid),
);

const selectedCount = computed(() =>
  selectedItems.value.reduce((sum, item) => sum + (item.quantity || 0), 0),
);

const totalAmount = computed(() =>
  selectedItems.value.reduce(
    (sum, item) => sum + Number(item.subtotal || 0),
    0,
  ),
);

const allSelected = computed(
  () =>
    cartList.value.length > 0 &&
    cartList.value.every((item) => item.selected === 1),
);

const someSelected = computed(
  () =>
    !allSelected.value && cartList.value.some((item) => item.selected === 1),
);

/** 农户商家没有购物车权限，直接展示提示，避免无谓的 403 请求 */
const isFarmer = computed(() =>
  (store.state.user?.roles || []).includes("FARMER"),
);

async function loadData() {
  if (isFarmer.value) {
    cartList.value = [];
    return;
  }
  loading.value = true;
  try {
    cartList.value = (await getCart()) || [];
  } catch {
    cartList.value = [];
  } finally {
    loading.value = false;
  }
}

async function handleSelect(row, checked) {
  await updateCartItem(row.id, { selected: checked ? 1 : 0 });
  await loadData();
}

async function handleToggleAll(checked) {
  const ids = cartList.value
    .filter((item) => !item.invalid)
    .map((item) => item.id);
  for (const id of ids) {
    await updateCartItem(id, { selected: checked ? 1 : 0 });
  }
  await loadData();
}

async function handleQuantityChange(row, value) {
  const quantity = Number(value);
  if (!quantity || quantity < 1) return;
  if (row.stock && quantity > row.stock) {
    ElMessage.warning(`该商品库存仅剩 ${row.stock} ${row.unit || "件"}`);
    await loadData();
    return;
  }
  await updateCartItem(row.id, { quantity });
  notifyCartChanged();
  await loadData();
}

async function handleRemove(row) {
  try {
    await ElMessageBox.confirm(
      `确定将「${row.productName}」移出购物车吗？`,
      "提示",
      {
        type: "warning",
      },
    );
  } catch {
    return;
  }
  await removeCartItem(row.id);
  notifyCartChanged();
  ElMessage.success("已删除");
  await loadData();
}

async function handleBatchRemove() {
  const ids = selectedItems.value.map((item) => item.id);
  if (!ids.length) {
    ElMessage.warning("请先勾选要删除的商品");
    return;
  }
  try {
    await ElMessageBox.confirm(
      `确定删除选中的 ${ids.length} 件商品吗？`,
      "提示",
      {
        type: "warning",
      },
    );
  } catch {
    return;
  }
  await removeCartItems(ids);
  notifyCartChanged();
  ElMessage.success("已删除选中商品");
  await loadData();
}

async function handleClear() {
  if (!cartList.value.length) return;
  try {
    await ElMessageBox.confirm("确定清空购物车吗？", "提示", {
      type: "warning",
    });
  } catch {
    return;
  }
  await removeCartItems([]);
  notifyCartChanged();
  ElMessage.success("购物车已清空");
  await loadData();
}

function handleCheckout() {
  ElMessage.info("订单模块正在接入，请稍后再试");
}

onMounted(loadData);
</script>

<style scoped>
.cart-page {
  max-width: 1100px;
  margin: 0 auto;
  padding: 32px 20px 48px;
}

.page-hero {
  margin-bottom: 20px;
}

.page-hero h1 {
  margin: 0 0 8px;
  font-size: 26px;
  color: #1f3d2b;
}

.page-hero p {
  margin: 0;
  color: #5d7668;
}

.cart-card {
  border-radius: 12px;
}

.cart-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--el-border-color-lighter);
  margin-bottom: 16px;
}

.toolbar-actions {
  display: flex;
  gap: 8px;
}

.shop-group {
  margin-bottom: 20px;
}

.shop-head {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  background: #f5f8f6;
  border-radius: 8px 8px 0 0;
  color: #1f3d2b;
}

.shop-name {
  font-weight: 600;
}

.shop-sub {
  font-size: 13px;
  color: #8a9d92;
}

.shop-subtotal {
  margin-left: auto;
  font-size: 13px;
  color: #e05b4c;
  font-weight: 600;
}

.goods-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.goods-thumb {
  width: 52px;
  height: 52px;
  border-radius: 6px;
  flex-shrink: 0;
}

.goods-name {
  font-weight: 500;
  color: #1f3d2b;
}

.goods-invalid {
  margin-top: 4px;
  font-size: 12px;
  color: #f56c6c;
}

.subtotal {
  color: #e05b4c;
  font-weight: 600;
}

.cart-footer {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 20px;
  padding-top: 16px;
  border-top: 1px solid var(--el-border-color-lighter);
}

.footer-tip {
  font-size: 13px;
  color: #8a9d92;
}

.footer-total {
  font-size: 14px;
  color: #5d7668;
}

.total-price {
  font-size: 24px;
  font-weight: 700;
  color: #e05b4c;
}
</style>
