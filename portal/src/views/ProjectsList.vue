<template>
  <div class="projects-page">
    <section class="page-hero">
      <h1>农家乐项目</h1>
      <p>浏览钟山镇已审核上架的农家乐项目，发现身边的田园好去处。</p>
    </section>

    <el-card shadow="never" class="filter-card">
      <el-form inline @submit.prevent>
        <el-form-item label="项目名称">
          <el-input
            v-model="query.keyword"
            placeholder="搜索项目名称"
            clearable
            style="width: 220px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="地址">
          <el-input
            v-model="query.address"
            placeholder="按地址筛选"
            clearable
            style="width: 200px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="search" @click="handleSearch">查询</el-button>
          <el-button icon="refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <div v-loading="loading" class="project-grid">
      <el-empty v-if="!loading && !list.length" description="暂无农家乐项目" />
      <el-card
        v-for="item in list"
        :key="item.id"
        shadow="hover"
        class="project-card"
        @click="goDetail(item.id)"
      >
        <div class="project-cover">
          <el-image
            v-if="item.cover"
            :src="item.cover"
            fit="cover"
            class="cover-img"
          >
            <template #error>
              <div class="cover-placeholder">暂无封面</div>
            </template>
          </el-image>
          <div v-else class="cover-placeholder">暂无封面</div>
          <el-tag v-if="item.capacity" class="capacity-tag" effect="dark" round>
            可接待 {{ item.capacity }} 人
          </el-tag>
        </div>
        <div class="project-body">
          <h3 class="project-title">{{ item.title }}</h3>
          <div class="project-meta">
            <span class="meta-item">
              <el-icon><Shop /></el-icon>{{ item.shopName || item.farmerName || "钟山镇农户" }}
            </span>
            <span class="meta-item views">
              <el-icon><View /></el-icon>{{ item.viewCount || 0 }}
            </span>
          </div>
          <div class="project-address" v-if="item.address">
            <el-icon><LocationFilled /></el-icon>{{ item.address }}
          </div>
          <div class="project-price" v-if="item.priceDesc">{{ item.priceDesc }}</div>
        </div>
      </el-card>
    </div>

    <div class="pagination-wrap">
      <el-pagination
        v-model:current-page="query.pageNum"
        v-model:page-size="query.pageSize"
        :total="total"
        :page-sizes="[9, 12, 24]"
        layout="total, sizes, prev, pager, next"
        background
        @size-change="loadData"
        @current-change="loadData"
      />
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { LocationFilled, Shop, View } from "@element-plus/icons-vue";
import { getProjects } from "../api/project";

const router = useRouter();
const loading = ref(false);
const list = ref([]);
const total = ref(0);

const query = reactive({
  pageNum: 1,
  pageSize: 9,
  projectType: "农家乐",
  keyword: "",
  address: ""
});

async function loadData() {
  loading.value = true;
  try {
    const page = await getProjects({ ...query });
    list.value = page.list || [];
    total.value = page.total || 0;
  } catch {
    list.value = [];
    total.value = 0;
  } finally {
    loading.value = false;
  }
}

function handleSearch() {
  query.pageNum = 1;
  loadData();
}

function handleReset() {
  query.keyword = "";
  query.address = "";
  query.pageNum = 1;
  loadData();
}

function goDetail(id) {
  router.push(`/projects/${id}`);
}

onMounted(loadData);
</script>

<style scoped>
.projects-page {
  max-width: 1100px;
  margin: 0 auto;
  padding: 32px 20px 48px;
}

.page-hero {
  text-align: center;
  margin-bottom: 24px;
}

.page-hero h1 {
  margin: 0 0 10px;
  font-size: 30px;
  color: #1f3d2b;
}

.page-hero p {
  margin: 0;
  color: #5d7668;
}

.filter-card {
  border-radius: 12px;
  margin-bottom: 24px;
}

.project-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
  min-height: 200px;
}

.project-card {
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.project-card:hover {
  transform: translateY(-4px);
}

.project-cover {
  position: relative;
  height: 170px;
  margin: -20px -20px 0;
  background: #eef3f0;
  overflow: hidden;
}

.cover-img {
  width: 100%;
  height: 100%;
}

.cover-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #8a9d92;
  font-size: 13px;
}

.capacity-tag {
  position: absolute;
  right: 10px;
  bottom: 10px;
  background: rgba(31, 61, 43, 0.8);
}

.project-body {
  padding-top: 14px;
}

.project-title {
  margin: 0 0 8px;
  font-size: 17px;
  color: #1f3d2b;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.project-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 13px;
  color: #5d7668;
  margin-bottom: 8px;
}

.meta-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.meta-item.views {
  color: #8a9d92;
}

.project-address {
  font-size: 13px;
  color: #5d7668;
  display: flex;
  align-items: center;
  gap: 4px;
  margin-bottom: 6px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.project-price {
  font-size: 14px;
  font-weight: 600;
  color: #2e9e5b;
}

.pagination-wrap {
  display: flex;
  justify-content: center;
  margin-top: 28px;
}
</style>
