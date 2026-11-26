<template>
  <div class="after-sales-page">
    <section class="page-hero">
      <h1>我的售后</h1>
      <p>查看退款、退货、换货与咨询的处理进度；对已完成的订单可在订单详情里按商品发起售后。</p>
    </section>

    <el-card shadow="never" class="list-card">
      <el-tabs v-model="activeStatus" @tab-change="handleTabChange">
        <el-tab-pane label="全部" name="all" />
        <el-tab-pane v-for="(item, key) in AFTER_SALE_STATUS" :key="key" :label="item.text" :name="String(key)" />
      </el-tabs>

      <div class="toolbar">
        <el-input
          v-model="keyword"
          placeholder="搜索售后单号、订单号或商品名"
          clearable
          style="width: 300px"
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        />
        <el-button type="primary" @click="handleSearch">搜索</el-button>
        <el-button @click="router.push('/orders')">返回订单</el-button>
      </div>

      <div v-loading="loading">
        <el-empty v-if="!loading && !records.length" description="暂无售后记录">
          <el-button type="primary" @click="router.push('/orders')">去订单页申请售后</el-button>
        </el-empty>

        <div v-for="record in records" :key="record.id" class="as-card">
          <div class="as-head">
            <span class="as-no">{{ record.afterSaleNo }}</span>
            <el-tag :type="saleTypeTag(record.saleType)" effect="plain" size="small">
              {{ saleTypeText(record.saleType) }}
            </el-tag>
            <span class="as-time">{{ record.createTime }}</span>
            <el-tag :type="afterSaleStatusTag(record.status)" class="as-status">
              {{ afterSaleStatusText(record.status) }}
            </el-tag>
          </div>

          <div class="as-body">
            <el-image
              v-if="record.productCover"
              :src="record.productCover"
              fit="cover"
              class="as-cover"
            />
            <div class="as-info">
              <div class="as-product">{{ record.productName || "-" }}</div>
              <div class="as-meta">
                订单号 {{ record.orderNo || "-" }}
                <span class="dot">·</span>
                店铺 {{ record.shopName || record.farmerName || "-" }}
              </div>
              <div class="as-reason">{{ record.reason || record.description || "-" }}</div>
            </div>
            <div class="as-amount">
              <div class="label">{{ record.saleType === 1 || record.saleType === 2 ? "申请退款" : "退款金额" }}</div>
              <div class="value">￥{{ money(record.refundAmount) }}</div>
            </div>
          </div>

          <div class="as-foot">
            <span v-if="record.handleRemark" class="as-remark">
              处理意见：{{ record.handleRemark }}
            </span>
            <span v-else class="as-remark muted">商家尚未处理，请耐心等待</span>
            <el-button size="small" @click="openDetail(record)">查看详情</el-button>
          </div>
        </div>

        <div v-if="total > 0" class="pager">
          <el-pagination
            v-model:current-page="pageNum"
            v-model:page-size="pageSize"
            :total="total"
            :page-sizes="[5, 10, 20]"
            layout="total, sizes, prev, pager, next"
            background
            @size-change="loadData"
            @current-change="loadData"
          />
        </div>
      </div>
    </el-card>

    <el-dialog v-model="detailVisible" title="售后详情" width="680px" top="8vh">
      <template v-if="current">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="售后单号">{{ current.afterSaleNo }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="afterSaleStatusTag(current.status)" effect="plain">
              {{ afterSaleStatusText(current.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="售后类型">{{ saleTypeText(current.saleType) }}</el-descriptions-item>
          <el-descriptions-item label="申请时间">{{ current.createTime }}</el-descriptions-item>
          <el-descriptions-item label="订单号">{{ current.orderNo || "-" }}</el-descriptions-item>
          <el-descriptions-item label="店铺">{{ current.shopName || current.farmerName || "-" }}</el-descriptions-item>
          <el-descriptions-item label="商品" :span="2">{{ current.productName || "-" }}</el-descriptions-item>
          <el-descriptions-item label="申请原因" :span="2">{{ current.reason || "-" }}</el-descriptions-item>
          <el-descriptions-item label="问题描述" :span="2">{{ current.description || "-" }}</el-descriptions-item>
          <el-descriptions-item label="退款金额">
            <span class="amount">￥{{ money(current.refundAmount) }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="处理时间">{{ current.handleTime || "-" }}</el-descriptions-item>
          <el-descriptions-item label="处理人">{{ current.handleUserName || "-" }}</el-descriptions-item>
          <el-descriptions-item label="处理意见">{{ current.handleRemark || "-" }}</el-descriptions-item>
        </el-descriptions>

        <div v-if="current.images?.length" class="proof-block">
          <div class="proof-title">凭证图片</div>
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
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { getMyAfterSales } from "../api/afterSale";
import { money } from "../utils/format";
import {
  AFTER_SALE_STATUS,
  afterSaleStatusTag,
  afterSaleStatusText,
  saleTypeTag,
  saleTypeText,
} from "../utils/afterSale";

const route = useRoute();
const router = useRouter();

const loading = ref(false);
const records = ref([]);
const total = ref(0);
const pageNum = ref(1);
const pageSize = ref(5);
const keyword = ref("");
const activeStatus = ref(route.query.status === undefined ? "all" : String(route.query.status));

const detailVisible = ref(false);
const current = ref(null);

async function loadData() {
  loading.value = true;
  try {
    const params = { pageNum: pageNum.value, pageSize: pageSize.value };
    if (activeStatus.value !== "all") {
      params.status = Number(activeStatus.value);
    }
    if (keyword.value.trim()) {
      params.keyword = keyword.value.trim();
    }
    const data = await getMyAfterSales(params);
    records.value = data?.list || [];
    total.value = data?.total || 0;
  } catch {
    records.value = [];
    total.value = 0;
  } finally {
    loading.value = false;
  }
}

function handleTabChange() {
  pageNum.value = 1;
  router.replace({
    path: "/after-sales",
    query: activeStatus.value === "all" ? {} : { status: activeStatus.value },
  });
  loadData();
}

function handleSearch() {
  pageNum.value = 1;
  loadData();
}

function openDetail(record) {
  current.value = record;
  detailVisible.value = true;
}

onMounted(loadData);
</script>

<style scoped>
.after-sales-page {
  max-width: 1000px;
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

.list-card {
  border-radius: 12px;
}

.toolbar {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
}

.as-card {
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 10px;
  margin-bottom: 14px;
  overflow: hidden;
}

.as-head {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  background: #f5f8f6;
  font-size: 13px;
  color: #5d7668;
}

.as-no {
  font-weight: 600;
  color: #1f3d2b;
}

.as-time {
  margin-left: auto;
  font-size: 12px;
  color: #8a9d92;
}

.as-body {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px;
}

.as-cover {
  width: 60px;
  height: 60px;
  border-radius: 6px;
  flex-shrink: 0;
}

.as-info {
  flex: 1;
  min-width: 0;
}

.as-product {
  font-weight: 500;
  color: #1f3d2b;
}

.as-meta {
  margin-top: 4px;
  font-size: 12px;
  color: #8a9d92;
}

.dot {
  margin: 0 4px;
}

.as-reason {
  margin-top: 6px;
  font-size: 13px;
  color: #5d7668;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.as-amount {
  text-align: right;
}

.as-amount .label {
  font-size: 12px;
  color: #8a9d92;
}

.as-amount .value {
  margin-top: 4px;
  font-size: 18px;
  font-weight: 700;
  color: #e05b4c;
}

.as-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 14px;
  border-top: 1px solid var(--el-border-color-lighter);
}

.as-remark {
  font-size: 12px;
  color: #5d7668;
}

.as-remark.muted {
  color: #8a9d92;
}

.amount {
  font-weight: 600;
  color: #e05b4c;
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

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 8px;
}
</style>
