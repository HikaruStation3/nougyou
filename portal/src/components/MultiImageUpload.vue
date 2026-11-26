<template>
  <div v-loading="uploading" class="multi-upload">
    <div v-for="(url, index) in modelValue" :key="`${index}-${url}`" class="multi-upload__item">
      <el-image
        :src="url"
        fit="cover"
        class="multi-upload__img"
        :preview-src-list="modelValue"
        :initial-index="index"
        preview-teleported
      />
      <span class="multi-upload__del" @click.stop="handleRemove(index)">
        <el-icon><CircleCloseFilled /></el-icon>
      </span>
    </div>
    <el-upload
      v-if="modelValue.length < limit"
      class="multi-upload__add"
      list-type="picture-card"
      :show-file-list="false"
      accept=".jpg,.jpeg,.png"
      :before-upload="handleBeforeUpload"
      :http-request="handleUpload"
    >
      <el-icon><Plus /></el-icon>
    </el-upload>
  </div>
</template>

<script setup>
import { ref } from "vue";
import { ElMessage } from "element-plus";
import { CircleCloseFilled, Plus } from "@element-plus/icons-vue";
import { uploadFile } from "../api/file";

const props = defineProps({
  modelValue: { type: Array, default: () => [] },
  /** 最多上传张数 */
  limit: { type: Number, default: 9 },
  /** 业务类型：idcard / proof / product / avatar（API-113） */
  bizType: { type: String, default: "product" },
  /** 单张最大文件大小（MB） */
  maxSize: { type: Number, default: 5 }
});
const emit = defineEmits(["update:modelValue"]);

const uploading = ref(false);

function handleBeforeUpload(file) {
  if (!file.type.startsWith("image/")) {
    ElMessage.warning("只能上传图片文件");
    return false;
  }
  const ext = file.name.split(".").pop()?.toLowerCase();
  if (!["jpg", "jpeg", "png"].includes(ext)) {
    ElMessage.warning("图片仅支持 jpg / jpeg / png 格式");
    return false;
  }
  if (file.size > props.maxSize * 1024 * 1024) {
    ElMessage.warning(`图片大小不能超过 ${props.maxSize}MB`);
    return false;
  }
  if (props.modelValue.length >= props.limit) {
    ElMessage.warning(`最多上传 ${props.limit} 张图片`);
    return false;
  }
  return true;
}

async function handleUpload(options) {
  uploading.value = true;
  try {
    const info = await uploadFile(options.file, props.bizType);
    emit("update:modelValue", [...props.modelValue, info.url]);
    ElMessage.success("上传成功");
    return info;
  } finally {
    uploading.value = false;
  }
}

function handleRemove(index) {
  const next = [...props.modelValue];
  next.splice(index, 1);
  emit("update:modelValue", next);
}
</script>

<style scoped>
.multi-upload {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.multi-upload__item {
  position: relative;
  width: 100px;
  height: 100px;
  overflow: hidden;
  border: 1px solid var(--el-border-color);
  border-radius: 6px;
}

.multi-upload__img {
  width: 100%;
  height: 100%;
}

.multi-upload__del {
  position: absolute;
  top: 2px;
  right: 2px;
  z-index: 2;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  color: #fff;
  background: rgba(0, 0, 0, 0.55);
  border-radius: 50%;
  cursor: pointer;
}

.multi-upload__add :deep(.el-upload--picture-card) {
  width: 100px;
  height: 100px;
}
</style>
