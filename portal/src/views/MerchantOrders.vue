<template>
  <div class="merchant-orders-page">
    <section class="page-hero">
      <h1>商家订单</h1>
      <p>查看本店订单并及时发货，发货后买家即可确认收货。</p>
    </section>

    <el-card shadow="never" class="table-card">
      <div class="toolbar">
        <el-select v-model="status" placeholder="全部状态" clearable style="width: 150px" @change="handleSearch">
          <el-option label="待付款" :value="0" />
          <el-option label="待发货" :value="1" />
          <el-option label="待收货" :value="2" />
          <el-option label="已完成" :value="3" />
          <el-option label="已取消" :value="4" />
        </el-select>
        <el-input
          v-model="keyword"
          placeholder="搜索订单号或买家姓名"
          clearable
          style="width: 240px"
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        />
        <el-button type="primary" @click="handleSearch">查询</el-button>
      </div>

      <el-table v-loading="loading" :data="orders" border stripe>
        <el-table-column prop="orderNo" label="订单号" min-width="180" />
        <el-table-column label="买家" min-width="120">
          <template #default="{ row }">{{ row.buyerName || "-" }}</template>
        </el-table-column>
        <el-table-column label="商品" min-width="220">
          <template #default="{ row }">
            <div class="goods-summary">
              <span>{{ row.items?.[0]?.productName || "-" }}</span>
              <el-tag v-if="(row.items?.length || 0) > 1" size="small" effect="plain">
                等 {{ row.items.length }} 种
              </el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="件数" width="80" align="center">
          <template #default="{ row }">{{ itemCount(row) }}</template>
        </el-table-column>
        <el-table-column label="金额" width="120" align="right">
          <template #default="{ row }">
            <span class="amount">￥{{ money(row.payAmount) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="orderStatusTag(row.orderStatus)" effect="plain">
              {{ orderStatusText(row.orderStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="下单时间" min-width="160" />
        <el-table-column label="操作" width="150" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openDetail(row)">详情</el-button>
            <el-button v-if="row.orderStatus === 1" type="success" link @click="openShip(row)">
              发货
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
          @size-change="loadOrders"
          @current-change="loadOrders"
        />
      </div>
    </el-card>

    <el-dialog v-model="detailVisible" title="订单详情" width="760px" top="6vh">
      <template v-if="current">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="订单号">{{ current.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="orderStatusTag(current.orderStatus)" effect="plain">
              {{ orderStatusText(current.orderStatus) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="买家">{{ current.buyerName || "-" }}</el-descriptions-item>
          <el-descriptions-item label="买家电话">{{ current.receiverPhone }}</el-descriptions-item>
          <el-descriptions-item label="收货人">{{ current.receiverName }}</el-descriptions-item>
          <el-descriptions-item label="下单时间">{{ current.createTime }}</el-descriptions-item>
          <el-descriptions-item label="收货地址" :span="2">
            {{ current.receiverAddress }}
          </el-descriptions-item>
          <el-descriptions-item label="买家备注" :span="2">
            {{ current.remark || "-" }}
          </el-descriptions-item>
          <el-descriptions-item v-if="current.logisticsNo" label="物流" :span="2">
            {{ current.logisticsCompany }} · {{ current.logisticsNo }}
          </el-descriptions-item>
        </el-descriptions>

        <el-table :data="current.items" border class="detail-table">
          <el-table-column prop="productName" label="商品" min-width="200" />
          <el-table-column label="单价" width="110" align="right">
            <template #default="{ row }">￥{{ money(row.price) }}</template>
          </el-table-column>
          <el-table-column prop="quantity" label="数量" width="80" align="center" />
          <el-table-column label="小计" width="120" align="right">
            <template #default="{ row }">￥{{ money(row.subtotal) }}</template>
          </el-table-column>
        </el-table>

        <div class="dialog-total">
          实付金额：<span class="amount">￥{{ money(current.payAmount) }}</span>
        </div>
      </template>
    </el-dialog>

    <el-dialog v-model="shipVisible" title="订单发货" width="520px">
      <el-form :model="shipForm" label-width="90px">
        <el-form-item label="物流公司" required>
          <el-input v-model="shipForm.logisticsCompany" maxlength="50" placeholder="如 顺丰速运" />
        </el-form-item>
        <el-form-item label="物流单号" required>
          <el-input v-model="shipForm.logisticsNo" maxlength="50" placeholder="请输入物流单号" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="shipVisible = false">取消</el-button>
        <el-button type="primary" :loading="shipping" @click="handleShip">确认发货</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import { getFarmerOrders, shipOrder } from "../api/order";
import { money } from "../utils/format";
import { orderStatusTag, orderStatusText } from "../utils/order";

const loading = ref(false);
const shipping = ref(false);
const orders = ref([]);
const total = ref(0);
const pageNum = ref(1);
const pageSize = ref(10);
const keyword = ref("");
const status = ref(null);

const detailVisible = ref(false);
const current = ref(null);
const shipVisible = ref(false);
const shipTarget = ref(null);
const shipForm = reactive({ logisticsCompany: "", logisticsNo: "" });

function itemCount(order) {
  return (order.items || []).reduce((sum, item) => sum + (item.quantity || 0), 0);
}

async function loadOrders() {
  loading.value = true;
  try {
    const params = { pageNum: pageNum.value, pageSize: pageSize.value };
    if (status.value !== null && status.value !== "") params.orderStatus = status.value;
    if (keyword.value.trim()) params.keyword = keyword.value.trim();
    const data = await getFarmerOrders(params);
    orders.value = data?.list || [];
    total.value = data?.total || 0;
  } catch {
    orders.value = [];
    total.value = 0;
  } finally {
    loading.value = false;
  }
}

function handleSearch() {
  pageNum.value = 1;
  loadOrders();
}

function openDetail(row) {
  current.value = row;
  detailVisible.value = true;
}

function openShip(row) {
  shipTarget.value = row;
  shipForm.logisticsCompany = "顺丰速运";
  shipForm.logisticsNo = "";
  shipVisible.value = true;
}

async function handleShip() {
  if (!shipForm.logisticsCompany.trim() || !shipForm.logisticsNo.trim()) {
    ElMessage.warning("请填写物流公司与物流单号");
    return;
  }
  shipping.value = true;
  try {
    await shipOrder(shipTarget.value.id, {
      logisticsCompany: shipForm.logisticsCompany.trim(),
      logisticsNo: shipForm.logisticsNo.trim(),
    });
    ElMessage.success("发货成功");
    shipVisible.value = false;
    await loadOrders();
  } finally {
    shipping.value = false;
  }
}

onMounted(loadOrders);
</script>

<style scoped>
.merchant-orders-page {
  max-width: 1100px;
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

.table-card {
  border-radius: 12px;
}

.toolbar {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
}

.goods-summary {
  display: flex;
  align-items: center;
  gap: 6px;
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

.detail-table {
  margin-top: 16px;
}

.dialog-total {
  margin-top: 12px;
  text-align: right;
  color: #5d7668;
}
</style>
