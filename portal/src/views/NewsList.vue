<template>
  <div class="news-page">
    <section class="page-hero">
      <span class="hero-tag">FR-004 · 乡镇资讯</span>
      <h1>钟山乡镇资讯</h1>
      <p>景点、线路、历史、文化与人文地理，先认识钟山，再走进钟山。</p>
    </section>

    <div class="toolbar">
      <div class="type-tabs">
        <button
          type="button"
          class="type-chip"
          :class="{ active: query.newsType === null }"
          @click="selectType(null)"
        >
          全部
        </button>
        <button
          v-for="type in NEWS_TYPES"
          :key="type.value"
          type="button"
          class="type-chip"
          :class="[`chip-${type.value}`, { active: query.newsType === type.value }]"
          @click="selectType(type.value)"
        >
          {{ type.label }}
        </button>
      </div>

      <el-input
        v-model="query.keyword"
        placeholder="搜索资讯标题 / 摘要"
        clearable
        class="search-input"
        :prefix-icon="Search"
        @keyup.enter="handleSearch"
        @clear="handleSearch"
      />
    </div>

    <div v-loading="loading" class="news-grid">
      <el-empty v-if="!loading && !list.length" description="暂无符合条件的资讯" />
      <article
        v-for="item in list"
        :key="item.id"
        class="news-card"
        @click="openDetail(item.id)"
      >
        <div class="cover-wrap">
          <el-image v-if="item.cover" :src="item.cover" fit="cover" class="cover-img">
            <template #error>
              <div class="cover-fallback" :class="`tone-${item.newsType}`">
                <span>{{ item.newsTypeName }}</span>
              </div>
            </template>
          </el-image>
          <div v-else class="cover-fallback" :class="`tone-${item.newsType}`">
            <span>{{ item.newsTypeName }}</span>
          </div>
          <span class="type-badge" :class="`badge-${item.newsType}`">{{ item.newsTypeName }}</span>
        </div>

        <div class="card-body">
          <h3>{{ item.title }}</h3>
          <p>{{ item.summary || "点击查看资讯正文。" }}</p>
          <div class="card-meta">
            <span v-if="item.location" class="location">
              <el-icon><Location /></el-icon>{{ item.location }}
            </span>
            <span class="meta-right">
              <el-icon><View /></el-icon>{{ item.viewCount || 0 }}
              <em>{{ String(item.publishTime || "").slice(0, 10) }}</em>
            </span>
          </div>
        </div>
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
import { Location, Search, View } from "@element-plus/icons-vue";
import { getNewsList, NEWS_TYPES } from "../api/content";

const router = useRouter();

const loading = ref(true);
const list = ref([]);
const total = ref(0);

const query = reactive({
  pageNum: 1,
  pageSize: 9,
  newsType: null,
  keyword: "",
});

async function loadList() {
  loading.value = true;
  try {
    const params = { pageNum: query.pageNum, pageSize: query.pageSize };
    if (query.newsType !== null) {
      params.newsType = query.newsType;
    }
    if (query.keyword.trim()) {
      params.keyword = query.keyword.trim();
    }
    const data = await getNewsList(params);
    list.value = data?.list || [];
    total.value = data?.total || 0;
  } finally {
    loading.value = false;
  }
}

function selectType(type) {
  query.newsType = type;
  query.pageNum = 1;
  loadList();
}

function handleSearch() {
  query.pageNum = 1;
  loadList();
}

function handlePageChange(page) {
  query.pageNum = page;
  loadList();
  window.scrollTo({ top: 0, behavior: "smooth" });
}

function openDetail(id) {
  router.push(`/news/${id}`);
}

onMounted(loadList);
</script>

<style scoped>
.news-page {
  max-width: 1120px;
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

.toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.type-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.type-chip {
  padding: 7px 16px;
  color: #5d7668;
  font-size: 13px;
  background: #f4f8f5;
  border: 1px solid transparent;
  border-radius: 999px;
  cursor: pointer;
  transition: all 0.16s ease;
}

.type-chip:hover {
  color: #1f7a48;
  background: #eaf5ee;
}

.type-chip.active {
  color: #fff;
  background: #2e9e5b;
  border-color: #2e9e5b;
}

.search-input {
  width: 260px;
}

.news-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 18px;
  min-height: 240px;
}

.news-card {
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: #fff;
  border: 1px solid var(--portal-border);
  border-radius: 16px;
  cursor: pointer;
  transition: border-color 0.18s ease, box-shadow 0.18s ease, transform 0.18s ease;
}

.news-card:hover {
  border-color: #b9ddc7;
  box-shadow: 0 14px 30px rgba(31, 61, 45, 0.1);
  transform: translateY(-2px);
}

.cover-wrap {
  position: relative;
  height: 150px;
  background: #eef8f1;
}

.cover-img {
  width: 100%;
  height: 100%;
}

.cover-fallback {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  color: #fff;
  font-size: 20px;
  font-weight: 600;
  letter-spacing: 3px;
  background: linear-gradient(135deg, #2e9e5b, #1a6440);
}

.cover-fallback.tone-2 {
  background: linear-gradient(135deg, #3d84c6, #1f4f7a);
}

.cover-fallback.tone-3 {
  background: linear-gradient(135deg, #c68b3d, #7a521f);
}

.cover-fallback.tone-4 {
  background: linear-gradient(135deg, #a75a8b, #5f2c4d);
}

.cover-fallback.tone-5 {
  background: linear-gradient(135deg, #3aa6a0, #1d5f5b);
}

.type-badge {
  position: absolute;
  top: 12px;
  left: 12px;
  padding: 3px 10px;
  color: #fff;
  font-size: 12px;
  background: rgba(0, 0, 0, 0.42);
  border-radius: 999px;
  backdrop-filter: blur(4px);
}

.card-body {
  display: flex;
  flex: 1;
  flex-direction: column;
  padding: 16px 18px 14px;
}

.card-body h3 {
  display: -webkit-box;
  margin: 0;
  overflow: hidden;
  color: #1f2d24;
  font-size: 16px;
  line-height: 1.5;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.card-body p {
  display: -webkit-box;
  margin: 8px 0 14px;
  overflow: hidden;
  color: #6b7c72;
  font-size: 13px;
  line-height: 1.7;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.card-meta {
  display: flex;
  gap: 10px;
  align-items: center;
  justify-content: space-between;
  margin-top: auto;
  color: #8a9a90;
  font-size: 12px;
}

.location {
  display: inline-flex;
  gap: 4px;
  align-items: center;
  overflow: hidden;
  max-width: 55%;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.meta-right {
  display: inline-flex;
  gap: 5px;
  align-items: center;
  flex-shrink: 0;
}

.meta-right em {
  margin-left: 4px;
  font-style: normal;
  color: #a7b5ac;
}

.pager {
  display: flex;
  justify-content: center;
  margin-top: 26px;
}

@media (max-width: 960px) {
  .news-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 640px) {
  .news-grid {
    grid-template-columns: 1fr;
  }

  .search-input {
    width: 100%;
  }
}
</style>
