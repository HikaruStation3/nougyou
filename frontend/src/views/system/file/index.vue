<template>
  <div class="app-container file-page">
    <el-card shadow="never" class="file-page__intro">
      <div class="file-page__intro-main">
        <div>
          <h2 class="file-page__title">文件上传</h2>
          <p class="file-page__subtitle">
            身份证、贫困证明、商品图、头像与短视频统一走此接口；文件按业务类型归档到独立目录。
          </p>
        </div>
        <el-tag type="success" effect="plain" round>MinIO 对象存储</el-tag>
      </div>
      <el-alert
        type="info"
        :closable="false"
        show-icon
        title="图片仅支持 jpg / jpeg / png，单文件不超过 5MB；短视频仅支持 mp4，不超过 100MB。MinIO 不可用时自动落本地磁盘，地址依然可直接访问。"
      />
    </el-card>

    <el-row :gutter="16">
      <el-col :xs="24" :lg="10">
        <el-card shadow="hover" class="file-page__panel">
          <template #header>
            <div class="file-page__panel-title">
              <span>选择业务类型</span>
              <span class="file-page__hint">决定存储目录与校验规则</span>
            </div>
          </template>

          <div class="biz-type-list">
            <div
              v-for="item in bizTypes"
              :key="item.value"
              class="biz-type"
              :class="{ 'biz-type--active': bizType === item.value }"
              @click="bizType = item.value"
            >
              <el-icon class="biz-type__icon"><component :is="item.icon" /></el-icon>
              <div class="biz-type__body">
                <div class="biz-type__label">{{ item.label }}</div>
                <div class="biz-type__desc">{{ item.desc }}</div>
              </div>
              <el-tag size="small" effect="plain">{{ item.maxMB }}MB</el-tag>
            </div>
          </div>

          <el-upload
            drag
            class="file-uploader"
            :show-file-list="false"
            :accept="currentType.accept"
            :before-upload="handleBeforeUpload"
            :http-request="handleUpload"
            :disabled="uploading"
          >
            <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
            <div class="el-upload__text">
              {{ uploading ? `上传中 ${percent}%` : "拖拽文件到此处，或点击选择文件" }}
            </div>
            <template #tip>
              <div class="el-upload__tip">
                当前类型：{{ currentType.label }} · {{ currentType.extensionText }} · 不超过
                {{ currentType.maxMB }}MB
              </div>
            </template>
          </el-upload>

          <el-progress v-if="uploading" :percentage="percent" :stroke-width="8" class="file-progress" />
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="14">
        <el-card shadow="hover" class="file-page__panel">
          <template #header>
            <div class="file-page__panel-title">
              <span>上传结果</span>
              <el-button text :disabled="!uploaded" @click="resetResult">清空</el-button>
            </div>
          </template>

          <el-empty v-if="!uploaded" description="上传成功后在这里预览并复制访问地址" :image-size="90" />

          <div v-else class="result">
            <div class="result__preview">
              <el-image
                v-if="isImage(uploaded)"
                :src="mediaUrl(uploaded.url)"
                :preview-src-list="[mediaUrl(uploaded.url)]"
                fit="contain"
                class="result__image"
                preview-teleported
              />
              <video
                v-else-if="isVideo(uploaded)"
                :src="mediaUrl(uploaded.url)"
                class="result__video"
                controls
              />
              <div v-else class="result__file">
                <el-icon><Document /></el-icon>
                <span>{{ uploaded.name }}</span>
              </div>
            </div>

            <el-descriptions :column="1" border class="result__meta">
              <el-descriptions-item label="文件名">{{ uploaded.name }}</el-descriptions-item>
              <el-descriptions-item label="文件大小">{{ formatSize(uploaded.size) }}</el-descriptions-item>
              <el-descriptions-item label="MIME 类型">{{ uploaded.contentType || "-" }}</el-descriptions-item>
              <el-descriptions-item label="访问地址">
                <div class="result__url">
                  <el-link type="primary" :href="mediaUrl(uploaded.url)" target="_blank">
                    {{ uploaded.url }}
                  </el-link>
                  <el-button text type="primary" @click="copyUrl(uploaded.url)">复制</el-button>
                </div>
              </el-descriptions-item>
            </el-descriptions>

            <div class="result__actions">
              <el-button icon="copy-document" @click="copyUrl(uploaded.url)">复制链接</el-button>
              <el-button icon="view" @click="openUrl(uploaded.url)">新窗口打开</el-button>
              <el-button
                v-hasPerm="['file:delete']"
                type="danger"
                icon="delete"
                @click="handleDelete(uploaded)"
              >
                删除文件
              </el-button>
            </div>
          </div>
        </el-card>

        <el-card v-if="records.length" shadow="hover" class="file-page__panel file-page__history">
          <template #header>
            <div class="file-page__panel-title">
              <span>本次上传记录</span>
              <span class="file-page__hint">仅保留当前页面会话，共 {{ records.length }} 条</span>
            </div>
          </template>

          <el-table :data="records" border stripe size="small">
            <el-table-column prop="name" label="文件名" min-width="160" show-overflow-tooltip />
            <el-table-column label="业务类型" width="100" align="center">
              <template #default="{ row }">
                <el-tag size="small" effect="plain">{{ bizLabel(row.bizType) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="大小" width="100" align="right">
              <template #default="{ row }">{{ formatSize(row.size) }}</template>
            </el-table-column>
            <el-table-column prop="time" label="上传时间" width="95" align="center" />
            <el-table-column label="操作" width="150" align="center">
              <template #default="{ row }">
                <el-button text type="primary" @click="copyUrl(row.url)">复制</el-button>
                <el-button
                  v-hasPerm="['file:delete']"
                  text
                  type="danger"
                  @click="handleDelete(row)"
                >
                  删除
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from "vue";
import { ElMessage, ElMessageBox, type UploadRawFile, type UploadRequestOptions } from "element-plus";
import {
  Avatar,
  Document,
  Files,
  Picture,
  UploadFilled,
  VideoCamera,
} from "@element-plus/icons-vue";
import FileAPI, { type FileBizType, type FileInfo } from "@/api/file";

defineOptions({ name: "FileUpload" });

interface BizTypeOption {
  value: FileBizType;
  label: string;
  desc: string;
  icon: any;
  exts: string[];
  extensionText: string;
  accept: string;
  maxMB: number;
}

const bizTypes: BizTypeOption[] = [
  {
    value: "idcard",
    label: "身份证",
    desc: "入驻申请的身份证正反面",
    icon: Avatar,
    exts: ["jpg", "jpeg", "png"],
    extensionText: "jpg / jpeg / png",
    accept: ".jpg,.jpeg,.png",
    maxMB: 5,
  },
  {
    value: "proof",
    label: "贫困证明",
    desc: "建档立卡、低保证明等材料",
    icon: Files,
    exts: ["jpg", "jpeg", "png"],
    extensionText: "jpg / jpeg / png",
    accept: ".jpg,.jpeg,.png",
    maxMB: 5,
  },
  {
    value: "product",
    label: "商品图",
    desc: "商品封面、图集与农家乐图片",
    icon: Picture,
    exts: ["jpg", "jpeg", "png"],
    extensionText: "jpg / jpeg / png",
    accept: ".jpg,.jpeg,.png",
    maxMB: 5,
  },
  {
    value: "video",
    label: "短视频",
    desc: "商家发布的助农短视频",
    icon: VideoCamera,
    exts: ["mp4"],
    extensionText: "mp4",
    accept: ".mp4",
    maxMB: 100,
  },
  {
    value: "avatar",
    label: "头像",
    desc: "用户与商家的头像图片",
    icon: Avatar,
    exts: ["jpg", "jpeg", "png"],
    extensionText: "jpg / jpeg / png",
    accept: ".jpg,.jpeg,.png",
    maxMB: 5,
  },
];

interface UploadRecord extends FileInfo {
  bizType: FileBizType;
  time: string;
}

const bizType = ref<FileBizType>("product");
const uploading = ref(false);
const percent = ref(0);
const uploaded = ref<FileInfo>();
const records = ref<UploadRecord[]>([]);

const currentType = computed(() => bizTypes.find((item) => item.value === bizType.value) || bizTypes[2]);

function bizLabel(value: FileBizType) {
  return bizTypes.find((item) => item.value === value)?.label || value;
}

function handleBeforeUpload(file: UploadRawFile) {
  const ext = file.name.split(".").pop()?.toLowerCase() || "";
  if (!currentType.value.exts.includes(ext)) {
    ElMessage.warning(`${currentType.value.label}仅支持 ${currentType.value.extensionText} 格式`);
    return false;
  }
  if (file.size > currentType.value.maxMB * 1024 * 1024) {
    ElMessage.warning(`${currentType.value.label}大小不能超过 ${currentType.value.maxMB}MB`);
    return false;
  }
  return true;
}

async function handleUpload(options: UploadRequestOptions) {
  uploading.value = true;
  percent.value = 0;
  try {
    const info = await FileAPI.uploadWithBizType(
      options.file as File,
      bizType.value,
      (value) => (percent.value = value)
    );
    uploaded.value = info;
    records.value.unshift({
      ...info,
      bizType: bizType.value,
      time: new Date().toLocaleTimeString("zh-CN", { hour12: false }),
    });
    if (records.value.length > 20) {
      records.value.length = 20;
    }
    ElMessage.success("上传成功");
  } finally {
    uploading.value = false;
  }
}

function isImage(file: FileInfo) {
  return (file.contentType || "").startsWith("image/");
}

function isVideo(file: FileInfo) {
  return (file.contentType || "").startsWith("video/");
}

function mediaUrl(url?: string) {
  if (!url) return "";
  if (url.startsWith("http") || url.startsWith("blob:") || url.startsWith("data:")) return url;
  return `${import.meta.env.VITE_APP_BASE_API}${url}`;
}

function formatSize(size?: number) {
  if (!size) return "-";
  if (size < 1024) return `${size} B`;
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`;
  return `${(size / 1024 / 1024).toFixed(2)} MB`;
}

async function copyUrl(url: string) {
  try {
    if (navigator.clipboard?.writeText) {
      await navigator.clipboard.writeText(url);
    } else {
      const input = document.createElement("input");
      input.value = url;
      input.style.position = "absolute";
      input.style.left = "-9999px";
      document.body.appendChild(input);
      input.select();
      document.execCommand("copy");
      document.body.removeChild(input);
    }
    ElMessage.success("链接已复制");
  } catch {
    ElMessage.warning("复制失败，请手动选中地址复制");
  }
}

function openUrl(url: string) {
  window.open(mediaUrl(url), "_blank");
}

// 表格行与上传结果共用同一套删除逻辑，行类型由 el-table 推断为 DefaultRow
async function handleDelete(file: any) {
  try {
    await ElMessageBox.confirm("删除后文件地址将立即失效，确认删除？", "删除文件", {
      type: "warning",
      confirmButtonText: "删除",
      cancelButtonText: "取消",
    });
  } catch {
    return;
  }
  await FileAPI.delete(file.url);
  records.value = records.value.filter((item) => item.url !== file.url);
  if (uploaded.value?.url === file.url) {
    uploaded.value = undefined;
  }
  ElMessage.success("文件已删除");
}

function resetResult() {
  uploaded.value = undefined;
}
</script>

<style lang="scss" scoped>
.file-page__intro {
  margin-bottom: 16px;
}

.file-page__intro-main {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 12px;
}

.file-page__title {
  margin: 0 0 6px;
  font-size: 20px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.file-page__subtitle {
  margin: 0;
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.file-page__panel {
  border-radius: 10px;
}

.file-page__panel + .file-page__panel {
  margin-top: 16px;
}

.file-page__panel-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-weight: 600;
}

.file-page__hint {
  font-size: 12px;
  font-weight: 400;
  color: var(--el-text-color-secondary);
}

.biz-type-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 10px;
  margin-bottom: 16px;
}

.biz-type {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  cursor: pointer;
  border: 1px solid var(--el-border-color);
  border-radius: 8px;
  transition: all 0.2s;

  &:hover {
    border-color: var(--el-color-primary-light-5);
  }

  &--active {
    background: var(--el-color-primary-light-9);
    border-color: var(--el-color-primary);
  }
}

.biz-type__icon {
  font-size: 20px;
  color: var(--el-color-primary);
}

.biz-type__body {
  flex: 1;
  min-width: 0;
}

.biz-type__label {
  font-size: 14px;
  font-weight: 600;
}

.biz-type__desc {
  overflow: hidden;
  font-size: 12px;
  color: var(--el-text-color-secondary);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-progress {
  margin-top: 12px;
}

.result__preview {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 180px;
  margin-bottom: 16px;
  overflow: hidden;
  background: var(--el-fill-color-light);
  border-radius: 8px;
}

.result__image {
  max-width: 100%;
  max-height: 260px;
}

.result__video {
  width: 100%;
  max-height: 280px;
  background: #000;
}

.result__file {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--el-text-color-secondary);
}

.result__url {
  display: flex;
  align-items: center;
  gap: 8px;
  word-break: break-all;
}

.result__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 16px;
}

.file-page__history {
  margin-top: 16px;
}
</style>
