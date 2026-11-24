<template>
  <div class="app-container">
    <el-card shadow="never" class="mb-4">
      <el-form inline @submit.prevent>
        <el-form-item label="审核状态">
          <el-select v-model="queryParams.auditStatus" placeholder="全部" clearable style="width: 140px">
            <el-option label="待审核" :value="0" />
            <el-option label="已通过" :value="1" />
            <el-option label="已驳回" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键字">
          <el-input
            v-model="queryParams.keyword"
            placeholder="视频标题 / 商家名称"
            clearable
            style="width: 240px"
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="search" @click="handleQuery">查询</el-button>
          <el-button icon="refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="videoList" border stripe>
        <el-table-column label="视频" min-width="280">
          <template #default="{ row }">
            <div class="flex items-center gap-3">
              <div class="cover-box" @click="openPreview(row)">
                <el-image :src="mediaUrl(row.cover)" fit="cover" class="h-full w-full">
                  <template #error>
                    <div class="cover-fallback">无封面</div>
                  </template>
                </el-image>
                <span class="cover-play">▶</span>
              </div>
              <div class="min-w-0">
                <div class="truncate font-medium">{{ row.title }}</div>
                <div class="truncate text-xs text-[--el-text-color-secondary]">
                  {{ row.description || "无简介" }}
                </div>
                <div class="mt-1 text-xs text-[--el-text-color-secondary]">
                  时长 {{ formatDuration(row.duration) }}
                </div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="商家" min-width="140">
          <template #default="{ row }">
            <div>{{ row.farmerName || "-" }}</div>
            <div class="text-xs text-[--el-text-color-secondary]">{{ row.shopName || "-" }}</div>
          </template>
        </el-table-column>
        <el-table-column label="互动数据" width="180" align="center">
          <template #default="{ row }">
            <div class="flex justify-around text-xs">
              <span>👍 {{ row.likeCount ?? 0 }}</span>
              <span>💬 {{ row.commentCount ?? 0 }}</span>
              <span>▶ {{ row.viewCount ?? 0 }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="审核状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="auditStatusType(row.auditStatus)" effect="plain">
              {{ auditStatusText(row.auditStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="展示状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 && row.auditStatus === 1 ? 'success' : 'info'" effect="plain">
              {{ row.status === 1 && row.auditStatus === 1 ? "展示中" : "未展示" }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="提交时间" min-width="160" />
        <el-table-column label="操作" width="160" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openPreview(row)">预览</el-button>
            <el-button
              v-hasPerm="['video:audit']"
              :type="row.auditStatus === 0 ? 'success' : 'warning'"
              link
              @click="openAudit(row)"
            >
              {{ row.auditStatus === 0 ? "审核" : "复审" }}
            </el-button>
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

    <!-- 预览 -->
    <el-dialog v-model="previewVisible" title="短视频预览" width="480px" destroy-on-close>
      <video
        v-if="currentVideo"
        :src="mediaUrl(currentVideo.videoUrl)"
        :poster="mediaUrl(currentVideo.cover)"
        class="preview-video"
        controls
        playsinline
      ></video>
      <el-descriptions v-if="currentVideo" :column="1" border size="small" class="mt-4">
        <el-descriptions-item label="标题">{{ currentVideo.title }}</el-descriptions-item>
        <el-descriptions-item label="商家">
          {{ currentVideo.shopName || "-" }}（{{ currentVideo.farmerName || "-" }}）
        </el-descriptions-item>
        <el-descriptions-item label="简介">{{ currentVideo.description || "-" }}</el-descriptions-item>
        <el-descriptions-item label="时长">{{ formatDuration(currentVideo.duration) }}</el-descriptions-item>
        <el-descriptions-item label="审核意见">{{ currentVideo.auditRemark || "-" }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <!-- 审核 -->
    <el-dialog v-model="auditVisible" title="审核短视频" width="560px" top="8vh">
      <el-descriptions v-if="currentVideo" :column="2" border size="small" class="mb-4">
        <el-descriptions-item label="视频标题" :span="2">{{ currentVideo.title }}</el-descriptions-item>
        <el-descriptions-item label="商家">{{ currentVideo.farmerName || "-" }}</el-descriptions-item>
        <el-descriptions-item label="店铺">{{ currentVideo.shopName || "-" }}</el-descriptions-item>
        <el-descriptions-item label="提交时间" :span="2">{{ currentVideo.createTime || "-" }}</el-descriptions-item>
      </el-descriptions>

      <el-form label-width="90px">
        <el-form-item label="审核结果" required>
          <el-radio-group v-model="auditForm.auditStatus">
            <el-radio :value="1">通过</el-radio>
            <el-radio :value="2">驳回</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="审核意见">
          <el-input
            v-model="auditForm.auditRemark"
            type="textarea"
            :rows="4"
            maxlength="200"
            show-word-limit
            :placeholder="auditForm.auditStatus === 1 ? '选填' : '驳回时请填写具体原因，便于商家修改'"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="auditVisible = false">取消</el-button>
        <el-button type="primary" :loading="auditLoading" @click="handleAudit">提交审核</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import VideoAPI, { type VideoItem, type VideoQueryParams } from "@/api/video";

defineOptions({ name: "VideoAudit" });

const loading = ref(false);
const auditLoading = ref(false);
const videoList = ref<VideoItem[]>([]);
const total = ref(0);

const queryParams = reactive<VideoQueryParams>({
  pageNum: 1,
  pageSize: 10,
  auditStatus: undefined,
  keyword: "",
});

const previewVisible = ref(false);
const auditVisible = ref(false);
const currentVideo = ref<VideoItem>();
const auditForm = reactive({ auditStatus: 1, auditRemark: "" });

/**
 * 后端返回的是 /api/v1/media 或 /api/v1/files/local 这类相对地址，
 * 管理端开发环境走 /dev-api 前缀代理，需要补上环境变量里的前缀。
 */
function mediaUrl(url?: string) {
  if (!url) return "";
  if (url.startsWith("http") || url.startsWith("blob:") || url.startsWith("data:")) return url;
  return `${import.meta.env.VITE_APP_BASE_API}${url}`;
}

function formatDuration(seconds?: number) {
  const total = Math.max(0, Math.floor(Number(seconds) || 0));
  return `${Math.floor(total / 60)}:${String(total % 60).padStart(2, "0")}`;
}

function auditStatusType(status: number) {
  if (status === 1) return "success";
  if (status === 2) return "danger";
  return "warning";
}

function auditStatusText(status: number) {
  if (status === 1) return "已通过";
  if (status === 2) return "已驳回";
  return "待审核";
}

async function loadData() {
  loading.value = true;
  try {
    const page = await VideoAPI.getPage(queryParams);
    videoList.value = page.list;
    total.value = page.total;
  } finally {
    loading.value = false;
  }
}

function handleQuery() {
  queryParams.pageNum = 1;
  loadData();
}

function handleReset() {
  queryParams.auditStatus = undefined;
  queryParams.keyword = "";
  queryParams.pageNum = 1;
  loadData();
}

function openPreview(row: VideoItem) {
  currentVideo.value = row;
  previewVisible.value = true;
}

function openAudit(row: VideoItem) {
  currentVideo.value = row;
  auditForm.auditStatus = 1;
  auditForm.auditRemark = "";
  auditVisible.value = true;
}

async function handleAudit() {
  if (!currentVideo.value) return;
  if (auditForm.auditStatus === 2 && !auditForm.auditRemark.trim()) {
    ElMessage.warning("驳回时请填写审核意见");
    return;
  }
  auditLoading.value = true;
  try {
    await VideoAPI.audit(currentVideo.value.id, { ...auditForm });
    ElMessage.success(auditForm.auditStatus === 1 ? "已通过，视频将在前台展示" : "已驳回");
    auditVisible.value = false;
    await loadData();
  } finally {
    auditLoading.value = false;
  }
}

onMounted(loadData);
</script>

<style scoped>
.cover-box {
  position: relative;
  flex-shrink: 0;
  width: 54px;
  height: 78px;
  overflow: hidden;
  cursor: pointer;
  background: #eef3f0;
  border-radius: 8px;
}

.cover-fallback {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  font-size: 12px;
  color: #8a9d92;
}

.cover-play {
  position: absolute;
  right: 4px;
  bottom: 4px;
  padding: 0 4px;
  font-size: 10px;
  color: #fff;
  background: rgba(0, 0, 0, 0.55);
  border-radius: 4px;
}

.preview-video {
  width: 100%;
  max-height: 68vh;
  background: #000;
  border-radius: 8px;
}
</style>
