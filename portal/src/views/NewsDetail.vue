<template>
  <div class="detail-page">
    <div class="back-bar">
      <el-button text @click="goBack">
        <el-icon><ArrowLeft /></el-icon> 返回资讯列表
      </el-button>
    </div>

    <el-skeleton v-if="loading" :rows="9" animated />

    <template v-else-if="news">
      <article class="article-card">
        <div class="cover-wrap">
          <el-image v-if="news.cover" :src="news.cover" fit="cover" class="cover-img">
            <template #error>
              <div class="cover-fallback" :class="`tone-${news.newsType}`">
                <span class="cover-type">{{ news.newsTypeName }}</span>
                <span class="cover-sub">钟山镇乡镇资讯</span>
              </div>
            </template>
          </el-image>
          <div v-else class="cover-fallback" :class="`tone-${news.newsType}`">
            <span class="cover-type">{{ news.newsTypeName }}</span>
            <span class="cover-sub">钟山镇乡镇资讯</span>
          </div>
        </div>

        <header class="article-head">
          <div class="article-tags">
            <el-tag type="success" effect="dark" round size="small">{{ news.newsTypeName }}</el-tag>
            <span v-if="news.location" class="location">
              <el-icon><Location /></el-icon>{{ news.location }}
            </span>
          </div>
          <h1>{{ news.title }}</h1>
          <div class="article-meta">
            <span>
              <el-icon><Clock /></el-icon>
              {{ formatTime(news.publishTime) }}
            </span>
            <span>
              <el-icon><View /></el-icon>
              {{ news.viewCount || 0 }} 次浏览
            </span>
          </div>
        </header>

        <p v-if="news.summary" class="lead">{{ news.summary }}</p>

        <div class="article-body">
          <p v-for="(text, idx) in paragraphs" :key="idx">{{ text }}</p>
          <el-empty v-if="!paragraphs.length" :image-size="80" description="正文暂未录入" />
        </div>

        <footer class="article-foot">
          <span class="foot-note">内容来源：钟山镇人民政府 · 乡镇资讯专栏</span>
          <div class="foot-actions">
            <el-button @click="router.push('/news')">更多资讯</el-button>
            <el-button type="primary" plain @click="router.push('/policies')">扶贫政策</el-button>
          </div>
        </footer>
      </article>
    </template>

    <el-empty v-else description="资讯不存在或已下架">
      <el-button type="primary" @click="router.push('/news')">返回资讯列表</el-button>
    </el-empty>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ArrowLeft, Clock, Location, View } from "@element-plus/icons-vue";
import { getNews } from "../api/content";
import { toParagraphs } from "../utils/format";

const route = useRoute();
const router = useRouter();

const loading = ref(true);
const news = ref(null);

const paragraphs = computed(() => toParagraphs(news.value?.content));

function formatTime(value) {
  return value ? String(value).slice(0, 16) : "--";
}

async function loadDetail() {
  loading.value = true;
  news.value = null;
  try {
    news.value = await getNews(route.params.id);
  } catch {
    news.value = null;
  } finally {
    loading.value = false;
  }
}

function goBack() {
  if (window.history.length > 1) {
    router.back();
  } else {
    router.push("/news");
  }
}

onMounted(loadDetail);
watch(() => route.params.id, loadDetail);
</script>

<style scoped>
.detail-page {
  max-width: 880px;
  margin: 0 auto;
  padding: 24px 20px 56px;
}

.back-bar {
  margin-bottom: 10px;
}

.article-card {
  overflow: hidden;
  background: #fff;
  border: 1px solid var(--portal-border);
  border-radius: 18px;
  box-shadow: 0 12px 32px rgba(31, 61, 45, 0.06);
}

.cover-wrap {
  height: 180px;
  background: #eef8f1;
}

.cover-img {
  width: 100%;
  height: 100%;
}

.cover-fallback {
  display: flex;
  flex-direction: column;
  gap: 6px;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  color: #fff;
  background: linear-gradient(135deg, #2e9e5b, #1a6440);
}

.cover-type {
  font-size: 22px;
  font-weight: 600;
  letter-spacing: 5px;
}

.cover-sub {
  color: rgba(255, 255, 255, 0.72);
  font-size: 12px;
  letter-spacing: 2px;
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

.article-head {
  padding: 30px 48px 20px;
}

.article-tags {
  display: flex;
  gap: 12px;
  align-items: center;
}

.location {
  display: inline-flex;
  gap: 4px;
  align-items: center;
  color: #6b7c72;
  font-size: 12px;
}

.article-head h1 {
  margin: 14px 0 16px;
  color: #1f2d24;
  font-size: 28px;
  line-height: 1.45;
  letter-spacing: -0.2px;
}

.article-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 18px;
  color: #8a9a90;
  font-size: 13px;
}

.article-meta span {
  display: inline-flex;
  gap: 5px;
  align-items: center;
}

.lead {
  margin: 0 48px 8px;
  padding: 14px 18px;
  color: #4c6357;
  font-size: 14px;
  line-height: 1.85;
  background: #f6faf7;
  border-left: 3px solid #2e9e5b;
  border-radius: 6px;
}

.article-body {
  padding: 18px 48px 26px;
}

.article-body p {
  margin: 0 0 16px;
  color: #33463c;
  font-size: 15px;
  line-height: 1.95;
}

.article-foot {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  align-items: center;
  justify-content: space-between;
  padding: 18px 48px 24px;
  border-top: 1px dashed var(--portal-border);
}

.foot-note {
  color: #8a9a90;
  font-size: 12px;
}

.foot-actions {
  display: flex;
  gap: 8px;
}

@media (max-width: 720px) {
  .cover-wrap {
    height: 170px;
  }

  .article-head,
  .article-body,
  .article-foot {
    padding-right: 20px;
    padding-left: 20px;
  }

  .lead {
    margin-right: 20px;
    margin-left: 20px;
  }

  .article-head h1 {
    font-size: 22px;
  }
}
</style>
