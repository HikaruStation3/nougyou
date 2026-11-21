<template>
  <div class="page-container">
    <el-card shadow="never">
      <template #header>
        <div class="file-header">
          <span>MinIO 文件上传</span>
          <el-tag size="small" type="info" effect="plain">对象存储</el-tag>
        </div>
      </template>

      <el-upload
        drag
        :show-file-list="false"
        :http-request="handleUpload"
        :disabled="uploading"
      >
        <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
        <div class="el-upload__text">
          {{ uploading ? "上传中..." : "拖拽文件到此处，或点击选择文件" }}
        </div>
      </el-upload>

      <el-descriptions v-if="uploaded" class="file-result" :column="1" border>
        <el-descriptions-item label="文件名">{{ uploaded.name }}</el-descriptions-item>
        <el-descriptions-item label="访问地址">
          <el-link type="primary" :href="uploaded.url" target="_blank">
            {{ uploaded.url }}
          </el-link>
        </el-descriptions-item>
      </el-descriptions>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref } from "vue";
import { ElMessage, type UploadRequestOptions } from "element-plus";
import { UploadFilled } from "@element-plus/icons-vue";
import FileAPI, { type FileInfo } from "@/api/file";

const uploading = ref(false);
const uploaded = ref<FileInfo>();

async function handleUpload(options: UploadRequestOptions) {
  uploading.value = true;
  try {
    uploaded.value = await FileAPI.uploadFile(options.file as File);
    ElMessage.success("上传成功");
  } finally {
    uploading.value = false;
  }
}
</script>

<style lang="scss" scoped>
.file-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-weight: 600;
}

.file-result {
  margin-top: 20px;
}
</style>
