<template>
  <div class="merchant-videos">
    <section class="page-head">
      <div>
        <h1>我的短视频</h1>
        <p>发布与扶贫助农相关的合规短视频，提交后由平台审核，通过后展示在「助农短视频」信息流。</p>
      </div>
      <div class="head-actions">
        <el-button round @click="goFeed">去刷短视频</el-button>
        <el-button type="primary" round @click="openPublish">
          <el-icon><Plus /></el-icon>
          发布短视频
        </el-button>
      </div>
    </section>

    <el-card shadow="never" class="filter-card">
      <div class="filter-row">
        <el-radio-group v-model="query.auditStatus" @change="handleFilter">
          <el-radio-button value="">全部</el-radio-button>
          <el-radio-button :value="0">待审核</el-radio-button>
          <el-radio-button :value="1">已通过</el-radio-button>
          <el-radio-button :value="2">已驳回</el-radio-button>
        </el-radio-group>
        <span class="tip">审核一般在 1 个工作日内完成，驳回后可按意见修改重新发布。</span>
      </div>
    </el-card>

    <div v-loading="loading" class="video-grid">
      <el-empty v-if="!loading && !list.length" description="还没有发布短视频">
        <el-button type="primary" round @click="openPublish">发布第一条</el-button>
      </el-empty>

      <el-card v-for="item in list" :key="item.id" shadow="hover" class="video-card">
        <div class="card-cover">
          <el-image :src="item.cover" fit="cover" class="cover-img">
            <template #error>
              <div class="cover-fallback">暂无封面</div>
            </template>
          </el-image>
          <span class="duration">{{ formatDuration(item.duration) }}</span>
          <span class="audit-tag" :class="statusClass(item.auditStatus)">
            {{ statusText(item.auditStatus) }}
          </span>
          <button class="preview-btn" title="预览" @click="openPreview(item)">
            <el-icon :size="22"><VideoPlay /></el-icon>
          </button>
        </div>

        <div class="card-body">
          <h3>{{ item.title }}</h3>
          <p v-if="item.description" class="desc">{{ item.description }}</p>
          <p v-if="item.auditStatus === 2" class="reject">
            驳回原因：{{ item.auditRemark || "未填写" }}
          </p>
          <div class="stats">
            <span>{{ item.viewCount || 0 }} 播放</span>
            <span>{{ item.likeCount || 0 }} 点赞</span>
            <span>{{ item.commentCount || 0 }} 评论</span>
          </div>
          <div class="card-footer">
            <span class="time">{{ timeAgo(item.createTime) }}提交</span>
            <el-button type="danger" link @click="handleDelete(item)">删除</el-button>
          </div>
        </div>
      </el-card>
    </div>

    <div v-if="total > query.pageSize" class="pagination-wrap">
      <el-pagination
        v-model:current-page="query.pageNum"
        v-model:page-size="query.pageSize"
        :total="total"
        :page-sizes="[6, 12, 24]"
        layout="total, sizes, prev, pager, next"
        background
        @size-change="loadData"
        @current-change="loadData"
      />
    </div>

    <!-- 发布弹窗 -->
    <el-dialog
      v-model="publishVisible"
      title="发布短视频"
      width="720px"
      :close-on-click-modal="false"
      destroy-on-close
      @closed="resetPublish"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="88px">
        <el-form-item label="视频文件" required>
          <div class="upload-box" :class="{ 'has-file': fileInfo.name }">
            <input
              ref="fileInputRef"
              type="file"
              accept="video/mp4,.mp4"
              class="file-input"
              @change="handleFileChange"
            />
            <template v-if="!fileInfo.name">
              <el-icon :size="30"><UploadFilled /></el-icon>
              <p>点击选择视频，支持 mp4，单个不超过 100MB</p>
            </template>
            <template v-else>
              <video
                ref="previewRef"
                :src="fileInfo.objectUrl"
                class="pick-preview"
                muted
                playsinline
                @loadedmetadata="onPreviewMeta"
              ></video>
              <div class="file-meta">
                <span class="file-name">{{ fileInfo.name }}</span>
                <span class="file-size">{{ fileInfo.sizeText }}</span>
                <span v-if="fileInfo.duration" class="file-duration">
                  时长 {{ formatDuration(fileInfo.duration) }}
                </span>
                <el-button link type="primary" @click="chooseAgain">重新选择</el-button>
              </div>
            </template>
          </div>
        </el-form-item>

        <el-form-item label="视频标题" prop="title">
          <el-input
            v-model="form.title"
            maxlength="50"
            show-word-limit
            placeholder="例如：兴化米粉是怎么晒出来的"
          />
        </el-form-item>

        <el-form-item label="视频简介">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            maxlength="500"
            show-word-limit
            placeholder="介绍一下这条视频拍了什么，买家更愿意看完"
          />
        </el-form-item>

        <el-form-item label="封面">
          <div class="cover-row">
            <div class="cover-preview">
              <img v-if="form.cover" :src="form.cover" alt="封面预览" />
              <div v-else class="cover-empty">选择视频后自动截取</div>
            </div>
            <div class="cover-tip">
              <p>系统默认截取视频画面作为封面，可在下方调整截取位置。</p>
              <el-slider
                v-model="coverSecond"
                :min="0"
                :max="Math.max(1, Math.floor(fileInfo.duration || 1))"
                :step="1"
                :disabled="!fileInfo.objectUrl"
                @change="captureCover"
              />
            </div>
          </div>
        </el-form-item>

        <el-form-item v-if="uploading" label="上传进度">
          <el-progress :percentage="uploadPercent" :stroke-width="12" class="upload-progress" />
        </el-form-item>
      </el-form>

      <template #footer>
        <span class="dialog-tip">提交后进入待审核，审核通过才会对外展示</span>
        <el-button @click="publishVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">提交审核</el-button>
      </template>
    </el-dialog>

    <!-- 预览弹窗 -->
    <el-dialog v-model="previewVisible" :title="previewItem?.title" width="480px" destroy-on-close>
      <video
        v-if="previewItem"
        :src="previewItem.videoUrl"
        :poster="previewItem.cover"
        class="preview-video"
        controls
        playsinline
      ></video>
      <p v-if="previewItem" class="preview-meta">
        {{ statusText(previewItem.auditStatus) }} ·
        {{ previewItem.auditRemark || "暂无审核意见" }}
      </p>
    </el-dialog>
  </div>
</template>

<script setup>
import { onBeforeUnmount, onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { ElMessage, ElMessageBox } from "element-plus";
import { Plus, UploadFilled, VideoPlay } from "@element-plus/icons-vue";
import { deleteVideo, getMyVideos, publishVideo, uploadFile } from "../api/video";
import { formatDuration, timeAgo } from "../utils/format";

const router = useRouter();

const loading = ref(false);
const list = ref([]);
const total = ref(0);
const query = reactive({ pageNum: 1, pageSize: 6, auditStatus: "" });

const publishVisible = ref(false);
const previewVisible = ref(false);
const previewItem = ref(null);
const submitting = ref(false);
const uploading = ref(false);
const uploadPercent = ref(0);
const coverSecond = ref(1);
const formRef = ref(null);
const fileInputRef = ref(null);
const previewRef = ref(null);

const form = reactive({ title: "", description: "", cover: "", duration: 0 });
const fileInfo = reactive({
  file: null,
  name: "",
  sizeText: "",
  objectUrl: "",
  duration: 0,
});

const rules = {
  title: [
    { required: true, message: "请填写视频标题", trigger: "blur" },
    { min: 2, max: 50, message: "标题长度需在 2-50 字之间", trigger: "blur" },
  ],
};

function statusText(status) {
  if (status === 1) return "已通过";
  if (status === 2) return "已驳回";
  return "待审核";
}

function statusClass(status) {
  if (status === 1) return "pass";
  if (status === 2) return "reject";
  return "pending";
}

async function loadData() {
  loading.value = true;
  try {
    const params = { pageNum: query.pageNum, pageSize: query.pageSize };
    if (query.auditStatus !== "" && query.auditStatus !== null) {
      params.auditStatus = query.auditStatus;
    }
    const page = await getMyVideos(params);
    list.value = page.list || [];
    total.value = page.total || 0;
  } catch {
    list.value = [];
    total.value = 0;
  } finally {
    loading.value = false;
  }
}

function handleFilter() {
  query.pageNum = 1;
  loadData();
}

function goFeed() {
  router.push("/videos");
}

function openPublish() {
  publishVisible.value = true;
}

function resetPublish() {
  if (fileInfo.objectUrl) URL.revokeObjectURL(fileInfo.objectUrl);
  Object.assign(form, { title: "", description: "", cover: "", duration: 0 });
  Object.assign(fileInfo, { file: null, name: "", sizeText: "", objectUrl: "", duration: 0 });
  coverSecond.value = 1;
  uploadPercent.value = 0;
  uploading.value = false;
  submitting.value = false;
  formRef.value?.clearValidate();
}

function chooseAgain() {
  fileInputRef.value?.click();
}

function handleFileChange(event) {
  const file = event.target.files?.[0];
  if (!file) return;
  if (!file.type.startsWith("video/")) {
    ElMessage.warning("请选择视频文件");
    event.target.value = "";
    return;
  }
  if (!file.name.toLowerCase().endsWith(".mp4")) {
    ElMessage.warning("平台仅支持 mp4 格式的视频");
    event.target.value = "";
    return;
  }
  if (file.size > 100 * 1024 * 1024) {
    ElMessage.warning("视频不能超过 100MB");
    event.target.value = "";
    return;
  }
  if (fileInfo.objectUrl) URL.revokeObjectURL(fileInfo.objectUrl);
  fileInfo.file = file;
  fileInfo.name = file.name;
  fileInfo.sizeText = `${(file.size / 1024 / 1024).toFixed(1)} MB`;
  fileInfo.objectUrl = URL.createObjectURL(file);
  form.cover = "";
  // 清空 input 的值，重复选择同一个文件时也能触发 change
  event.target.value = "";
}

function onPreviewMeta() {
  const video = previewRef.value;
  if (!video) return;
  const duration = Math.round(video.duration || 0);
  fileInfo.duration = duration;
  form.duration = duration;
  coverSecond.value = Math.min(1, Math.max(0, Math.floor(duration * 0.1)));
  if (duration > 600) {
    ElMessage.warning("平台只接收 10 分钟以内的短视频");
  }
  captureCover();
}

function captureCover() {
  const video = previewRef.value;
  if (!video || !video.videoWidth) return;
  video.currentTime = Math.min(coverSecond.value, Math.max(0, video.duration - 0.1));
  const draw = () => {
    const canvas = document.createElement("canvas");
    canvas.width = 720;
    canvas.height = 1280;
    const ctx = canvas.getContext("2d");
    const scale = Math.max(canvas.width / video.videoWidth, canvas.height / video.videoHeight);
    const drawWidth = video.videoWidth * scale;
    const drawHeight = video.videoHeight * scale;
    ctx.drawImage(
      video,
      (canvas.width - drawWidth) / 2,
      (canvas.height - drawHeight) / 2,
      drawWidth,
      drawHeight
    );
    const dataUrl = canvas.toDataURL("image/jpeg", 0.86);
    form.cover = dataUrl;
  };
  if (Math.abs(video.currentTime - coverSecond.value) < 0.05) {
    draw();
  } else {
    video.addEventListener("seeked", draw, { once: true });
  }
}

async function handleSubmit() {
  if (!fileInfo.file) {
    ElMessage.warning("请先选择视频文件");
    return;
  }
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid) return;
  if (!form.cover) {
    ElMessage.warning("封面还在生成，请稍候再提交");
    return;
  }
  if (!form.duration) {
    ElMessage.warning("读取视频时长失败，请重新选择视频");
    return;
  }
  if (form.duration > 600) {
    ElMessage.warning("平台只接收 10 分钟以内的短视频");
    return;
  }
  submitting.value = true;
  uploading.value = true;
  uploadPercent.value = 0;
  try {
    // 1. 上传视频
    const videoFile = await uploadFile(fileInfo.file, "video", (percent) => {
      uploadPercent.value = Math.round(percent * 0.8);
    });
    // 2. 上传封面（canvas 截图转成文件）
    const coverBlob = await (await fetch(form.cover)).blob();
    const coverFile = new File([coverBlob], "cover.jpg", { type: "image/jpeg" });
    const coverResult = await uploadFile(coverFile, "product", (percent) => {
      uploadPercent.value = 80 + Math.round(percent * 0.2);
    });
    // 3. 提交审核
    await publishVideo({
      title: form.title.trim(),
      description: form.description.trim() || null,
      cover: coverResult.url,
      videoUrl: videoFile.url,
      duration: form.duration,
    });
    ElMessage.success("已提交，等待平台审核");
    publishVisible.value = false;
    query.pageNum = 1;
    await loadData();
  } catch {
    /* 失败提示已由拦截器统一处理 */
  } finally {
    submitting.value = false;
    uploading.value = false;
  }
}

function openPreview(item) {
  previewItem.value = item;
  previewVisible.value = true;
}

async function handleDelete(item) {
  try {
    await ElMessageBox.confirm("删除后买家将无法再看到这条视频，确认删除？", "删除短视频", {
      type: "warning",
      confirmButtonText: "删除",
      cancelButtonText: "取消",
    });
  } catch {
    return;
  }
  try {
    await deleteVideo(item.id);
    ElMessage.success("已删除");
    await loadData();
  } catch {
    /* 失败提示已由拦截器统一处理 */
  }
}

onMounted(loadData);
onBeforeUnmount(() => {
  if (fileInfo.objectUrl) URL.revokeObjectURL(fileInfo.objectUrl);
});
</script>

<style scoped>
.merchant-videos {
  max-width: 1180px;
  padding: 32px 20px 56px;
  margin: 0 auto;
}

.page-head {
  display: flex;
  gap: 16px;
  align-items: flex-end;
  justify-content: space-between;
  margin-bottom: 20px;
}

.page-head h1 {
  margin: 0 0 8px;
  font-size: 26px;
  color: #1f3d2b;
}

.page-head p {
  max-width: 620px;
  margin: 0;
  font-size: 13px;
  line-height: 1.7;
  color: #6b7c72;
}

.head-actions {
  display: flex;
  flex-shrink: 0;
  gap: 10px;
}

.filter-card {
  margin-bottom: 20px;
  border-radius: 12px;
}

.filter-row {
  display: flex;
  gap: 16px;
  align-items: center;
  justify-content: space-between;
}

.tip {
  font-size: 12px;
  color: #8a9d92;
}

.video-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 18px;
  min-height: 200px;
}

.video-card {
  overflow: hidden;
  border-radius: 14px;
}

.card-cover {
  position: relative;
  height: 190px;
  margin: -20px -20px 0;
  overflow: hidden;
  background: #0f1512;
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
  font-size: 13px;
  color: #8a9d92;
  background: #eef3f0;
}

.duration {
  position: absolute;
  right: 10px;
  bottom: 10px;
  padding: 1px 7px;
  font-size: 12px;
  color: #fff;
  background: rgba(0, 0, 0, 0.55);
  border-radius: 5px;
}

.audit-tag {
  position: absolute;
  top: 10px;
  left: 10px;
  padding: 2px 10px;
  font-size: 12px;
  border-radius: 999px;
}

.audit-tag.pending {
  color: #8a5a00;
  background: #fff3d6;
}

.audit-tag.pass {
  color: #1c6f42;
  background: #e3f6ea;
}

.audit-tag.reject {
  color: #a52a2a;
  background: #fdeaea;
}

.preview-btn {
  position: absolute;
  top: 50%;
  left: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 52px;
  height: 52px;
  color: #fff;
  cursor: pointer;
  background: rgba(0, 0, 0, 0.42);
  border: none;
  border-radius: 50%;
  transform: translate(-50%, -50%);
  transition: background 0.2s ease;
}

.preview-btn:hover {
  background: rgba(0, 0, 0, 0.62);
}

.card-body {
  padding-top: 14px;
}

.card-body h3 {
  margin: 0 0 8px;
  overflow: hidden;
  font-size: 16px;
  color: #1f3d2b;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.desc {
  display: -webkit-box;
  margin: 0 0 8px;
  overflow: hidden;
  font-size: 13px;
  line-height: 1.6;
  color: #5d7668;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.reject {
  padding: 8px 10px;
  margin: 0 0 10px;
  font-size: 12px;
  line-height: 1.6;
  color: #a52a2a;
  background: #fdf1f1;
  border-radius: 8px;
}

.stats {
  display: flex;
  gap: 14px;
  font-size: 12px;
  color: #8a9d92;
}

.card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 10px;
  margin-top: 10px;
  border-top: 1px solid #f2f5f3;
}

.time {
  font-size: 12px;
  color: #a3b0a8;
}

.pagination-wrap {
  display: flex;
  justify-content: center;
  margin-top: 26px;
}

/* ------------------------------ 发布弹窗 ------------------------------ */

.upload-box {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 6px;
  align-items: center;
  justify-content: center;
  width: 100%;
  min-height: 168px;
  padding: 18px;
  color: #8a9d92;
  text-align: center;
  background: #f7faf8;
  border: 1px dashed #cfdcd5;
  border-radius: 12px;
}

.upload-box.has-file {
  align-items: stretch;
  padding: 12px;
  background: #0f1512;
  border-style: solid;
}

.upload-box p {
  margin: 0;
  font-size: 13px;
}

.file-input {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  cursor: pointer;
  opacity: 0;
}

.has-file .file-input {
  display: none;
}

.pick-preview {
  width: 100%;
  max-height: 240px;
  background: #000;
  border-radius: 8px;
  object-fit: contain;
}

.file-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
  font-size: 12px;
  color: #cfe0d6;
}

.file-name {
  max-width: 260px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-duration {
  color: #8fe0ad;
}

.cover-row {
  display: flex;
  gap: 18px;
  align-items: center;
  width: 100%;
}

.cover-preview {
  flex-shrink: 0;
  width: 96px;
  height: 170px;
  overflow: hidden;
  background: #f2f6f4;
  border-radius: 10px;
}

.cover-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.cover-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  padding: 8px;
  font-size: 12px;
  color: #96a79c;
  text-align: center;
}

.cover-tip {
  flex: 1;
  min-width: 0;
}

.cover-tip p {
  margin: 0 0 6px;
  font-size: 12px;
  color: #8a9d92;
}

.upload-progress {
  width: 100%;
}

.dialog-tip {
  float: left;
  font-size: 12px;
  line-height: 32px;
  color: #8a9d92;
}

.preview-video {
  width: 100%;
  max-height: 70vh;
  background: #000;
  border-radius: 10px;
}

.preview-meta {
  margin: 10px 0 0;
  font-size: 12px;
  color: #6b7c72;
}

@media (max-width: 720px) {
  .page-head {
    flex-direction: column;
    align-items: flex-start;
  }

  .filter-row {
    flex-direction: column;
    align-items: flex-start;
  }

  .cover-row {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
