<template>
  <div class="app-container ai-stat">
    <!-- 筛选区 -->
    <div class="filter-section">
      <el-form :inline="true" label-width="auto" @submit.prevent>
        <el-form-item label="统计区间">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            unlink-panels
            range-separator="~"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            :shortcuts="shortcuts"
            style="width: 260px"
          />
        </el-form-item>
        <el-form-item label="商品">
          <el-select
            v-model="productId"
            placeholder="全部商品"
            clearable
            filterable
            style="width: 220px"
          >
            <el-option
              v-for="item in productOptions"
              :key="item.productId"
              :label="item.productName"
              :value="item.productId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="统计维度">
          <el-radio-group v-model="groupBy">
            <el-radio-button value="product">按商品</el-radio-button>
            <el-radio-button value="month">按月</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item class="search-buttons">
          <el-button type="primary" icon="search" :loading="loading" @click="loadStat">查询</el-button>
          <el-button icon="refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 汇总指标 -->
    <el-row :gutter="12" class="metric-row">
      <el-col v-for="metric in metrics" :key="metric.label" :xs="12" :sm="12" :md="6">
        <div class="metric-card">
          <div class="metric-label">{{ metric.label }}</div>
          <div class="metric-value">
            {{ metric.value }}<span v-if="metric.unit" class="metric-unit">{{ metric.unit }}</span>
          </div>
          <div class="metric-extra">{{ metric.extra }}</div>
        </div>
      </el-col>
    </el-row>

    <!-- 走势 -->
    <el-card shadow="never" class="chart-card">
      <template #header>
        <div class="card-head">
          <span>月度销量与销售额</span>
          <span class="card-sub">{{ result.startDate }} 至 {{ result.endDate }}</span>
        </div>
      </template>
      <ECharts :options="chartOptions" height="280px" />
    </el-card>

    <!-- 明细 -->
    <el-card shadow="never" class="table-section">
      <div class="table-section__toolbar">
        <div class="table-section__toolbar--actions">
          <span class="table-title">销售明细</span>
        </div>
        <div class="table-section__toolbar--tools">
          <el-button
            v-hasPerm="'ai:analysis'"
            type="primary"
            icon="MagicStick"
            :disabled="!result.items.length"
            @click="openAnalysis()"
          >
            整店可行性分析
          </el-button>
        </div>
      </div>

      <el-table
        v-if="groupBy === 'product'"
        v-loading="loading"
        :data="result.items"
        border
        stripe
        class="table-section__content"
      >
        <el-table-column label="商品" prop="productName" min-width="220" show-overflow-tooltip>
          <template #default="scope">
            <div class="product-cell">
              <el-image
                v-if="scope.row.cover"
                :src="scope.row.cover"
                fit="cover"
                class="product-cover"
              >
                <template #error>
                  <div class="cover-fallback">{{ scope.row.productName.slice(0, 1) }}</div>
                </template>
              </el-image>
              <span>{{ scope.row.productName }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="库存" width="90" align="center">
          <template #default="scope">
            <span :class="{ 'text-danger': scope.row.stock <= 10 }">
              {{ scope.row.stock }}{{ scope.row.unit || "件" }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="销量" prop="saleQty" width="90" align="center" sortable />
        <el-table-column label="销售额(元)" width="120" align="right">
          <template #default="scope">{{ formatAmount(scope.row.saleAmount) }}</template>
        </el-table-column>
        <el-table-column label="退单" prop="refundQty" width="80" align="center" />
        <el-table-column label="退款(元)" width="100" align="right">
          <template #default="scope">{{ formatAmount(scope.row.refundAmount) }}</template>
        </el-table-column>
        <el-table-column label="月度走势" min-width="150" align="center">
          <template #default="scope">
            <el-tooltip
              :content="trendText(scope.row.trend)"
              placement="top"
              :show-after="200"
            >
              <div class="mini-trend">
                <span
                  v-for="(point, index) in scope.row.trend"
                  :key="index"
                  class="mini-bar"
                  :style="{ height: barHeight(point.qty, scope.row.trend) + 'px' }"
                />
              </div>
            </el-tooltip>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140" align="center" fixed="right">
          <template #default="scope">
            <el-button
              v-hasPerm="'ai:analysis'"
              type="primary"
              link
              icon="MagicStick"
              @click="openAnalysis(scope.row)"
            >
              AI 分析
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-table
        v-else
        v-loading="loading"
        :data="result.trend"
        border
        stripe
        class="table-section__content"
      >
        <el-table-column label="月份" prop="month" min-width="140" />
        <el-table-column label="销量" prop="qty" width="120" align="center" sortable />
        <el-table-column label="销售额(元)" width="160" align="right">
          <template #default="scope">{{ formatAmount(scope.row.amount) }}</template>
        </el-table-column>
        <el-table-column label="占比" min-width="200">
          <template #default="scope">
            <div class="share-bar">
              <div class="share-bar__inner" :style="{ width: shareWidth(scope.row.qty) }" />
            </div>
            <span class="share-text">{{ shareText(scope.row.qty) }}</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 可行性分析 -->
    <el-dialog
      v-model="analysisVisible"
      :title="analysisTitle"
      width="760px"
      top="6vh"
      destroy-on-close
    >
      <div class="analysis-bar">
        <span class="analysis-label">分析跨度</span>
        <el-select v-model="analysisMonths" style="width: 120px" size="small">
          <el-option v-for="m in [3, 6, 9, 12]" :key="m" :label="`近 ${m} 个月`" :value="m" />
        </el-select>
        <el-button
          type="primary"
          size="small"
          icon="MagicStick"
          :loading="analyzing"
          @click="runAnalysis"
        >
          {{ analysisResult ? "重新分析" : "开始分析" }}
        </el-button>
        <span class="analysis-tip">分析基于近 N 个月已支付订单，结果仅供参考</span>
      </div>

      <el-skeleton v-if="analyzing" :rows="6" animated class="analysis-skeleton" />

      <template v-else-if="analysisResult">
        <div class="answer-block">
          <div class="answer-head">
            <el-icon><Service /></el-icon>
            <span>AI 分析结论</span>
            <el-tag v-if="analysisResult.tokenCount" size="small" type="info" effect="plain">
              {{ analysisResult.tokenCount }} tokens
            </el-tag>
          </div>
          <div class="answer-text">{{ analysisResult.answer }}</div>
        </div>

        <el-table :data="analysisResult.products" border size="small" class="analysis-table">
          <el-table-column label="商品" prop="productName" min-width="180" show-overflow-tooltip />
          <el-table-column label="近 N 月销量" prop="saleQty" width="110" align="center" />
          <el-table-column label="走势" width="90" align="center">
            <template #default="scope">
              <el-tag :type="trendTagType(scope.row.trendDirection)" effect="light" size="small">
                {{ trendLabel(scope.row.trendDirection) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="规则建议" prop="suggestion" min-width="220" show-overflow-tooltip />
        </el-table>
      </template>

      <el-empty v-else description="选择分析跨度后开始分析" :image-size="72" />
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { ElMessage } from "element-plus";
import { Service } from "@element-plus/icons-vue";
import ECharts from "@/components/ECharts/index.vue";
import AIAPI, {
  type SalesAnalysisResult,
  type SalesPoint,
  type SalesStatItem,
  type SalesStatResult,
} from "@/api/ai";

const shortcuts = [
  { text: "近 7 天", value: () => rangeOf(7) },
  { text: "近 30 天", value: () => rangeOf(30) },
  { text: "近 90 天", value: () => rangeOf(90) },
  { text: "本月", value: () => monthRange() },
];

const loading = ref(false);
const groupBy = ref<"product" | "month">("product");
const dateRange = ref<[string, string] | null>(lastDays(30));
const productId = ref<number | undefined>();
const productOptions = ref<SalesStatItem[]>([]);
const result = ref<SalesStatResult>({
  startDate: "",
  endDate: "",
  groupBy: "product",
  totalQty: 0,
  totalAmount: 0,
  refundQty: 0,
  refundAmount: 0,
  productCount: 0,
  stockTotal: 0,
  items: [],
  trend: [],
});

const analysisVisible = ref(false);
const analyzing = ref(false);
const analysisMonths = ref(6);
const analysisProduct = ref<SalesStatItem | null>(null);
const analysisResult = ref<SalesAnalysisResult | null>(null);

const analysisTitle = computed(() =>
  analysisProduct.value ? `可行性分析 · ${analysisProduct.value.productName}` : "整店可行性分析"
);

const metrics = computed(() => [
  {
    label: "区间销量",
    value: result.value.totalQty,
    unit: "件",
    extra: `${result.value.startDate} 起`,
  },
  {
    label: "区间销售额",
    value: formatAmount(result.value.totalAmount),
    unit: "元",
    extra: `共 ${result.value.productCount} 个商品`,
  },
  {
    label: "退单 / 退款",
    value: `${result.value.refundQty} 件`,
    unit: "",
    extra: `退款 ${formatAmount(result.value.refundAmount)} 元`,
  },
  {
    label: "当前库存",
    value: result.value.stockTotal,
    unit: "件",
    extra: "全部在库商品合计",
  },
]);

const chartOptions = computed(() => ({
  tooltip: { trigger: "axis" },
  legend: { data: ["销量", "销售额"], right: 10, top: 4 },
  grid: { left: 48, right: 56, top: 44, bottom: 32 },
  xAxis: {
    type: "category",
    data: result.value.trend.map((item) => item.month),
    axisTick: { show: false },
  },
  yAxis: [
    { type: "value", name: "销量(件)", minInterval: 1 },
    { type: "value", name: "销售额(元)" },
  ],
  series: [
    {
      name: "销量",
      type: "bar",
      barMaxWidth: 34,
      itemStyle: { color: "#409eff", borderRadius: [4, 4, 0, 0] },
      data: result.value.trend.map((item) => item.qty),
    },
    {
      name: "销售额",
      type: "line",
      yAxisIndex: 1,
      smooth: true,
      symbolSize: 7,
      itemStyle: { color: "#f0a020" },
      lineStyle: { width: 2 },
      data: result.value.trend.map((item) => Number(item.amount)),
    },
  ],
}));

function lastDays(days: number): [string, string] {
  const end = new Date();
  const start = new Date();
  start.setDate(end.getDate() - (days - 1));
  return [fmtDate(start), fmtDate(end)];
}

function rangeOf(days: number): [string, string] {
  return lastDays(days);
}

function monthRange(): [string, string] {
  const now = new Date();
  return [fmtDate(new Date(now.getFullYear(), now.getMonth(), 1)), fmtDate(now)];
}

function fmtDate(date: Date) {
  const month = `${date.getMonth() + 1}`.padStart(2, "0");
  const day = `${date.getDate()}`.padStart(2, "0");
  return `${date.getFullYear()}-${month}-${day}`;
}

function formatAmount(value?: number) {
  return Number(value ?? 0).toLocaleString("zh-CN", {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  });
}

function trendText(trend: SalesPoint[]) {
  return trend.map((item) => `${item.month}：${item.qty} 件`).join("　");
}

function barHeight(qty: number, trend: SalesPoint[]) {
  const max = Math.max(1, ...trend.map((item) => item.qty));
  return Math.max(3, Math.round((qty / max) * 24));
}

function shareText(qty: number) {
  const total = result.value.totalQty || 1;
  return `${Math.round((qty / total) * 100)}%`;
}

function shareWidth(qty: number) {
  const total = result.value.totalQty || 1;
  return `${Math.min(100, Math.round((qty / total) * 100))}%`;
}

function trendLabel(direction: string) {
  return direction === "up" ? "上升" : direction === "down" ? "下降" : "平稳";
}

function trendTagType(direction: string) {
  return direction === "up" ? "danger" : direction === "down" ? "success" : "info";
}

async function loadStat() {
  loading.value = true;
  try {
    const [start, end] = dateRange.value ?? [];
    const data = await AIAPI.salesStat({
      productId: productId.value,
      startDate: start,
      endDate: end,
      groupBy: groupBy.value,
    });
    result.value = data;
    if (!productId.value) {
      productOptions.value = data.items;
    }
  } catch {
    // 拦截器已提示
  } finally {
    loading.value = false;
  }
}

function handleReset() {
  dateRange.value = lastDays(30);
  productId.value = undefined;
  groupBy.value = "product";
  loadStat();
}

function openAnalysis(row?: any) {
  analysisProduct.value = (row as SalesStatItem) ?? null;
  analysisResult.value = null;
  analysisMonths.value = 6;
  analysisVisible.value = true;
}

async function runAnalysis() {
  const target = analysisProduct.value ?? result.value.items[0];
  if (!target) {
    ElMessage.warning("暂无可分析的商品");
    return;
  }
  analyzing.value = true;
  try {
    analysisResult.value = await AIAPI.salesAnalysis({
      productId: target.productId,
      months: analysisMonths.value,
    });
  } catch {
    // 拦截器已提示
  } finally {
    analyzing.value = false;
  }
}

onMounted(loadStat);
</script>

<style lang="scss" scoped>
.ai-stat {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.metric-row {
  .metric-card {
    padding: 16px 18px;
    border-radius: 8px;
    background: var(--el-bg-color);
    border: 1px solid var(--el-border-color-light);
    height: 100%;
  }

  .metric-label {
    font-size: 13px;
    color: var(--el-text-color-secondary);
  }

  .metric-value {
    margin-top: 8px;
    font-size: 24px;
    font-weight: 600;
    color: var(--el-text-color-primary);
    line-height: 1.2;
  }

  .metric-unit {
    margin-left: 4px;
    font-size: 13px;
    font-weight: 400;
    color: var(--el-text-color-secondary);
  }

  .metric-extra {
    margin-top: 6px;
    font-size: 12px;
    color: var(--el-text-color-placeholder);
  }
}

.chart-card,
.table-section {
  :deep(.el-card__header) {
    padding: 12px 16px;
  }
}

.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;

  .card-sub {
    font-size: 12px;
    color: var(--el-text-color-secondary);
  }
}

.table-title {
  font-weight: 600;
}

.product-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.product-cover,
.cover-fallback {
  width: 36px;
  height: 36px;
  border-radius: 6px;
  flex-shrink: 0;
}

.cover-fallback {
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--el-fill-color-light);
  color: var(--el-text-color-secondary);
  font-size: 14px;
}

.mini-trend {
  display: flex;
  align-items: flex-end;
  justify-content: center;
  gap: 3px;
  height: 26px;
}

.mini-bar {
  width: 7px;
  border-radius: 2px 2px 0 0;
  background: var(--el-color-primary-light-3);
}

.share-bar {
  display: inline-block;
  width: 60%;
  height: 8px;
  border-radius: 4px;
  background: var(--el-fill-color-light);
  vertical-align: middle;
  overflow: hidden;

  &__inner {
    height: 100%;
    background: var(--el-color-primary-light-3);
  }
}

.share-text {
  margin-left: 8px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.text-danger {
  color: var(--el-color-danger);
  font-weight: 600;
}

.analysis-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 14px;

  .analysis-label {
    font-size: 13px;
    color: var(--el-text-color-secondary);
  }

  .analysis-tip {
    font-size: 12px;
    color: var(--el-text-color-placeholder);
  }
}

.analysis-skeleton {
  padding: 8px 4px;
}

.answer-block {
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  padding: 14px 16px;
  background: var(--el-fill-color-lighter);
  margin-bottom: 14px;
}

.answer-head {
  display: flex;
  align-items: center;
  gap: 6px;
  font-weight: 600;
  margin-bottom: 8px;
}

.answer-text {
  white-space: pre-wrap;
  line-height: 1.75;
  font-size: 14px;
  color: var(--el-text-color-primary);
}

.analysis-table {
  margin-top: 4px;
}
</style>
