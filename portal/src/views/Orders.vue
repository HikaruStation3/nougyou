<template>
  <div class="orders-page">
    <section class="page-hero">
      <h1>我的订单</h1>
      <p>查看订单进度、完成支付或确认收货，订单按商家自动拆分。</p>
    </section>

    <el-card shadow="never" class="orders-card">
      <el-tabs v-model="activeStatus" @tab-change="handleTabChange">
        <el-tab-pane label="全部" name="all" />
        <el-tab-pane label="待付款" name="0" />
        <el-tab-pane label="待发货" name="1" />
        <el-tab-pane label="待收货" name="2" />
        <el-tab-pane label="已完成" name="3" />
        <el-tab-pane label="已取消" name="4" />
      </el-tabs>

      <div class="toolbar">
        <el-input
          v-model="keyword"
          placeholder="搜索订单号或商品名"
          clearable
          style="width: 260px"
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        />
        <el-button type="primary" @click="handleSearch">搜索</el-button>
      </div>

      <div v-loading="loading">
        <el-empty v-if="!loading && !orders.length" description="暂无订单">
          <el-button type="primary" @click="router.push('/products')">去逛逛商城</el-button>
        </el-empty>

        <div v-for="order in orders" :key="order.id" class="order-card">
          <div class="order-head">
            <span class="order-no">{{ order.orderNo }}</span>
            <span class="order-shop">{{ order.shopName || order.farmerName || "钟山镇农户" }}</span>
            <span class="order-time">{{ order.createTime }}</span>
            <el-tag :type="orderStatusTag(order.orderStatus)" effect="plain" class="order-status">
              {{ orderStatusText(order.orderStatus) }}
            </el-tag>
          </div>

          <div class="order-items">
            <div v-for="item in order.items" :key="item.id" class="order-item">
              <el-image v-if="item.productCover" :src="item.productCover" fit="cover" class="item-cover" />
              <div class="item-info">
                <div class="item-name">{{ item.productName }}</div>
                <div class="item-meta">
                  ￥{{ money(item.price) }} x {{ item.quantity }}
                  <el-tag v-if="item.entrustId" size="small" type="success" effect="plain" class="entrust-tag">
                    委托代销
                  </el-tag>
                </div>
              </div>
              <div class="item-subtotal">￥{{ money(item.subtotal) }}</div>
            </div>
          </div>

          <div class="order-foot">
            <span class="order-total">
              共 {{ itemCount(order) }} 件，实付
              <span class="amount">￥{{ money(order.payAmount) }}</span>
            </span>
            <div class="order-actions">
              <el-button size="small" @click="goDetail(order)">查看详情</el-button>
              <el-button
                v-if="order.orderStatus === 0"
                size="small"
                @click="handleCancel(order)"
                >取消订单</el-button
              >
              <el-button
                v-if="order.orderStatus === 0"
                size="small"
                type="primary"
                @click="handlePay(order)"
                >去支付</el-button
              >
              <el-button
                v-if="order.orderStatus === 2"
                size="small"
                type="primary"
                @click="handleConfirm(order)"
                >确认收货</el-button
              >
            </div>
          </div>
        </div>

        <div v-if="total > 0" class="pager">
          <el-pagination
            v-model:current-page="pageNum"
            v-model:page-size="pageSize"
            :total="total"
            :page-sizes="[5, 10, 20]"
            layout="total, sizes, prev, pager, next"
            background
            @size-change="loadOrders"
            @current-change="loadOrders"
          />
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage, ElMessageBox } from "element-plus";
import {
  cancelOrder,
  confirmOrder,
  getMyOrders,
  payOrder,
} from "../api/order";
import { money } from "../utils/format";
import { orderStatusTag, orderStatusText } from "../utils/order";
import { choosePayChannel, gotoAlipayCashier } from "../utils/alipay";

const route = useRoute();
const router = useRouter();

const loading = ref(false);
const orders = ref([]);
const total = ref(0);
const pageNum = ref(1);
const pageSize = ref(5);
const keyword = ref("");
const activeStatus = ref(
  route.query.status === undefined ? "all" : String(route.query.status),
);

function itemCount(order) {
  return (order.items || []).reduce((sum, item) => sum + (item.quantity || 0), 0);
}

async function loadOrders() {
  loading.value = true;
  try {
    const params = { pageNum: pageNum.value, pageSize: pageSize.value };
    if (activeStatus.value !== "all") {
      params.orderStatus = Number(activeStatus.value);
    }
    if (keyword.value.trim()) {
      params.keyword = keyword.value.trim();
    }
    const data = await getMyOrders(params);
    orders.value = data?.list || [];
    total.value = data?.total || 0;
  } catch {
    orders.value = [];
    total.value = 0;
  } finally {
    loading.value = false;
  }
}

function handleTabChange() {
  pageNum.value = 1;
  const query = activeStatus.value === "all" ? {} : { status: activeStatus.value };
  router.replace({ path: "/orders", query });
  loadOrders();
}

function handleSearch() {
  pageNum.value = 1;
  loadOrders();
}

function goDetail(order) {
  router.push(`/orders/${order.id}`);
}

async function handlePay(order) {
  const channel = await choosePayChannel(order, money(order.payAmount));
  if (!channel) {
    return;
  }
  if (channel === "alipay") {
    await gotoAlipayCashier(order);
    return;
  }
  await payOrder(order.id, { payType: 3 });
  ElMessage.success("支付成功，等待商家发货");
  await loadOrders();
}

async function handleCancel(order) {
  try {
    await ElMessageBox.confirm(`确认取消订单 ${order.orderNo}？取消后库存将回滚。`, "取消订单", {
      type: "warning",
    });
  } catch {
    return;
  }
  await cancelOrder(order.id, { reason: "买家主动取消" });
  ElMessage.success("订单已取消");
  await loadOrders();
}

async function handleConfirm(order) {
  try {
    await ElMessageBox.confirm("确认已收到商品？确认后订单将完成。", "确认收货", {
      type: "info",
    });
  } catch {
    return;
  }
  await confirmOrder(order.id);
  ElMessage.success("已确认收货");
  await loadOrders();
}

onMounted(loadOrders);
</script>

<style scoped>
.orders-page {
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

.orders-card {
  border-radius: 12px;
}

.toolbar {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
}

.order-card {
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 10px;
  margin-bottom: 16px;
  overflow: hidden;
}

.order-head {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 10px 14px;
  background: #f5f8f6;
  font-size: 13px;
  color: #5d7668;
}

.order-no {
  font-weight: 600;
  color: #1f3d2b;
}

.order-shop {
  color: #1f3d2b;
}

.order-time {
  margin-left: auto;
  font-size: 12px;
  color: #8a9d92;
}

.order-items {
  padding: 6px 14px;
}

.order-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 0;
  border-bottom: 1px dashed var(--el-border-color-lighter);
}

.order-item:last-child {
  border-bottom: none;
}

.item-cover {
  width: 56px;
  height: 56px;
  border-radius: 6px;
  flex-shrink: 0;
}

.item-info {
  flex: 1;
  min-width: 0;
}

.item-name {
  font-weight: 500;
  color: #1f3d2b;
}

.item-meta {
  margin-top: 4px;
  font-size: 12px;
  color: #8a9d92;
}

.entrust-tag {
  margin-left: 6px;
}

.item-subtotal {
  font-weight: 600;
  color: #1f3d2b;
}

.order-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 14px;
  border-top: 1px solid var(--el-border-color-lighter);
}

.order-total {
  font-size: 13px;
  color: #5d7668;
}

.amount {
  font-size: 18px;
  font-weight: 700;
  color: #e05b4c;
}

.order-actions {
  display: flex;
  gap: 8px;
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 8px;
}
</style>
