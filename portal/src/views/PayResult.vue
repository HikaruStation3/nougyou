<template>
  <div class="pay-result-page">
    <el-card shadow="never" class="result-card" v-loading="checking">
      <el-result
        :icon="resultIcon"
        :title="resultTitle"
        :sub-title="resultSubTitle"
      >
        <template #extra>
          <el-button v-if="detail?.orderId" type="primary" @click="goDetail">查看订单</el-button>
          <el-button @click="goOrders">返回订单列表</el-button>
          <el-button v-if="!paid && orderNo" :loading="checking" @click="check(true)">
            重新查询
          </el-button>
        </template>
      </el-result>

      <el-descriptions v-if="detail" :column="1" border class="detail-block">
        <el-descriptions-item label="订单号">{{ detail.orderNo }}</el-descriptions-item>
        <el-descriptions-item v-if="detail.tradeNo" label="支付宝交易号">
          {{ detail.tradeNo }}
        </el-descriptions-item>
        <el-descriptions-item v-if="detail.totalAmount" label="支付金额">
          ￥{{ detail.totalAmount }}
        </el-descriptions-item>
        <el-descriptions-item label="交易状态">
          {{ TRADE_STATUS[detail.tradeStatus] || detail.tradeStatus || "-" }}
        </el-descriptions-item>
      </el-descriptions>

      <div class="tips">
        <p>· 沙箱付款用的是支付宝提供的虚拟买家账号，不会产生真实扣款。</p>
        <p>· 若收银台显示「交易已关闭」，回订单列表取消该单重新下单即可。</p>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { queryAlipayTrade } from "../api/order";
import { clearPendingOrderNo, readPendingOrderNo, readReturnOrderNo } from "../utils/alipay";

const router = useRouter();

const TRADE_STATUS = {
  WAIT_BUYER_PAY: "等待付款",
  TRADE_SUCCESS: "支付成功",
  TRADE_FINISHED: "交易完成",
  TRADE_CLOSED: "交易关闭",
};

const orderNo = ref("");
const detail = ref(null);
const checking = ref(true);
/** success / unpaid / error */
const state = ref("unpaid");

const paid = computed(() => state.value === "success");
const resultIcon = computed(() =>
  state.value === "success" ? "success" : state.value === "error" ? "error" : "warning"
);
const resultTitle = computed(() => {
  if (checking.value) return "正在确认支付结果…";
  if (state.value === "success") return "支付成功";
  if (state.value === "error") return "查询支付结果失败";
  return "尚未完成支付";
});
const resultSubTitle = computed(() => {
  if (checking.value) return "正在向支付宝查询交易状态，请稍候";
  if (state.value === "success") return "商家将尽快为你发货";
  if (state.value === "error") return detail.value?.message || "请稍后重试或联系管理员";
  return detail.value?.message || "可以在订单列表重新发起支付";
});

/** 查询一次或轮询多次：刚付款时支付宝侧可能有秒级延迟，多查两次更稳 */
async function check(poll = false) {
  if (!orderNo.value) {
    checking.value = false;
    state.value = "error";
    return;
  }
  checking.value = true;
  const attempts = poll ? 3 : 1;
  try {
    for (let i = 0; i < attempts; i++) {
      const result = await queryAlipayTrade(orderNo.value);
      detail.value = result;
      if (result.paid) {
        state.value = "success";
        clearPendingOrderNo();
        ElMessage.success("支付成功");
        return;
      }
      if (i < attempts - 1) {
        await new Promise((resolve) => setTimeout(resolve, 2000));
      }
    }
    // 有 message 且带了验签/网关字样才算异常，其余都是「还没付」
    state.value = detail.value?.message?.includes("验签") ? "error" : "unpaid";
  } catch {
    state.value = "error";
  } finally {
    checking.value = false;
  }
}

function goDetail() {
  router.replace(`/orders/${detail.value.orderId}`);
}

function goOrders() {
  router.replace({ path: "/orders", query: { status: 0 } });
}

onMounted(() => {
  const { orderNo: fromUrl } = readReturnOrderNo();
  // 回跳参数优先，其次是跳转收银台前记下的订单号
  orderNo.value = fromUrl || readPendingOrderNo() || "";
  check(true);
});
</script>

<style scoped>
.pay-result-page {
  max-width: 720px;
  margin: 32px auto;
  padding: 0 16px;
}

.result-card {
  border-radius: 12px;
}

.detail-block {
  margin: 8px 24px 0;
}

.tips {
  margin: 20px 24px 4px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 1.9;
}

.tips p {
  margin: 0;
}
</style>
