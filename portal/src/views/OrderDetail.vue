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

        <h3 ref="itemsTitle" class="block-title">商品明细</h3>
        <el-table :data="order.items" border>
          <el-table-column label="商品" min-width="240">
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
          <el-table-column label="单价" width="110" align="right">
            <template #default="{ row }">￥{{ money(row.price) }}</template>
          </el-table-column>
          <el-table-column label="数量" width="80" align="center">
            <template #default="{ row }">x{{ row.quantity }}</template>
          </el-table-column>
          <el-table-column label="小计" width="120" align="right">
            <template #default="{ row }">￥{{ money(row.subtotal) }}</template>
          </el-table-column>
          <el-table-column label="售后" width="100" align="center">
            <template #default="{ row }">
              <el-tag
                v-if="row.afterSaleStatus !== null && row.afterSaleStatus !== undefined"
                :type="afterSaleStatusTag(row.afterSaleStatus)"
                size="small"
                effect="plain"
              >
                {{ afterSaleStatusText(row.afterSaleStatus) }}
              </el-tag>
              <span v-else class="muted">-</span>
            </template>
          </el-table-column>
          <el-table-column v-if="isBuyer" label="操作" width="110" align="center">
            <template #default="{ row }">
              <el-button v-if="canApply(row)" type="primary" link @click="openApply(row)">
                申请售后
              </el-button>
              <span v-else class="muted">-</span>
            </template>
          </el-table-column>
        </el-table>

        <template v-if="afterSales.length">
          <h3 class="block-title">售后进度</h3>
          <div v-for="record in afterSales" :key="record.id" class="as-row">
            <div class="as-row__main">
              <el-tag :type="saleTypeTag(record.saleType)" size="small" effect="plain">
                {{ saleTypeText(record.saleType) }}
              </el-tag>
              <span class="as-row__no">{{ record.afterSaleNo }}</span>
              <span class="as-row__product">{{ record.productName }}</span>
              <span v-if="record.refundAmount > 0" class="as-row__amount">
                退款 ￥{{ money(record.refundAmount) }}
              </span>
            </div>
            <div class="as-row__side">
              <span class="as-row__time">{{ record.createTime }}</span>
              <el-tag :type="afterSaleStatusTag(record.status)" size="small">
                {{ afterSaleStatusText(record.status) }}
              </el-tag>
            </div>
            <div v-if="record.handleRemark" class="as-row__remark">
              处理意见：{{ record.handleRemark }}
            </div>
          </div>
        </template>

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

    <el-dialog v-model="applyVisible" title="申请售后" width="560px" top="8vh">
      <el-form :model="applyForm" label-width="90px">
        <el-form-item label="商品">
          <span class="apply-product">{{ applyTarget?.productName }}</span>
        </el-form-item>
        <el-form-item label="售后类型" required>
          <el-radio-group v-model="applyForm.saleType" @change="handleTypeChange">
            <el-radio-button :value="1">仅退款</el-radio-button>
            <el-radio-button :value="2">退货退款</el-radio-button>
            <el-radio-button :value="3">换货</el-radio-button>
            <el-radio-button :value="4">咨询</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="needRefund" label="退款金额" required>
          <el-input-number
            v-model="applyForm.refundAmount"
            :min="0.01"
            :max="Number(applyTarget?.subtotal || 0)"
            :precision="2"
            :step="1"
            controls-position="right"
            style="width: 180px"
          />
          <span class="apply-hint">该商品小计 ￥{{ money(applyTarget?.subtotal) }}</span>
        </el-form-item>
        <el-form-item label="申请原因" required>
          <el-input v-model="applyForm.reason" maxlength="100" show-word-limit placeholder="如 商品受潮、少发一件" />
        </el-form-item>
        <el-form-item label="问题描述">
          <el-input
            v-model="applyForm.description"
            type="textarea"
            :rows="3"
            maxlength="500"
            show-word-limit
            placeholder="补充问题细节，便于商家核实处理"
          />
        </el-form-item>
        <el-form-item label="凭证图片">
          <MultiImageUpload v-model="applyForm.images" :limit="9" biz-type="proof" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="applyVisible = false">取消</el-button>
        <el-button type="primary" :loading="applying" @click="submitApply">提交申请</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, reactive, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage, ElMessageBox } from "element-plus";
import { ArrowLeft } from "@element-plus/icons-vue";
import { cancelOrder, confirmOrder, getOrder, payOrder } from "../api/order";
import { createAfterSale, getOrderAfterSales } from "../api/afterSale";
import MultiImageUpload from "../components/MultiImageUpload.vue";
import { money } from "../utils/format";
import { orderStatusText, PAY_STATUS, PAY_TYPE } from "../utils/order";
import {
  afterSaleStatusTag,
  afterSaleStatusText,
  isAfterSalePending,
  saleTypeTag,
  saleTypeText,
} from "../utils/afterSale";
import { choosePayChannel, gotoAlipayCashier } from "../utils/alipay";
import { useUserStore } from "../store/user";

const route = useRoute();
const router = useRouter();
const store = useUserStore();

const loading = ref(false);
const order = ref(null);
const afterSales = ref([]);
const itemsTitle = ref(null);

const applyVisible = ref(false);
const applying = ref(false);
const applyTarget = ref(null);
const applyForm = reactive({
  saleType: 1,
  reason: "",
  description: "",
  refundAmount: 0,
  images: [],
});

const isBuyer = computed(
  () => String(store.state.user?.userId ?? "") === String(order.value?.buyerId ?? ""),
);

const needRefund = computed(() => applyForm.saleType === 1 || applyForm.saleType === 2);

/** 已支付且在待收货/已完成后可售后，同一商品有进行中的售后时不可重复申请 */
function canApply(item) {
  if (!order.value || !isBuyer.value) {
    return false;
  }
  const status = order.value.orderStatus;
  const paid = order.value.payStatus === 1;
  return paid && (status === 2 || status === 3) && !isAfterSalePending(item.afterSaleStatus);
}

function openApply(item) {
  applyTarget.value = item;
  applyForm.saleType = 1;
  applyForm.reason = "";
  applyForm.description = "";
  applyForm.refundAmount = Number(item.subtotal || 0);
  applyForm.images = [];
  applyVisible.value = true;
}

function handleTypeChange() {
  if (!needRefund.value) {
    applyForm.refundAmount = 0;
  } else if (!applyForm.refundAmount) {
    applyForm.refundAmount = Number(applyTarget.value?.subtotal || 0);
  }
}

async function submitApply() {
  if (!applyForm.reason.trim()) {
    ElMessage.warning("请填写申请原因");
    return;
  }
  if (needRefund.value && !(applyForm.refundAmount > 0)) {
    ElMessage.warning("请填写退款金额");
    return;
  }
  applying.value = true;
  try {
    await createAfterSale({
      orderId: Number(order.value.id),
      orderItemId: Number(applyTarget.value.id),
      saleType: applyForm.saleType,
      reason: applyForm.reason.trim(),
      description: applyForm.description.trim() || undefined,
      refundAmount: needRefund.value ? applyForm.refundAmount : 0,
      images: applyForm.images,
    });
    ElMessage.success("售后申请已提交，请等待商家处理");
    applyVisible.value = false;
    await Promise.all([loadDetail(), loadAfterSales()]);
  } finally {
    applying.value = false;
  }
}

async function loadAfterSales() {
  if (!order.value) {
    afterSales.value = [];
    return;
  }
  try {
    afterSales.value = (await getOrderAfterSales(order.value.id)) || [];
  } catch {
    afterSales.value = [];
  }
}

async function loadDetail() {
  loading.value = true;
  try {
    order.value = await getOrder(route.params.id);
    await loadAfterSales();
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

onMounted(async () => {
  await loadDetail();
  // 从订单列表点「申请售后」进来时，定位到商品明细，由买家选择具体商品
  if (route.query.focus === "after-sale" && order.value) {
    nextTick(() => {
      itemsTitle.value?.scrollIntoView({ behavior: "smooth", block: "center" });
    });
    ElMessage.info("请在商品明细中选择要申请售后的商品");
  }
});
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

.muted {
  color: #8a9d92;
}

.as-row {
  padding: 10px 12px;
  margin-bottom: 8px;
  background: #f7faf8;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
}

.as-row__main {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 13px;
  color: #1f3d2b;
}

.as-row__no {
  font-weight: 600;
}

.as-row__product {
  color: #5d7668;
}

.as-row__amount {
  color: #e05b4c;
}

.as-row__side {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 6px;
  font-size: 12px;
  color: #8a9d92;
}

.as-row__time {
  color: #8a9d92;
}

.as-row__remark {
  margin-top: 6px;
  padding-top: 6px;
  border-top: 1px dashed var(--el-border-color-lighter);
  font-size: 12px;
  color: #5d7668;
}

.apply-product {
  color: #1f3d2b;
  font-weight: 500;
}

.apply-hint {
  margin-left: 10px;
  font-size: 12px;
  color: #8a9d92;
}
</style>
