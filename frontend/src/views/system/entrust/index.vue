<template>
  <div class="app-container">
    <el-card shadow="never" class="mb-4">
      <el-form inline @submit.prevent>
        <el-form-item label="关键字">
          <el-input
            v-model="queryParams.keyword"
            placeholder="委托编号 / 商品名称"
            clearable
            style="width: 220px"
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryParams.status" placeholder="全部" clearable style="width: 150px">
            <el-option label="待确认" :value="0" />
            <el-option label="生效中" :value="1" />
            <el-option label="已终止" :value="2" />
            <el-option label="已拒绝" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="search" @click="handleQuery">查询</el-button>
          <el-button icon="refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="entrustList" border stripe>
        <el-table-column prop="entrustNo" label="委托编号" min-width="170" />
        <el-table-column label="商品" min-width="200">
          <template #default="{ row }">
            <div class="flex items-center gap-2">
              <el-image
                v-if="row.productCover"
                :src="row.productCover"
                fit="cover"
                class="h-9 w-9 rounded"
                preview-teleported
                :preview-src-list="[row.productCover]"
              />
              <span>{{ row.productName || "-" }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="委托关系" min-width="180">
          <template #default="{ row }">
            {{ row.entrustUserName || "-" }} → {{ row.agentUserName || "-" }}
          </template>
        </el-table-column>
        <el-table-column label="受托方分成" width="110" align="center">
          <template #default="{ row }">{{ Number(row.splitRatio ?? 0).toFixed(0) }}%</template>
        </el-table-column>
        <el-table-column label="结算周期" width="100" align="center">
          <template #default="{ row }">{{ SETTLE_CYCLE[row.settleCycle] || "-" }}</template>
        </el-table-column>
        <el-table-column label="状态" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="ENTRUST_STATUS[row.status]?.tag || 'info'" effect="plain">
              {{ ENTRUST_STATUS[row.status]?.text || "-" }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="有效期" min-width="190">
          <template #default="{ row }">
            {{ row.startDate || "-" }} ~ {{ row.endDate || "长期" }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="190" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openSettle(row)">生成结算</el-button>
            <el-button type="primary" link @click="openSettlements(row)">结算记录</el-button>
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

    <el-dialog v-model="settleVisible" title="生成月度结算单" width="560px">
      <el-descriptions v-if="currentEntrust" :column="1" border size="small" class="mb-4">
        <el-descriptions-item label="委托编号">{{ currentEntrust.entrustNo }}</el-descriptions-item>
        <el-descriptions-item label="商品">{{ currentEntrust.productName }}</el-descriptions-item>
        <el-descriptions-item label="委托关系">
          {{ currentEntrust.entrustUserName }} → {{ currentEntrust.agentUserName }}
        </el-descriptions-item>
        <el-descriptions-item label="受托方分成">
          {{ Number(currentEntrust.splitRatio ?? 0).toFixed(0) }}%
        </el-descriptions-item>
      </el-descriptions>
      <el-form label-width="90px">
        <el-form-item label="结算月份" required>
          <el-date-picker v-model="settleMonth" type="month" value-format="YYYY-MM" placeholder="请选择月份" />
        </el-form-item>
      </el-form>
      <p class="text-xs text-[--el-text-color-secondary]">
        结算单按当月已支付订单与已完成退款汇总，生成后不可重复结算。
      </p>
      <template #footer>
        <el-button @click="settleVisible = false">取消</el-button>
        <el-button type="primary" :loading="settling" @click="handleSettle">生成结算单</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="settlementVisible" title="结算记录" width="800px">
      <el-empty v-if="!settlements.length" description="暂无结算单" />
      <el-table v-else :data="settlements" border>
        <el-table-column prop="settleMonth" label="月份" width="100" />
        <el-table-column prop="saleQty" label="销量" width="80" align="center" />
        <el-table-column label="销售额" width="110" align="right">
          <template #default="{ row }">￥{{ money(row.saleAmount) }}</template>
        </el-table-column>
        <el-table-column prop="refundQty" label="退单" width="80" align="center" />
        <el-table-column label="退款" width="100" align="right">
          <template #default="{ row }">￥{{ money(row.refundAmount) }}</template>
        </el-table-column>
        <el-table-column label="委托方应得" width="120" align="right">
          <template #default="{ row }">￥{{ money(row.entrustIncome) }}</template>
        </el-table-column>
        <el-table-column label="受托方应得" width="120" align="right">
          <template #default="{ row }">￥{{ money(row.agentIncome) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'warning'" effect="plain">
              {{ row.status === 1 ? "已结算" : "待结算" }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="settleTime" label="结算时间" min-width="160" />
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import EntrustAPI, { type EntrustQueryParams, type EntrustVO, type SettlementVO } from "@/api/entrust";

const SETTLE_CYCLE: Record<number, string> = { 1: "按月", 2: "按季", 3: "按单" };
const ENTRUST_STATUS: Record<number, { text: string; tag: "primary" | "success" | "info" | "warning" | "danger" }> = {
  0: { text: "待确认", tag: "warning" },
  1: { text: "生效中", tag: "success" },
  2: { text: "已终止", tag: "info" },
  3: { text: "已拒绝", tag: "danger" },
};

const loading = ref(false);
const settling = ref(false);
const entrustList = ref<EntrustVO[]>([]);
const total = ref(0);

const queryParams = reactive<EntrustQueryParams>({
  pageNum: 1,
  pageSize: 10,
  keyword: "",
  status: undefined,
});

const settleVisible = ref(false);
const currentEntrust = ref<EntrustVO | null>(null);
const settleMonth = ref("");

const settlementVisible = ref(false);
const settlements = ref<SettlementVO[]>([]);

function money(value?: number) {
  return Number(value ?? 0).toFixed(2);
}

async function loadData() {
  loading.value = true;
  try {
    const data = await EntrustAPI.getAdminPage(queryParams);
    entrustList.value = data.list || [];
    total.value = data.total || 0;
  } catch {
    entrustList.value = [];
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
  queryParams.keyword = "";
  queryParams.status = undefined;
  loadData();
}

function openSettle(row: any) {
  currentEntrust.value = row;
  const now = new Date();
  settleMonth.value = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, "0")}`;
  settleVisible.value = true;
}

async function handleSettle() {
  if (!settleMonth.value) {
    ElMessage.warning("请选择结算月份");
    return;
  }
  settling.value = true;
  try {
    await EntrustAPI.settle(currentEntrust.value!.id, settleMonth.value);
    ElMessage.success(`${settleMonth.value} 结算单已生成`);
    settleVisible.value = false;
    await loadData();
  } finally {
    settling.value = false;
  }
}

async function openSettlements(row: any) {
  const data = await EntrustAPI.getSettlements(row.id, { pageNum: 1, pageSize: 12 });
  settlements.value = data.list || [];
  settlementVisible.value = true;
}

onMounted(loadData);
</script>
