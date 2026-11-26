<template>
  <div class="order-detail-page">
    <div class="page-head">
      <el-button text @click="router.back()">
        <el-icon><ArrowLeft /></el-icon>
        返回
      </el-button>
      <h1>订单详情</h1>
    </div>

    <el-card v-loading="loading" shadow="never" class="detail-card">
      <el-empty v-if="!loading && !order" description="订单不存在或无权查看" />

      <template v-else-if="order">
        <div class="status-banner">
          <div>
            <div class="status-title">{{ orderStatusText(order.orderStatus) }}</div>
            <div class="status-desc">
              <template v-if="order.orderStatus === 0">订单尚未支付，请尽快完成支付</template>
              <template v-else-if="order.orderStatus === 1">商家正在备货，请耐心等待发货</template>
              <template v-else-if="order.orderStatus === 2">商品已发出，请注意查收</template>
              <template v-else-if="order.orderStatus === 3">订单已完成，感谢支持钟山镇农户</template>
              <template v-else>订单已取消</template>
            </div>
          </div>
          <div class="status-amount">
            实付金额
            <span class="amount">￥{{ money(order.payAmount) }}</span>
          </div>
        </div>

        <el-descriptions :column="2" border class="block">
          <el-descriptions-item label="订单号">{{ order.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="下单时间">{{ order.createTime }}</el-descriptions-item>
          <el-descriptions-item label="店铺">
            {{ order.shopName || order.farmerName || "-" }}
          </el-descriptions-item>
          <el-descriptions-item label="支付状态">
            <el-tag :type="order.payStatus === 1 ? 'success' : 'warning'" effect="plain">
              {{ PAY_STATUS[order.payStatus]?.text || "-" }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="支付方式">
            {{ PAY_TYPE[order.payType] || "-" }}
          </el-descriptions-item>
          <el-descriptions-item label="支付时间">{{ order.payTime || "-" }}</el-descriptions-item>
          <el-descriptions-item v-if="order.tradeNo" label="支付宝交易号" :span="2">
            {{ order.tradeNo }}
          </el-descriptions-item>
          <el-descriptions-item label="收货人">{{ order.receiverName }}</el-descriptions-item>
          <el-descriptions-item label="联系电话">{{ order.receiverPhone }}</el-descriptions-item>
          <el-descriptions-item label="收货地址" :span="2">
            {{ order.receiverAddress }}
          </el-descriptions-item>
          <el-descriptions-item label="买家备注" :span="2">
            {{ order.remark || "-" }}
          </el-descriptions-item>
          <el-descriptions-item v-if="order.logisticsCompany" label="物流公司">
            {{ order.logisticsCompany }}
          </el-descriptions-item>
          <el-descriptions-item v-if="order.logisticsNo" label="物流单号">
            {{ order.logisticsNo }}
          </el-descriptions-item>
        </el-descriptions>

        <h3 class="block-title">商品明细</h3>
        <el-table :data="order.items" border>
          <el-table-column label="商品" min-width="260">
            <template #default="{ row }">
              <div class="goods-cell">
                <el-image v-if="row.productCover" :src="row.productCover" fit="cover" class="goods-thumb" />
                <div>
                  <div class="goods-name">{{ row.productName }}</div>
                  <el-tag v-if="row.entrustId" size="small" type="success" effect="plain">委托代销</el-tag>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="单价" width="120" align="right">
            <template #default="{ row }">￥{{ money(row.price) }}</template>
          </el-table-column>
          <el-table-column label="数量" width="90" align="center">
            <template #default="{ row }">x{{ row.quantity }}</template>
          </el-table-column>
          <el-table-column label="小计" width="130" align="right">
            <template #default="{ row }">￥{{ money(row.subtotal) }}</template>
          </el-table-column>
        </el-table>

        <div class="detail-footer">
          <div class="summary">
            商品总额 ￥{{ money(order.totalAmount) }}，运费 ￥{{ money(order.freight) }}
          </div>
          <div v-if="isBuyer" class="actions">
            <el-button v-if="order.orderStatus === 0" @click="handleCancel">取消订单</el-button>
            <el-button v-if="order.orderStatus === 0" type="primary" @click="handlePay">
              去支付
            </el-button>
            <el-button v-if="order.orderStatus === 2" type="primary" @click="handleConfirm">
              确认收货
            </el-button>
          </div>
        </div>
      </template>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage, ElMessageBox } from "element-plus";
import { ArrowLeft } from "@element-plus/icons-vue";
import { cancelOrder, confirmOrder, getOrder, payOrder } from "../api/order";
import { money } from "../utils/format";
import { orderStatusText, PAY_STATUS, PAY_TYPE } from "../utils/order";
import { choosePayChannel, gotoAlipayCashier } from "../utils/alipay";
import { useUserStore } from "../store/user";

const route = useRoute();
const router = useRouter();
const store = useUserStore();

const loading = ref(false);
const order = ref(null);

const isBuyer = computed(
  () => String(store.state.user?.userId ?? "") === String(order.value?.buyerId ?? ""),
);

async function loadDetail() {
  loading.value = true;
  try {
    order.value = await getOrder(route.params.id);
  } catch {
    order.value = null;
  } finally {
    loading.value = false;
  }
}

async function handlePay() {
  const channel = await choosePayChannel(order.value, money(order.value.payAmount));
  if (!channel) {
    return;
  }
  // 支付宝：整页跳转到沙箱收银台，付款后由 return_url 跳回 /pay/result
  if (channel === "alipay") {
    await gotoAlipayCashier(order.value);
    return;
  }
  await payOrder(order.value.id, { payType: 3 });
  ElMessage.success("支付成功，等待商家发货");
  await loadDetail();
}

async function handleCancel() {
  try {
    await ElMessageBox.confirm("确认取消该订单？取消后库存将回滚。", "取消订单", {
      type: "warning",
    });
  } catch {
    return;
  }
  await cancelOrder(order.value.id, { reason: "买家主动取消" });
  ElMessage.success("订单已取消");
  await loadDetail();
}

async function handleConfirm() {
  try {
    await ElMessageBox.confirm("确认已收到商品？", "确认收货", { type: "info" });
  } catch {
    return;
  }
  await confirmOrder(order.value.id);
  ElMessage.success("已确认收货");
  await loadDetail();
}

onMounted(loadDetail);
</script>

<style scoped>
.order-detail-page {
  max-width: 1000px;
  margin: 0 auto;
  padding: 24px 20px 48px;
}

.page-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
}

.page-head h1 {
  margin: 0;
  font-size: 24px;
  color: #1f3d2b;
}

.detail-card {
  border-radius: 12px;
}

.status-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  margin-bottom: 20px;
  border-radius: 10px;
  background: linear-gradient(90deg, #eef6f0, #f7fbf8);
}

.status-title {
  font-size: 18px;
  font-weight: 700;
  color: #1f3d2b;
}

.status-desc {
  margin-top: 4px;
  font-size: 13px;
  color: #5d7668;
}

.status-amount {
  font-size: 13px;
  color: #5d7668;
}

.amount {
  display: block;
  margin-top: 4px;
  font-size: 22px;
  font-weight: 700;
  color: #e05b4c;
  text-align: right;
}

.block {
  margin-bottom: 20px;
}

.block-title {
  margin: 8px 0 12px;
  font-size: 16px;
  color: #1f3d2b;
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
  margin-bottom: 4px;
  font-weight: 500;
  color: #1f3d2b;
}

.detail-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 16px;
}

.summary {
  font-size: 13px;
  color: #5d7668;
}

.actions {
  display: flex;
  gap: 8px;
}
</style>
