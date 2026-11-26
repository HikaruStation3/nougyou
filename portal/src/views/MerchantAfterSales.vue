<template>
  <div class="merchant-after-sales-page">
    <section class="page-hero">
      <h1>
        商家售后
        <span v-if="shopName" class="hero-shop">店铺 · {{ shopName }}</span>
      </h1>
      <p>处理本店买家的退款、退货、换货与咨询诉求，受理退款会按填写金额原路退回。</p>
    </section>

    <el-card shadow="never" class="table-card">
      <div class="toolbar">
        <el-select v-model="status" placeholder="全部状态" clearable style="width: 150px" @change="handleSearch">
          <el-option v-for="(item, key) in AFTER_SALE_STATUS" :key="key" :label="item.text" :value="Number(key)" />
        </el-select>
        <el-input
          v-model="keyword"
          placeholder="搜索售后单号、订单号或商品名"
          clearable
          style="width: 280px"
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        />
        <el-button type="primary" @click="handleSearch">查询</el-button>
      </div>

      <el-table v-loading="loading" :data="records" border stripe>
        <el-table-column prop="afterSaleNo" label="售后单号" min-width="160" />
        <el-table-column label="商品" min-width="200">
          <template #default="{ row }">
            <div class="goods-cell">
              <el-image v-if="row.productCover" :src="row.productCover" fit="cover" class="goods-thumb" />
              <div>
                <div>{{ row.productName || "-" }}</div>
                <div class="text-xs muted">订单 {{ row.orderNo || "-" }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="类型" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="saleTypeTag(row.saleType)" size="small" effect="plain">
              {{ saleTypeText(row.saleType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="买家" min-width="100">
          <template #default="{ row }">{{ row.buyerName || "-" }}</template>
        </el-table-column>
        <el-table-column label="申请退款" width="110" align="right">
          <template #default="{ row }">
            <span class="amount">￥{{ money(row.refundAmount) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="afterSaleStatusTag(row.status)" effect="plain">
              {{ afterSaleStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="申请时间" min-width="160" />
        <el-table-column label="操作" width="120" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openDetail(row)">详情</el-button>
            <el-button
              v-if="row.status === 0 || row.status === 1"
              type="success"
              link
              @click="openHandle(row)"
            >
              处理
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination
          v-model:current-page="pageNum"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          background
          @size-change="loadData"
          @current-change="loadData"
        />
      </div>
    </el-card>

    <el-dialog v-model="detailVisible" :title="current ? '售后详情' : ''" width="680px" top="7vh">
      <template v-if="current">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="售后单号">{{ current.afterSaleNo }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="afterSaleStatusTag(current.status)" effect="plain">
              {{ afterSaleStatusText(current.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="订单号">{{ current.orderNo || "-" }}</el-descriptions-item>
          <el-descriptions-item label="申请时间">{{ current.createTime }}</el-descriptions-item>
          <el-descriptions-item label="买家">{{ current.buyerName || "-" }}</el-descriptions-item>
          <el-descriptions-item label="售后类型">{{ saleTypeText(current.saleType) }}</el-descriptions-item>
          <el-descriptions-item label="商品" :span="2">
            {{ current.productName || "-" }}（x{{ current.quantity ?? 1 }}，小计 ￥{{ money(current.itemSubtotal) }}）
          </el-descriptions-item>
          <el-descriptions-item label="申请原因" :span="2">{{ current.reason || "-" }}</el-descriptions-item>
          <el-descriptions-item label="问题描述" :span="2">{{ current.description || "-" }}</el-descriptions-item>
          <el-descriptions-item label="申请退款">
            <span class="amount">￥{{ money(current.refundAmount) }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="处理时间">{{ current.handleTime || "-" }}</el-descriptions-item>
          <el-descriptions-item label="处理意见" :span="2">{{ current.handleRemark || "-" }}</el-descriptions-item>
        </el-descriptions>

        <div v-if="current.images?.length" class="proof-block">
          <div class="proof-title">买家凭证</div>
          <div class="proof-list">
            <el-image
              v-for="(url, index) in current.images"
              :key="url"
              :src="url"
              fit="cover"
              class="proof-img"
              :preview-src-list="current.images"
              :initial-index="index"
              preview-teleported
            />
          </div>
        </div>
      </template>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="handleVisible" title="处理售后" width="560px" top="8vh">
      <template v-if="handleTarget">
        <el-alert type="info" :closable="false" class="handle-tip">
          {{ handleTarget.afterSaleNo }} · {{ saleTypeText(handleTarget.saleType) }} ·
          申请退款 ￥{{ money(handleTarget.refundAmount) }}
        </el-alert>
        <el-form :model="handleForm" label-width="90px">
          <el-form-item label="处理结果" required>
            <el-radio-group v-model="handleForm.handleResult" @change="handleResultChange">
              <el-radio :value="1">受理并退款</el-radio>
              <el-radio :value="3">拒绝申请</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item v-if="handleForm.handleResult === 1" label="退款金额" required>
            <el-input-number
              v-model="handleForm.refundAmount"
              :min="0"
              :max="Number(handleTarget.refundAmount || 0) || undefined"
              :precision="2"
              :step="1"
              controls-position="right"
              style="width: 180px"
            />
            <span class="handle-hint">不能超过申请金额 ￥{{ money(handleTarget.refundAmount) }}</span>
          </el-form-item>
          <el-form-item label="处理意见" :required="handleForm.handleResult === 3">
            <el-input
              v-model="handleForm.handleRemark"
              type="textarea"
              :rows="3"
              maxlength="200"
              show-word-limit
              :placeholder="handleForm.handleResult === 1 ? '如 已核实，同意退款' : '拒绝时必填，说明驳回理由'"
            />
          </el-form-item>
        </el-form>
      </template>
      <template #footer>
        <el-button @click="handleVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitHandle">确认处理</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { getFarmerAfterSales, handleAfterSale } from "../api/afterSale";
import { useUserStore } from "../store/user";
import { money } from "../utils/format";
import {
  AFTER_SALE_STATUS,
  afterSaleStatusTag,
  afterSaleStatusText,
  saleTypeTag,
  saleTypeText,
} from "../utils/afterSale";

const store = useUserStore();
const shopName = computed(() => store.state.user?.shopName || "");

const loading = ref(false);
const records = ref([]);
const total = ref(0);
const pageNum = ref(1);
const pageSize = ref(10);
const keyword = ref("");
const status = ref(null);

const detailVisible = ref(false);
const current = ref(null);

const handleVisible = ref(false);
const submitting = ref(false);
const handleTarget = ref(null);
const handleForm = reactive({ handleResult: 1, refundAmount: 0, handleRemark: "" });

async function loadData() {
  loading.value = true;
  try {
    const params = { pageNum: pageNum.value, pageSize: pageSize.value };
    if (status.value !== null && status.value !== "") {
      params.status = status.value;
    }
    if (keyword.value.trim()) {
      params.keyword = keyword.value.trim();
    }
    const data = await getFarmerAfterSales(params);
    records.value = data?.list || [];
    total.value = data?.total || 0;
  } catch {
    records.value = [];
    total.value = 0;
  } finally {
    loading.value = false;
  }
}

function handleSearch() {
  pageNum.value = 1;
  loadData();
}

function openDetail(row) {
  current.value = row;
  detailVisible.value = true;
}

function openHandle(row) {
  handleTarget.value = row;
  handleForm.handleResult = 1;
  handleForm.refundAmount = Number(row.refundAmount || 0);
  handleForm.handleRemark = "";
  handleVisible.value = true;
}

function handleResultChange() {
  if (handleForm.handleResult === 3) {
    handleForm.refundAmount = 0;
  } else if (!handleForm.refundAmount) {
    handleForm.refundAmount = Number(handleTarget.value?.refundAmount || 0);
  }
}

async function submitHandle() {
  if (handleForm.handleResult === 3 && !handleForm.handleRemark.trim()) {
    ElMessage.warning("拒绝售后时请填写处理意见");
    return;
  }
  if (handleForm.handleResult === 1 && !(handleForm.refundAmount > 0)
      && [1, 2].includes(handleTarget.value?.saleType)) {
    ElMessage.warning("请填写退款金额");
    return;
  }
  try {
    await ElMessageBox.confirm(
      handleForm.handleResult === 1
        ? `确认受理并退款 ￥${money(handleForm.refundAmount)}？`
        : "确认拒绝该售后申请？",
      "处理售后",
      { type: "warning" },
    );
  } catch {
    return;
  }
  submitting.value = true;
  try {
    await handleAfterSale(handleTarget.value.id, {
      handleResult: handleForm.handleResult,
      refundAmount: handleForm.handleResult === 1 ? handleForm.refundAmount : undefined,
      handleRemark: handleForm.handleRemark.trim() || undefined,
    });
    ElMessage.success(handleForm.handleResult === 1 ? "已受理并退款" : "已拒绝该售后");
    handleVisible.value = false;
    await loadData();
  } finally {
    submitting.value = false;
  }
}

onMounted(loadData);
</script>

<style scoped>
.merchant-after-sales-page {
  max-width: 1120px;
  margin: 0 auto;
  padding: 32px 20px 48px;
}

.page-hero {
  margin-bottom: 20px;
}

.page-hero h1 {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 0 0 8px;
  font-size: 26px;
  color: #1f3d2b;
}

.hero-shop {
  padding: 2px 10px;
  color: #1f7a47;
  font-size: 12px;
  font-weight: 400;
  background: #e6f4ec;
  border: 1px solid #c9e5d5;
  border-radius: 999px;
}

.page-hero p {
  margin: 0;
  color: #5d7668;
}

.table-card {
  border-radius: 12px;
}

.toolbar {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
}

.goods-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.goods-thumb {
  width: 42px;
  height: 42px;
  border-radius: 6px;
  flex-shrink: 0;
}

.text-xs {
  font-size: 12px;
}

.muted {
  color: #8a9d92;
}

.amount {
  font-weight: 600;
  color: #e05b4c;
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.proof-block {
  margin-top: 16px;
}

.proof-title {
  margin-bottom: 8px;
  font-size: 13px;
  color: #5d7668;
}

.proof-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.proof-img {
  width: 88px;
  height: 88px;
  border-radius: 6px;
  border: 1px solid var(--el-border-color-lighter);
}

.handle-tip {
  margin-bottom: 16px;
}

.handle-hint {
  margin-left: 10px;
  font-size: 12px;
  color: #8a9d92;
}
</style>
