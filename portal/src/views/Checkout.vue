<template>
  <div class="checkout-page">
    <section class="page-hero">
      <h1>确认订单</h1>
      <p>核对商品与收货信息后提交，系统会按商家自动拆单。</p>
    </section>

    <el-card v-loading="loading" shadow="never" class="checkout-card">
      <el-empty v-if="!loading && !items.length" description="没有待结算的商品">
        <el-button type="primary" @click="router.push('/products')">去逛逛商城</el-button>
      </el-empty>

      <template v-else>
        <h3 class="section-title">商品清单</h3>
        <el-table :data="items" border>
          <el-table-column label="商品" min-width="280">
            <template #default="{ row }">
              <div class="goods-cell">
                <el-image v-if="row.cover" :src="row.cover" fit="cover" class="goods-thumb" />
                <div>
                  <div class="goods-name">{{ row.productName }}</div>
                  <div class="goods-shop">{{ row.shopName || "钟山镇农户" }}</div>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="单价" width="120" align="right">
            <template #default="{ row }">￥{{ money(row.price) }}</template>
          </el-table-column>
          <el-table-column label="数量" width="100" align="center">
            <template #default="{ row }">x{{ row.quantity }}</template>
          </el-table-column>
          <el-table-column label="小计" width="130" align="right">
            <template #default="{ row }">
              <span class="amount">￥{{ money(row.subtotal) }}</span>
            </template>
          </el-table-column>
        </el-table>

        <h3 class="section-title">收货信息</h3>
        <el-form :model="form" label-width="90px" class="address-form">
          <el-form-item label="收货人" required>
            <el-input v-model="form.receiverName" maxlength="20" placeholder="请输入收货人姓名" />
          </el-form-item>
          <el-form-item label="联系电话" required>
            <el-input v-model="form.receiverPhone" maxlength="11" placeholder="请输入 11 位手机号" />
          </el-form-item>
          <el-form-item label="收货地址" required>
            <el-input
              v-model="form.receiverAddress"
              type="textarea"
              :rows="2"
              maxlength="200"
              show-word-limit
              placeholder="请输入详细收货地址"
            />
          </el-form-item>
          <el-form-item label="买家备注">
            <el-input v-model="form.remark" maxlength="200" placeholder="选填，如发货偏好" />
          </el-form-item>
          <el-form-item label="支付方式">
            <el-radio-group v-model="form.payType">
              <el-radio :value="3">模拟支付</el-radio>
              <el-radio :value="1">微信支付</el-radio>
              <el-radio :value="2">支付宝</el-radio>
            </el-radio-group>
            <div class="form-tip">本项目未接入真实支付，提交后可在订单列表完成模拟支付。</div>
          </el-form-item>
        </el-form>

        <div class="checkout-footer">
          <div class="total">
            应付合计：<span class="total-price">￥{{ money(totalAmount) }}</span>
          </div>
          <el-button type="primary" size="large" :loading="submitting" @click="handleSubmit">
            提交订单
          </el-button>
        </div>
      </template>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { getCart } from "../api/cart";
import { getProduct } from "../api/product";
import { createOrder } from "../api/order";
import { notifyCartChanged } from "../utils/cartBus";
import { money } from "../utils/format";
import { useUserStore } from "../store/user";

const route = useRoute();
const router = useRouter();
const store = useUserStore();

const loading = ref(false);
const submitting = ref(false);
const items = ref([]);

const source = computed(() => (route.query.productId ? "buyNow" : "cart"));
const cartIds = computed(() =>
  String(route.query.cartIds || "")
    .split(",")
    .filter(Boolean),
);

const form = reactive({
  receiverName: store.state.user?.nickname || "",
  receiverPhone: store.state.user?.mobile || "",
  receiverAddress: "",
  remark: "",
  payType: 3,
});

const totalAmount = computed(() =>
  items.value.reduce((sum, item) => sum + Number(item.subtotal || 0), 0),
);

async function loadPreview() {
  loading.value = true;
  try {
    if (source.value === "buyNow") {
      const product = await getProduct(route.query.productId);
      const quantity = Number(route.query.quantity || 1);
      items.value = [
        {
          productId: product.id,
          productName: product.name,
          cover: product.cover,
          shopName: product.shopName,
          price: product.price,
          quantity,
          subtotal: Number(product.price || 0) * quantity,
        },
      ];
      return;
    }
    const cart = (await getCart()) || [];
    const selected = cart.filter((item) => cartIds.value.includes(String(item.id)));
    if (selected.some((item) => item.invalid)) {
      ElMessage.warning("部分商品已下架或库存不足，请返回购物车调整");
    }
    items.value = selected.map((item) => ({
      productId: item.productId,
      productName: item.productName,
      cover: item.cover,
      shopName: item.shopName,
      price: item.price,
      quantity: item.quantity,
      subtotal: item.subtotal,
    }));
  } catch {
    items.value = [];
  } finally {
    loading.value = false;
  }
}

function validate() {
  if (!items.value.length) {
    ElMessage.warning("没有待结算的商品");
    return false;
  }
  if (!form.receiverName.trim() || form.receiverName.trim().length < 2) {
    ElMessage.warning("请填写 2-20 字的收货人姓名");
    return false;
  }
  if (!/^1\d{10}$/.test(form.receiverPhone.trim())) {
    ElMessage.warning("请填写正确的 11 位手机号");
    return false;
  }
  if (!form.receiverAddress.trim()) {
    ElMessage.warning("请填写收货地址");
    return false;
  }
  return true;
}

async function handleSubmit() {
  if (!validate()) return;
  submitting.value = true;
  try {
    const payload = {
      source: source.value,
      receiverName: form.receiverName.trim(),
      receiverPhone: form.receiverPhone.trim(),
      receiverAddress: form.receiverAddress.trim(),
      remark: form.remark.trim() || undefined,
      payType: form.payType,
    };
    if (source.value === "cart") {
      payload.cartIds = cartIds.value.map((id) => Number(id));
    } else {
      payload.productId = Number(route.query.productId);
      payload.quantity = Number(route.query.quantity || 1);
    }
    const orders = await createOrder(payload);
    notifyCartChanged();
    ElMessage.success(`已提交 ${orders?.length || 1} 个订单，请完成支付`);
    router.replace({ path: "/orders", query: { status: 0 } });
  } catch {
    // 拦截器已提示，保持页面让用户修改
  } finally {
    submitting.value = false;
  }
}

onMounted(loadPreview);
</script>

<style scoped>
.checkout-page {
  max-width: 1000px;
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

.checkout-card {
  border-radius: 12px;
}

.section-title {
  margin: 8px 0 12px;
  font-size: 16px;
  color: #1f3d2b;
}

.section-title:not(:first-child) {
  margin-top: 28px;
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

.goods-shop {
  margin-top: 4px;
  font-size: 12px;
  color: #8a9d92;
}

.amount {
  color: #e05b4c;
  font-weight: 600;
}

.address-form {
  max-width: 620px;
}

.form-tip {
  margin-top: 4px;
  font-size: 12px;
  color: #8a9d92;
}

.checkout-footer {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 20px;
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid var(--el-border-color-lighter);
}

.total {
  color: #5d7668;
}

.total-price {
  font-size: 22px;
  font-weight: 700;
  color: #e05b4c;
}
</style>
