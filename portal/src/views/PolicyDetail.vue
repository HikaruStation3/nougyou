<template>
  <div class="detail-page">
    <div class="back-bar">
      <el-button text @click="goBack">
        <el-icon><ArrowLeft /></el-icon> 返回政策列表
      </el-button>
    </div>

    <el-skeleton v-if="loading" :rows="9" animated />

    <template v-else-if="policy">
      <article class="article-card">
        <header class="article-head">
          <div class="article-tags">
            <el-tag type="success" effect="dark" round size="small">扶贫政策</el-tag>
            <span v-if="policy.policyNo" class="doc-no">文号 {{ policy.policyNo }}</span>
          </div>
          <h1>{{ policy.title }}</h1>
          <div class="article-meta">
            <span>
              <el-icon><OfficeBuilding /></el-icon>
              {{ policy.issueDept || "钟山镇人民政府" }}
            </span>
            <span>
              <el-icon><Clock /></el-icon>
              {{ formatTime(policy.publishTime) }}
            </span>
            <span>
              <el-icon><View /></el-icon>
              {{ policy.viewCount || 0 }} 次浏览
            </span>
          </div>
        </header>

        <div v-if="policy.summary" class="summary-box">
          <strong>政策要点</strong>
          <p>{{ policy.summary }}</p>
        </div>

        <div class="article-body">
          <p v-for="(text, idx) in paragraphs" :key="idx">{{ text }}</p>
          <el-empty v-if="!paragraphs.length" :image-size="80" description="正文暂未录入" />
        </div>

        <footer class="article-foot">
          <span>本政策由 {{ policy.issueDept || "钟山镇人民政府" }} 发布，如有疑问请咨询镇便民服务中心。</span>
          <div class="foot-actions">
            <el-button @click="router.push('/policies')">更多政策</el-button>
            <el-button type="primary" plain @click="router.push('/news')">浏览乡镇资讯</el-button>
          </div>
        </footer>
      </article>
    </template>

    <el-empty v-else description="政策不存在或已下架">
      <el-button type="primary" @click="router.push('/policies')">返回政策列表</el-button>
    </el-empty>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ArrowLeft, Clock, OfficeBuilding, View } from "@element-plus/icons-vue";
import { getPolicy } from "../api/content";
import { toParagraphs } from "../utils/format";

const route = useRoute();
const router = useRouter();

const loading = ref(true);
const policy = ref(null);

const paragraphs = computed(() => toParagraphs(policy.value?.content));

function formatTime(value) {
  return value ? String(value).slice(0, 16) : "--";
}

async function loadDetail() {
  loading.value = true;
  policy.value = null;
  try {
    policy.value = await getPolicy(route.params.id);
  } catch {
    policy.value = null;
  } finally {
    loading.value = false;
  }
}

function goBack() {
  if (window.history.length > 1) {
    router.back();
  } else {
    router.push("/policies");
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
  padding: 40px 48px 32px;
  background: #fff;
  border: 1px solid var(--portal-border);
  border-radius: 18px;
  box-shadow: 0 12px 32px rgba(31, 61, 45, 0.06);
}

.article-head {
  padding-bottom: 22px;
  border-bottom: 1px solid var(--portal-border);
}

.article-tags {
  display: flex;
  gap: 12px;
  align-items: center;
}

.doc-no {
  color: #8a9a90;
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

.summary-box {
  margin: 24px 0 6px;
  padding: 16px 18px;
  background: #f6faf7;
  border-left: 3px solid #2e9e5b;
  border-radius: 6px;
}

.summary-box strong {
  display: block;
  margin-bottom: 6px;
  color: #1f7a48;
  font-size: 13px;
}

.summary-box p {
  margin: 0;
  color: #4c6357;
  font-size: 14px;
  line-height: 1.85;
}

.article-body {
  padding: 18px 0 8px;
}

.article-body p {
  margin: 0 0 16px;
  color: #33463c;
  font-size: 15px;
  line-height: 2;
  text-indent: 2em;
}

.article-foot {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  align-items: center;
  justify-content: space-between;
  margin-top: 18px;
  padding-top: 20px;
  border-top: 1px dashed var(--portal-border);
}

.article-foot > span {
  flex: 1;
  min-width: 240px;
  color: #8a9a90;
  font-size: 12px;
  line-height: 1.7;
}

.foot-actions {
  display: flex;
  gap: 8px;
}

@media (max-width: 720px) {
  .article-card {
    padding: 26px 20px 24px;
  }

  .article-head h1 {
    font-size: 22px;
  }
}
</style>
