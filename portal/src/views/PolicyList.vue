<template>
  <div class="policy-page">
    <section class="page-hero">
      <span class="hero-tag">FR-003 · 扶贫政策</span>
      <h1>扶贫政策专栏</h1>
      <p>产业奖补、贴息贷款、公益岗位与电商扶持，政策原文一站查阅。</p>
    </section>

    <el-card shadow="never" class="filter-card">
      <el-form inline @submit.prevent>
        <el-form-item label="关键词">
          <el-input
            v-model="query.keyword"
            placeholder="政策标题 / 文号"
            clearable
            style="width: 220px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="发布单位">
          <el-input
            v-model="query.issueDept"
            placeholder="如 钟山镇人民政府"
            clearable
            style="width: 220px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <div v-loading="loading" class="policy-list">
      <el-empty v-if="!loading && !list.length" description="暂无符合条件的政策" />
      <article
        v-for="item in list"
        :key="item.id"
        class="policy-card"
        @click="openDetail(item.id)"
      >
        <div class="date-block">
          <strong>{{ dayOf(item.publishTime) }}</strong>
          <span>{{ monthOf(item.publishTime) }}</span>
        </div>

        <div class="policy-main">
          <div class="policy-title-row">
            <h3>{{ item.title }}</h3>
            <el-tag v-if="item.policyNo" size="small" effect="plain" type="success">
              {{ item.policyNo }}
            </el-tag>
          </div>
          <p>{{ item.summary || "点击查看政策正文。" }}</p>
          <div class="policy-meta">
            <span>
              <el-icon><OfficeBuilding /></el-icon>
              {{ item.issueDept || "钟山镇人民政府" }}
            </span>
            <span>
              <el-icon><View /></el-icon>
              {{ item.viewCount || 0 }} 次浏览
            </span>
          </div>
        </div>

        <el-icon class="card-arrow"><ArrowRight /></el-icon>
      </article>
    </div>

    <div v-if="total > query.pageSize" class="pager">
      <el-pagination
        background
        layout="prev, pager, next, total"
        :total="total"
        :current-page="query.pageNum"
        :page-size="query.pageSize"
        @current-change="handlePageChange"
      />
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { ArrowRight, OfficeBuilding, Refresh, Search, View } from "@element-plus/icons-vue";
import { getPolicies } from "../api/content";

const router = useRouter();

const loading = ref(true);
const list = ref([]);
const total = ref(0);

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: "",
  issueDept: "",
});

function dayOf(value) {
  return value ? String(value).slice(8, 10) : "--";
}

function monthOf(value) {
  return value ? String(value).slice(0, 7) : "--";
}

async function loadList() {
  loading.value = true;
  try {
    const data = await getPolicies({ ...query });
    list.value = data?.list || [];
    total.value = data?.total || 0;
  } finally {
    loading.value = false;
  }
}

function handleSearch() {
  query.pageNum = 1;
  loadList();
}

function handleReset() {
  query.keyword = "";
  query.issueDept = "";
  query.pageNum = 1;
  loadList();
}

function handlePageChange(page) {
  query.pageNum = page;
  loadList();
  window.scrollTo({ top: 0, behavior: "smooth" });
}

function openDetail(id) {
  router.push(`/policies/${id}`);
}

onMounted(loadList);
</script>

<style scoped>
.policy-page {
  max-width: 1080px;
  margin: 0 auto;
  padding: 32px 20px 56px;
}

.page-hero {
  text-align: center;
  margin-bottom: 22px;
}

.hero-tag {
  display: inline-block;
  padding: 4px 12px;
  color: #257e48;
  font-size: 12px;
  background: #eef8f1;
  border-radius: 999px;
}

.page-hero h1 {
  margin: 12px 0 8px;
  color: #1f3d2b;
  font-size: 30px;
}

.page-hero p {
  margin: 0;
  color: #5d7668;
}

.filter-card {
  margin-bottom: 18px;
  border-radius: 12px;
}

.filter-card :deep(.el-form-item) {
  margin-bottom: 0;
}

.policy-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-height: 220px;
}

.policy-card {
  display: flex;
  gap: 18px;
  align-items: center;
  padding: 20px 22px;
  background: #fff;
  border: 1px solid var(--portal-border);
  border-radius: 14px;
  cursor: pointer;
  transition: border-color 0.18s ease, box-shadow 0.18s ease, transform 0.18s ease;
}

.policy-card:hover {
  border-color: #b9ddc7;
  box-shadow: 0 10px 26px rgba(31, 61, 45, 0.09);
  transform: translateY(-1px);
}

.date-block {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 66px;
  height: 66px;
  flex-shrink: 0;
  color: #1f7a48;
  background: #eef8f1;
  border-radius: 12px;
}

.date-block strong {
  font-size: 22px;
  line-height: 1.1;
}

.date-block span {
  margin-top: 2px;
  font-size: 11px;
  color: #5d7668;
}

.policy-main {
  flex: 1;
  min-width: 0;
}

.policy-title-row {
  display: flex;
  gap: 10px;
  align-items: center;
}

.policy-title-row h3 {
  margin: 0;
  overflow: hidden;
  color: #1f2d24;
  font-size: 17px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.policy-main p {
  display: -webkit-box;
  margin: 8px 0 10px;
  overflow: hidden;
  color: #6b7c72;
  font-size: 13px;
  line-height: 1.7;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.policy-meta {
  display: flex;
  gap: 18px;
  color: #8a9a90;
  font-size: 12px;
}

.policy-meta span {
  display: inline-flex;
  gap: 4px;
  align-items: center;
}

.card-arrow {
  flex-shrink: 0;
  color: #b3c5b9;
}

.policy-card:hover .card-arrow {
  color: #2e9e5b;
}

.pager {
  display: flex;
  justify-content: center;
  margin-top: 26px;
}
</style>
