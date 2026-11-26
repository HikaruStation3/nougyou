<template>
  <div class="app-container">
    <el-card shadow="never" class="mb-4">
      <el-form inline @submit.prevent>
        <el-form-item label="售后状态">
          <el-select v-model="queryParams.status" placeholder="全部" clearable style="width: 140px">
            <el-option v-for="(item, key) in AFTER_SALE_STATUS" :key="key" :label="item.text" :value="Number(key)" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input
            v-model="queryParams.keyword"
            placeholder="售后单号 / 订单号 / 商品名"
            clearable
            style="width: 240px"
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="search" @click="handleQuery">查询</el-button>
          <el-button icon="refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
      <el-alert type="info" :closable="false" class="tip">
        商家拒绝的售后（状态「已驳回」）可在此由平台终裁：支持买家退款则按金额原路退回，驳回则维持商家结论。
      </el-alert>
    </el-card>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column prop="afterSaleNo" label="售后单号" min-width="160" />
        <el-table-column label="商品" min-width="200">
          <template #default="{ row }">
            <div>{{ row.productName || "-" }}</div>
            <div class="text-xs text-[--el-text-color-secondary]">订单 {{ row.orderNo || "-" }}</div>
          </template>
        </el-table-column>
        <el-table-column label="买家" min-width="100">
          <template #default="{ row }">{{ row.buyerName || "-" }}</template>
        </el-table-column>
        <el-table-column label="商家" min-width="150">
          <template #default="{ row }">
            <div>{{ row.shopName || "-" }}</div>
            <div class="text-xs text-[--el-text-color-secondary]">{{ row.farmerName || "-" }}</div>
          </template>
        </el-table-column>
        <el-table-column label="类型" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="SALE_TYPE[row.saleType]?.tag || 'info'" size="small" effect="plain">
              {{ SALE_TYPE[row.saleType]?.text || "-" }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="申请退款" width="110" align="right">
          <template #default="{ row }">￥{{ money(row.refundAmount) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="AFTER_SALE_STATUS[row.status]?.tag || 'info'" effect="plain">
              {{ AFTER_SALE_STATUS[row.status]?.text || "-" }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="申请时间" min-width="160" />
        <el-table-column label="操作" width="120" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openDetail(row)">详情</el-button>
            <el-button
              v-if="row.status === 3"
              v-hasPerm="'afterSale:intervene'"
              type="warning"
              link
              @click="openIntervene(row)"
            >
              介入
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="mt-4 flex justify-end">
        <el-pagination
          v-model:current-page="queryParams.pageNum"
          v-model:page-size="queryParams.pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          background
          @size-change="loadData"
          @current-change="loadData"
        />
      </div>
    </el-card>

    <el-dialog v-model="detailVisible" title="售后详情" width="720px" top="7vh">
      <template v-if="current">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="售后单号">{{ current.afterSaleNo }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="AFTER_SALE_STATUS[current.status]?.tag || 'info'" effect="plain">
              {{ AFTER_SALE_STATUS[current.status]?.text || "-" }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="订单号">{{ current.orderNo || "-" }}</el-descriptions-item>
          <el-descriptions-item label="类型">{{ SALE_TYPE[current.saleType]?.text || "-" }}</el-descriptions-item>
          <el-descriptions-item label="买家">{{ current.buyerName || "-" }}</el-descriptions-item>
          <el-descriptions-item label="商家">
            {{ current.shopName || "-" }}（{{ current.farmerName || "-" }}）
          </el-descriptions-item>
          <el-descriptions-item label="商品" :span="2">
            {{ current.productName || "-" }}（x{{ current.quantity ?? 1 }}，小计 ￥{{ money(current.itemSubtotal) }}）
          </el-descriptions-item>
          <el-descriptions-item label="申请原因" :span="2">{{ current.reason || "-" }}</el-descriptions-item>
          <el-descriptions-item label="问题描述" :span="2">{{ current.description || "-" }}</el-descriptions-item>
          <el-descriptions-item label="申请退款">
            <span class="text-[--el-color-danger] font-semibold">￥{{ money(current.refundAmount) }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="申请时间">{{ current.createTime }}</el-descriptions-item>
          <el-descriptions-item label="处理人">{{ current.handleUserName || "-" }}</el-descriptions-item>
          <el-descriptions-item label="处理时间">{{ current.handleTime || "-" }}</el-descriptions-item>
          <el-descriptions-item label="处理意见" :span="2">{{ current.handleRemark || "-" }}</el-descriptions-item>
        </el-descriptions>

        <div v-if="current.images?.length" class="mt-4">
          <div class="mb-2 text-sm text-[--el-text-color-secondary]">买家凭证</div>
          <div class="flex flex-wrap gap-2">
            <el-image
              v-for="(url, index) in current.images"
              :key="url"
              :src="url"
              fit="cover"
              class="h-[88px] w-[88px] rounded border border-[--el-border-color-lighter]"
              :preview-src-list="current.images"
              :initial-index="index"
              preview-teleported
            />
          </div>
        </div>
      </template>
    </el-dialog>

    <el-dialog v-model="interveneVisible" title="平台介入处理" width="560px" top="8vh">
      <template v-if="target">
        <el-alert type="warning" :closable="false" class="mb-4">
          {{ target.afterSaleNo }} · 商家已驳回 · 买家申请退款 ￥{{ money(target.refundAmount) }}
          <div v-if="target.handleRemark" class="mt-1 text-xs">商家意见：{{ target.handleRemark }}</div>
        </el-alert>
        <el-form label-width="90px">
          <el-form-item label="裁定结果" required>
            <el-radio-group v-model="form.handleResult">
              <el-radio :value="1">支持买家退款</el-radio>
              <el-radio :value="3">驳回申请</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item v-if="form.handleResult === 1" label="退款金额">
            <el-input-number
              v-model="form.refundAmount"
              :min="0"
              :max="Number(target.refundAmount || 0) || undefined"
              :precision="2"
              controls-position="right"
              style="width: 180px"
            />
            <span class="ml-2 text-xs text-[--el-text-color-secondary]">
              不超过申请金额 ￥{{ money(target.refundAmount) }}
            </span>
          </el-form-item>
          <el-form-item label="裁定说明" required>
            <el-input
              v-model="form.handleRemark"
              type="textarea"
              :rows="3"
              maxlength="200"
              show-word-limit
              placeholder="如 经核实商品确有质量问题，支持退款"
            />
          </el-form-item>
        </el-form>
      </template>
      <template #footer>
        <el-button @click="interveneVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitIntervene">确认裁定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import AfterSaleAPI, { type AfterSaleQueryParams, type AfterSaleVO } from "@/api/afterSale";

const AFTER_SALE_STATUS: Record<number, { text: string; tag: "primary" | "success" | "info" | "warning" | "danger" }> = {
  0: { text: "待处理", tag: "warning" },
  1: { text: "处理中", tag: "primary" },
  2: { text: "已完成", tag: "success" },
  3: { text: "已驳回", tag: "danger" },
};

const SALE_TYPE: Record<number, { text: string; tag: "primary" | "success" | "info" | "warning" | "danger" }> = {
  1: { text: "仅退款", tag: "warning" },
  2: { text: "退货退款", tag: "danger" },
  3: { text: "换货", tag: "primary" },
  4: { text: "咨询", tag: "info" },
};

const loading = ref(false);
const submitting = ref(false);
const list = ref<AfterSaleVO[]>([]);
const total = ref(0);

const detailVisible = ref(false);
const current = ref<AfterSaleVO | null>(null);

const interveneVisible = ref(false);
const target = ref<AfterSaleVO | null>(null);
const form = reactive({ handleResult: 1, refundAmount: 0, handleRemark: "" });

const queryParams = reactive<AfterSaleQueryParams>({
  pageNum: 1,
  pageSize: 10,
  status: undefined,
  keyword: "",
});

function money(value?: number) {
  return Number(value ?? 0).toFixed(2);
}

async function loadData() {
  loading.value = true;
  try {
    const data = await AfterSaleAPI.getAdminPage(queryParams);
    list.value = data.list || [];
    total.value = data.total || 0;
  } catch {
    list.value = [];
    total.value = 0;
  } finally {
    loading.value = false;
  }
}

function handleQuery() {
  queryParams.pageNum = 1;
  loadData();
}

function handleReset() {
  queryParams.pageNum = 1;
  queryParams.status = undefined;
  queryParams.keyword = "";
  loadData();
}

function openDetail(row: any) {
  current.value = row as AfterSaleVO;
  detailVisible.value = true;
}

function openIntervene(row: any) {
  const record = row as AfterSaleVO;
  target.value = record;
  form.handleResult = 1;
  form.refundAmount = Number(record.refundAmount || 0);
  form.handleRemark = "";
  interveneVisible.value = true;
}

async function submitIntervene() {
  if (!form.handleRemark.trim()) {
    ElMessage.warning("请填写裁定说明");
    return;
  }
  if (form.handleResult === 1 && !(form.refundAmount > 0) && [1, 2].includes(target.value?.saleType || 0)) {
    ElMessage.warning("请填写退款金额");
    return;
  }
  submitting.value = true;
  try {
    await AfterSaleAPI.intervene(target.value!.id, {
      handleResult: form.handleResult,
      refundAmount: form.handleResult === 1 ? form.refundAmount : undefined,
      handleRemark: form.handleRemark.trim(),
    });
    ElMessage.success(form.handleResult === 1 ? "已支持买家退款" : "已驳回申请");
    interveneVisible.value = false;
    await loadData();
  } finally {
    submitting.value = false;
  }
}

onMounted(loadData);
</script>

<style scoped>
.tip {
  margin-top: 4px;
}
</style>
