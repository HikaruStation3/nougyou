<template>
  <div class="app-container">
    <el-card shadow="never" class="mb-4">
      <el-form inline @submit.prevent>
        <el-form-item label="订单号">
          <el-input
            v-model="queryParams.orderNo"
            placeholder="精确匹配订单号"
            clearable
            style="width: 200px"
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="订单状态">
          <el-select v-model="queryParams.orderStatus" placeholder="全部" clearable style="width: 140px">
            <el-option v-for="(item, key) in ORDER_STATUS" :key="key" :label="item.text" :value="Number(key)" />
          </el-select>
        </el-form-item>
        <el-form-item label="下单时间">
          <el-date-picker
            v-model="dateRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 340px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="search" @click="handleQuery">查询</el-button>
          <el-button icon="refresh" @click="handleReset">重置</el-button>
          <el-button type="success" icon="download" :loading="exporting" @click="handleExport">
            导出
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="orderList" border stripe>
        <el-table-column prop="orderNo" label="订单号" min-width="180" />
        <el-table-column label="买家" min-width="110">
          <template #default="{ row }">{{ row.buyerName || "-" }}</template>
        </el-table-column>
        <el-table-column label="商家" min-width="150">
          <template #default="{ row }">
            <div>{{ row.shopName || "-" }}</div>
            <div class="text-xs text-[--el-text-color-secondary]">{{ row.farmerName || "-" }}</div>
          </template>
        </el-table-column>
        <el-table-column label="商品" min-width="200">
          <template #default="{ row }">
            <div>{{ row.items?.[0]?.productName || "-" }}</div>
            <div v-if="(row.items?.length || 0) > 1" class="text-xs text-[--el-text-color-secondary]">
              共 {{ row.items.length }} 种商品
            </div>
          </template>
        </el-table-column>
        <el-table-column label="实付金额" width="120" align="right">
          <template #default="{ row }">￥{{ money(row.payAmount) }}</template>
        </el-table-column>
        <el-table-column label="支付状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.payStatus === 1 ? 'success' : 'warning'" effect="plain">
              {{ PAY_STATUS[row.payStatus]?.text || "-" }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="订单状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="ORDER_STATUS[row.orderStatus]?.tag || 'info'" effect="plain">
              {{ ORDER_STATUS[row.orderStatus]?.text || "-" }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="下单时间" min-width="160" />
        <el-table-column label="操作" width="90" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openDetail(row)">详情</el-button>
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

    <el-dialog v-model="detailVisible" title="订单详情" width="820px" top="6vh">
      <template v-if="currentOrder">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="订单号">{{ currentOrder.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="订单状态">
            <el-tag :type="ORDER_STATUS[currentOrder.orderStatus]?.tag || 'info'" effect="plain">
              {{ ORDER_STATUS[currentOrder.orderStatus]?.text || "-" }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="买家">{{ currentOrder.buyerName || "-" }}</el-descriptions-item>
          <el-descriptions-item label="商家">
            {{ currentOrder.shopName || "-" }}（{{ currentOrder.farmerName || "-" }}）
          </el-descriptions-item>
          <el-descriptions-item label="收货人">{{ currentOrder.receiverName }}</el-descriptions-item>
          <el-descriptions-item label="联系电话">{{ currentOrder.receiverPhone }}</el-descriptions-item>
          <el-descriptions-item label="收货地址" :span="2">
            {{ currentOrder.receiverAddress }}
          </el-descriptions-item>
          <el-descriptions-item label="买家备注" :span="2">
            {{ currentOrder.remark || "-" }}
          </el-descriptions-item>
          <el-descriptions-item label="支付方式">
            {{ PAY_TYPE[currentOrder.payType || 0] || "-" }}
          </el-descriptions-item>
          <el-descriptions-item label="支付时间">{{ currentOrder.payTime || "-" }}</el-descriptions-item>
          <el-descriptions-item label="物流公司">{{ currentOrder.logisticsCompany || "-" }}</el-descriptions-item>
          <el-descriptions-item label="物流单号">{{ currentOrder.logisticsNo || "-" }}</el-descriptions-item>
          <el-descriptions-item label="下单时间">{{ currentOrder.createTime || "-" }}</el-descriptions-item>
          <el-descriptions-item label="完成时间">{{ currentOrder.finishTime || "-" }}</el-descriptions-item>
        </el-descriptions>

        <el-table :data="currentOrder.items" border class="mt-4">
          <el-table-column prop="productName" label="商品" min-width="220" />
          <el-table-column label="单价" width="110" align="right">
            <template #default="{ row }">￥{{ money(row.price) }}</template>
          </el-table-column>
          <el-table-column prop="quantity" label="数量" width="80" align="center" />
          <el-table-column label="小计" width="120" align="right">
            <template #default="{ row }">￥{{ money(row.subtotal) }}</template>
          </el-table-column>
          <el-table-column label="委托代销" width="100" align="center">
            <template #default="{ row }">
              <el-tag v-if="row.entrustId" type="success" effect="plain" size="small">代销</el-tag>
              <span v-else>-</span>
            </template>
          </el-table-column>
        </el-table>

        <div class="mt-3 text-right">
          商品总额 ￥{{ money(currentOrder.totalAmount) }}，运费 ￥{{ money(currentOrder.freight) }}，
          <span class="text-lg font-semibold text-[--el-color-danger]">
            实付 ￥{{ money(currentOrder.payAmount) }}
          </span>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import OrderAPI, { type OrderQueryParams, type OrderVO } from "@/api/order";

const ORDER_STATUS: Record<number, { text: string; tag: "primary" | "success" | "info" | "warning" | "danger" }> = {
  0: { text: "待付款", tag: "warning" },
  1: { text: "待发货", tag: "primary" },
  2: { text: "待收货", tag: "info" },
  3: { text: "已完成", tag: "success" },
  4: { text: "已取消", tag: "info" },
  5: { text: "售后中", tag: "danger" },
};

const PAY_STATUS: Record<number, { text: string; tag: "primary" | "success" | "info" | "warning" | "danger" }> = {
  0: { text: "未支付", tag: "warning" },
  1: { text: "已支付", tag: "success" },
  2: { text: "已退款", tag: "info" },
};

const PAY_TYPE: Record<number, string> = {
  1: "微信支付",
  2: "支付宝",
  3: "模拟支付",
};

const loading = ref(false);
const exporting = ref(false);
const orderList = ref<OrderVO[]>([]);
const total = ref(0);
const dateRange = ref<[string, string] | null>(null);
const detailVisible = ref(false);
const currentOrder = ref<OrderVO | null>(null);

const queryParams = reactive<OrderQueryParams>({
  pageNum: 1,
  pageSize: 10,
  orderNo: "",
  orderStatus: undefined,
});

function money(value?: number) {
  return Number(value ?? 0).toFixed(2);
}

async function loadData() {
  loading.value = true;
  try {
    queryParams.startTime = dateRange.value?.[0];
    queryParams.endTime = dateRange.value?.[1];
    const data = await OrderAPI.getAdminPage(queryParams);
    orderList.value = data.list || [];
    total.value = data.total || 0;
  } catch {
    orderList.value = [];
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
  queryParams.orderNo = "";
  queryParams.orderStatus = undefined;
  dateRange.value = null;
  loadData();
}

/** API-060 导出：带上当前筛选条件，后端返回 xlsx 文件流 */
async function handleExport() {
  exporting.value = true;
  try {
    const response = await OrderAPI.exportAdminOrders({
      ...queryParams,
      startTime: dateRange.value?.[0],
      endTime: dateRange.value?.[1],
    });

    const contentType = String(response.headers["content-type"] || "");
    // 出错时后端返回的仍是 JSON，避免把错误信息存成表格文件
    if (contentType.includes("application/json")) {
      const text = await (response.data as Blob).text();
      const result = JSON.parse(text || "{}");
      ElMessage.error(result.msg || "导出失败");
      return;
    }

    const blob = new Blob([response.data as Blob], {
      type: "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    });
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = `订单列表_${fileStamp(new Date())}.xlsx`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    window.URL.revokeObjectURL(url);
    ElMessage.success("导出完成，已开始下载");
  } catch {
    ElMessage.error("导出失败，请稍后重试");
  } finally {
    exporting.value = false;
  }
}

function fileStamp(date: Date) {
  const pad = (value: number) => String(value).padStart(2, "0");
  return (
    `${date.getFullYear()}${pad(date.getMonth() + 1)}${pad(date.getDate())}_` +
    `${pad(date.getHours())}${pad(date.getMinutes())}${pad(date.getSeconds())}`
  );
}

async function openDetail(row: any) {
  currentOrder.value = await OrderAPI.getDetail(row.id);
  detailVisible.value = true;
}

onMounted(loadData);
</script>
