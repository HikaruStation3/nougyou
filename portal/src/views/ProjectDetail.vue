<template>
  <div class="detail-page">
    <div class="back-bar">
      <el-button text @click="router.back()">
        <el-icon><ArrowLeft /></el-icon> 返回列表
      </el-button>
    </div>

    <el-skeleton v-if="loading" :rows="6" animated />

    <template v-else-if="project">
      <el-card shadow="never" class="detail-card">
        <div class="detail-cover">
          <el-image
            v-if="project.cover"
            :src="project.cover"
            :preview-src-list="galleryList"
            fit="cover"
            class="cover-img"
            preview-teleported
          />
          <div v-else class="cover-empty">暂无封面图</div>
        </div>

        <div class="detail-head">
          <h1>{{ project.title }}</h1>
          <div class="head-tags">
            <el-tag type="success" effect="plain">{{ project.projectType }}</el-tag>
            <el-tag v-if="project.capacity" effect="plain">可接待 {{ project.capacity }} 人</el-tag>
            <el-button
              v-if="project.farmerId"
              size="small"
              type="primary"
              plain
              :icon="ChatDotRound"
              @click="consultFarmer"
            >
              咨询商家
            </el-button>
            <span class="view-count">
              <el-icon><View /></el-icon> {{ project.viewCount || 0 }} 次浏览
            </span>
          </div>
        </div>

        <el-descriptions :column="2" border class="info-desc">
          <el-descriptions-item label="商家">
            {{ project.farmerName || "-" }}
            <span v-if="project.shopName" class="sub">（{{ project.shopName }}）</span>
          </el-descriptions-item>
          <el-descriptions-item label="联系电话">
            <a v-if="project.contactPhone" :href="`tel:${project.contactPhone}`">
              {{ project.contactPhone }}
            </a>
            <span v-else>-</span>
          </el-descriptions-item>
          <el-descriptions-item label="项目地址" :span="2">
            {{ project.address || "-" }}
          </el-descriptions-item>
          <el-descriptions-item label="营业时间">{{ project.businessHours || "-" }}</el-descriptions-item>
          <el-descriptions-item label="价格说明">
            <span class="price">{{ project.priceDesc || "-" }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="提交时间" :span="2">{{ project.createTime || "-" }}</el-descriptions-item>
        </el-descriptions>

        <el-card v-if="project.intro" shadow="never" class="intro-card">
          <template #header>项目介绍</template>
          <div class="intro-text">{{ project.intro }}</div>
        </el-card>

        <el-card v-if="galleryList.length" shadow="never" class="gallery-card">
          <template #header>环境图集</template>
          <div class="gallery-grid">
            <el-image
              v-for="(img, idx) in galleryList"
              :key="idx"
              :src="img"
              :preview-src-list="galleryList"
              :initial-index="idx"
              fit="cover"
              class="gallery-img"
              preview-teleported
            />
          </div>
        </el-card>
      </el-card>
    </template>

    <el-empty v-else description="项目不存在或已下架">
      <el-button type="primary" @click="router.push('/projects')">返回列表</el-button>
    </el-empty>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { ArrowLeft, ChatDotRound, View } from "@element-plus/icons-vue";
import { getProject } from "../api/project";

const route = useRoute();
const router = useRouter();
const loading = ref(false);
const project = ref(null);

/** 带上商家 ID 跳到我的咨询，由咨询页自动建会话 */
function consultFarmer() {
  if (!project.value?.farmerId) return;
  if (!localStorage.getItem("portal_token")) {
    ElMessage.warning("登录后即可咨询商家");
    router.push({ name: "login", query: { redirect: `/projects/${route.params.id}` } });
    return;
  }
  router.push({
    path: "/consult",
    query: { targetType: 2, targetId: project.value.farmerId },
  });
}

const galleryList = computed(() => {
  if (!project.value) return [];
  return [project.value.cover, ...(project.value.images || [])].filter(Boolean);
});

async function loadDetail() {
  loading.value = true;
  try {
    project.value = await getProject(route.params.id);
  } catch {
    project.value = null;
  } finally {
    loading.value = false;
  }
}

onMounted(loadDetail);
</script>

<style scoped>
.detail-page {
  max-width: 960px;
  margin: 0 auto;
  padding: 24px 20px 48px;
}

.back-bar {
  margin-bottom: 12px;
}

.detail-card {
  border-radius: 12px;
}

.detail-cover {
  margin: -20px -20px 0;
  height: 320px;
  background: #eef3f0;
  overflow: hidden;
  border-radius: 12px 12px 0 0;
}

.cover-img {
  width: 100%;
  height: 100%;
}

.cover-empty {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #8a9d92;
}

.detail-head {
  padding: 20px 0 8px;
}

.detail-head h1 {
  margin: 0 0 10px;
  font-size: 26px;
  color: #1f3d2b;
}

.head-tags {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.view-count {
  margin-left: auto;
  font-size: 13px;
  color: #8a9d92;
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.info-desc {
  margin-top: 8px;
}

.info-desc :deep(.el-descriptions__label) {
  width: 110px;
  color: #5d7668;
}

.sub {
  color: #8a9d92;
  font-size: 13px;
}

.price {
  color: #2e9e5b;
  font-weight: 600;
}

.intro-card,
.gallery-card {
  margin-top: 18px;
  border-radius: 12px;
}

.intro-text {
  white-space: pre-wrap;
  line-height: 1.8;
  color: #3a4a42;
}

.gallery-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 12px;
}

.gallery-img {
  width: 100%;
  height: 140px;
  border-radius: 8px;
  cursor: pointer;
}
</style>
