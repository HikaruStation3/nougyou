<template>
  <div class="entrust-page">
    <section class="page-hero">
      <div>
        <h1>委托销售</h1>
        <p>把不便自主经营的商品委托给同村农户代销，按约定比例分成，销售数据双方实时可见。</p>
      </div>
      <el-button type="primary" @click="openCreate">建立委托关系</el-button>
    </section>

    <el-card shadow="never" class="table-card">
      <el-tabs v-model="tab" @tab-change="handleTabChange">
        <el-tab-pane label="我委托出去的" name="sent" />
        <el-tab-pane label="我代销的" name="received" />
      </el-tabs>

      <div class="toolbar">
        <el-select v-model="status" placeholder="全部状态" clearable style="width: 160px" @change="handleSearch">
          <el-option label="待确认" :value="0" />
          <el-option label="生效中" :value="1" />
          <el-option label="已终止" :value="2" />
          <el-option label="已拒绝" :value="3" />
        </el-select>
        <el-button type="primary" @click="handleSearch">查询</el-button>
      </div>

      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column prop="entrustNo" label="委托编号" min-width="170" />
        <el-table-column label="商品" min-width="200">
          <template #default="{ row }">
            <div class="goods-cell">
              <el-image v-if="row.productCover" :src="row.productCover" fit="cover" class="goods-thumb" />
              <span>{{ row.productName || "-" }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="委托方" min-width="110">
          <template #default="{ row }">{{ row.entrustUserName || "-" }}</template>
        </el-table-column>
        <el-table-column label="受托方" min-width="110">
          <template #default="{ row }">{{ row.agentUserName || "-" }}</template>
        </el-table-column>
        <el-table-column label="受托方分成" width="110" align="center">
          <template #default="{ row }">{{ Number(row.splitRatio || 0).toFixed(0) }}%</template>
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
        <el-table-column label="操作" width="230" align="center" fixed="right">
          <template #default="{ row }">
            <template v-if="tab === 'received' && row.status === 0">
              <el-button type="success" link @click="handleAccept(row)">接受</el-button>
              <el-button type="danger" link @click="handleReject(row)">拒绝</el-button>
            </template>
            <el-button type="primary" link @click="openStat(row)">统计</el-button>
            <el-button type="primary" link @click="openSettlements(row)">结算单</el-button>
            <el-button
              v-if="row.status === 0 || row.status === 1"
              type="danger"
              link
              @click="handleTerminate(row)"
              >终止</el-button
            >
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
          @size-change="loadList"
          @current-change="loadList"
        />
      </div>
    </el-card>

    <el-dialog v-model="createVisible" title="建立委托关系" width="620px" top="6vh">
      <el-form :model="createForm" label-width="110px">
        <el-form-item label="被委托商品" required>
          <el-select v-model="createForm.productId" placeholder="请选择本店商品" filterable style="width: 100%">
            <el-option
              v-for="item in myProducts"
              :key="item.id"
              :label="`${item.name}（库存 ${item.stock}）`"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="受托方" required>
          <el-select v-model="createForm.agentUserId" placeholder="请选择同村农户商家" filterable style="width: 100%">
            <el-option
              v-for="agent in agents"
              :key="agent.id"
              :label="agent.shopName ? `${agent.name}（${agent.shopName}）` : agent.name"
              :value="agent.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="受托方分成" required>
          <el-input-number v-model="createForm.splitRatio" :min="0" :max="100" :precision="2" />
          <span class="unit-tip">% ，剩余归委托方</span>
        </el-form-item>
        <el-form-item label="结算周期" required>
          <el-radio-group v-model="createForm.settleCycle">
            <el-radio :value="1">按月</el-radio>
            <el-radio :value="2">按季</el-radio>
            <el-radio :value="3">按单</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="生效开始" required>
          <el-date-picker v-model="createForm.startDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="生效结束">
          <el-date-picker
            v-model="createForm.endDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="留空表示长期"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="约定说明">
          <el-input
            v-model="createForm.agreementDesc"
            type="textarea"
            :rows="3"
            maxlength="500"
            show-word-limit
            placeholder="如：受托方负责接单发货，按月结算"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="handleCreate">提交</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="statVisible" title="委托销售统计" width="640px">
      <template v-if="stat">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="商品">{{ stat.productName }}</el-descriptions-item>
          <el-descriptions-item label="委托编号">{{ stat.entrustNo }}</el-descriptions-item>
          <el-descriptions-item label="委托方">{{ stat.entrustUserName }}</el-descriptions-item>
          <el-descriptions-item label="受托方">{{ stat.agentUserName }}</el-descriptions-item>
          <el-descriptions-item label="当前库存">{{ stat.stock ?? 0 }}</el-descriptions-item>
          <el-descriptions-item label="商品累计销量">{{ stat.totalSaleCount ?? 0 }}</el-descriptions-item>
          <el-descriptions-item label="代销销量">{{ stat.saleQty ?? 0 }}</el-descriptions-item>
          <el-descriptions-item label="代销销售额">￥{{ money(stat.saleAmount) }}</el-descriptions-item>
          <el-descriptions-item label="退单件数">{{ stat.refundQty ?? 0 }}</el-descriptions-item>
          <el-descriptions-item label="退款金额">￥{{ money(stat.refundAmount) }}</el-descriptions-item>
          <el-descriptions-item label="委托方分成收入">￥{{ money(stat.entrustIncome) }}</el-descriptions-item>
          <el-descriptions-item label="受托方分成收入">￥{{ money(stat.agentIncome) }}</el-descriptions-item>
        </el-descriptions>
        <p class="stat-tip">
          统计数据来自视图 v_entrust_sale_stat，委托方与受托方看到完全一致的结果。
        </p>
      </template>
    </el-dialog>

    <el-dialog v-model="settlementVisible" title="委托结算单" width="760px">
      <el-empty v-if="!settlements.length" description="暂无结算单，待管理员按月生成" />
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
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import {
  confirmEntrust,
  createEntrust,
  getEntrustAgents,
  getEntrustSettlements,
  getEntrustStat,
  getReceivedEntrusts,
  getSentEntrusts,
  terminateEntrust,
} from "../api/entrust";
import { getMyProducts } from "../api/product";
import { money } from "../utils/format";
import { ENTRUST_STATUS, SETTLE_CYCLE } from "../utils/order";

const loading = ref(false);
const creating = ref(false);
const tab = ref("sent");
const status = ref(null);
const list = ref([]);
const total = ref(0);
const pageNum = ref(1);
const pageSize = ref(10);

const createVisible = ref(false);
const myProducts = ref([]);
const agents = ref([]);
const createForm = reactive({
  productId: null,
  agentUserId: null,
  splitRatio: 20,
  settleCycle: 1,
  startDate: "",
  endDate: "",
  agreementDesc: "",
});

const statVisible = ref(false);
const stat = ref(null);

const settlementVisible = ref(false);
const settlements = ref([]);

async function loadList() {
  loading.value = true;
  try {
    const params = { pageNum: pageNum.value, pageSize: pageSize.value };
    if (status.value !== null && status.value !== "") params.status = status.value;
    const data =
      tab.value === "sent"
        ? await getSentEntrusts(params)
        : await getReceivedEntrusts(params);
    list.value = data?.list || [];
    total.value = data?.total || 0;
  } catch {
    list.value = [];
    total.value = 0;
  } finally {
    loading.value = false;
  }
}

function handleTabChange() {
  pageNum.value = 1;
  loadList();
}

function handleSearch() {
  pageNum.value = 1;
  loadList();
}

async function openCreate() {
  const now = new Date();
  const pad = (value) => String(value).padStart(2, "0");
  createForm.productId = null;
  createForm.agentUserId = null;
  createForm.splitRatio = 20;
  createForm.settleCycle = 1;
  createForm.startDate = `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}`;
  createForm.endDate = "";
  createForm.agreementDesc = "";
  createVisible.value = true;
  try {
    const [products, options] = await Promise.all([
      getMyProducts({ pageNum: 1, pageSize: 100 }),
      getEntrustAgents(),
    ]);
    myProducts.value = products?.list || [];
    agents.value = options || [];
    if (!agents.value.length) {
      ElMessage.warning("暂无可选的受托方，请先确认已有其他农户商家入驻");
    }
  } catch {
    myProducts.value = [];
    agents.value = [];
  }
}

function validateCreate() {
  if (!createForm.productId) return "请选择被委托商品";
  if (!createForm.agentUserId) return "请选择受托方";
  if (!createForm.startDate) return "请选择生效开始日期";
  if (
    createForm.endDate &&
    new Date(createForm.endDate) < new Date(createForm.startDate)
  ) {
    return "结束日期不能早于开始日期";
  }
  return "";
}

async function handleCreate() {
  const error = validateCreate();
  if (error) {
    ElMessage.warning(error);
    return;
  }
  creating.value = true;
  try {
    await createEntrust({
      productId: Number(createForm.productId),
      agentUserId: Number(createForm.agentUserId),
      splitRatio: Number(createForm.splitRatio),
      settleCycle: createForm.settleCycle,
      startDate: createForm.startDate,
      endDate: createForm.endDate || null,
      agreementDesc: createForm.agreementDesc.trim() || null,
    });
    ElMessage.success("委托关系已发起，等待受托方确认");
    createVisible.value = false;
    tab.value = "sent";
    await loadList();
  } finally {
    creating.value = false;
  }
}

async function handleAccept(row) {
  try {
    await ElMessageBox.confirm(`确认接受「${row.productName}」的代销委托？`, "接受委托", {
      type: "info",
    });
  } catch {
    return;
  }
  await confirmEntrust(row.id, { agree: true });
  ElMessage.success("已接受委托，关系即时生效");
  await loadList();
}

async function handleReject(row) {
  let remark = "";
  try {
    const result = await ElMessageBox.prompt("请填写拒绝原因", "拒绝委托", {
      inputPlaceholder: "如：近期人手不足",
      inputValidator: (value) => (value && value.trim() ? true : "请填写原因"),
    });
    remark = result.value;
  } catch {
    return;
  }
  await confirmEntrust(row.id, { agree: false, remark });
  ElMessage.success("已拒绝该委托");
  await loadList();
}

async function handleTerminate(row) {
  let reason = "";
  try {
    const result = await ElMessageBox.prompt("终止后不再统计新订单，请填写原因", "终止委托", {
      inputPlaceholder: "如：商品已售完",
    });
    reason = result.value || "";
  } catch {
    return;
  }
  await terminateEntrust(row.id, { reason });
  ElMessage.success("委托关系已终止");
  await loadList();
}

async function openStat(row) {
  try {
    stat.value = await getEntrustStat(row.id);
    statVisible.value = true;
  } catch {
    stat.value = null;
  }
}

async function openSettlements(row) {
  const data = await getEntrustSettlements(row.id, { pageNum: 1, pageSize: 12 });
  settlements.value = data?.list || [];
  settlementVisible.value = true;
}

onMounted(loadList);
</script>

<style scoped>
.entrust-page {
  max-width: 1180px;
  margin: 0 auto;
  padding: 32px 20px 48px;
}

.page-hero {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
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
  gap: 10px;
}

.goods-thumb {
  width: 44px;
  height: 44px;
  border-radius: 6px;
  flex-shrink: 0;
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.unit-tip {
  margin-left: 8px;
  color: #8a9d92;
  font-size: 13px;
}

.stat-tip {
  margin: 12px 0 0;
  font-size: 12px;
  color: #8a9d92;
}
</style>
