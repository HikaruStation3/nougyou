<template>
  <el-upload
    v-loading="uploading"
    class="single-upload"
    list-type="picture-card"
    :show-file-list="false"
    accept=".jpg,.jpeg,.png"
    :before-upload="handleBeforeUpload"
    :http-request="handleUpload"
  >
    <template v-if="modelValue">
      <img :src="modelValue" class="single-upload__img" alt="图片" />
      <span class="single-upload__del" @click.stop="handleRemove">
        <el-icon><Delete /></el-icon>
      </span>
    </template>
    <el-icon v-else><Plus /></el-icon>
  </el-upload>
</template>

<script setup>
import { ref } from "vue";
import { ElMessage } from "element-plus";
import { Delete, Plus } from "@element-plus/icons-vue";
import { uploadFile } from "../api/file";

const props = defineProps({
  modelValue: { type: String, default: "" },
  /** 业务类型：idcard / proof / product / avatar（API-113） */
  bizType: { type: String, default: "product" },
  /** 最大文件大小（MB） */
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
  return true;
}

async function handleUpload(options) {
  uploading.value = true;
  try {
    const info = await uploadFile(options.file, props.bizType);
    emit("update:modelValue", info.url);
    ElMessage.success("上传成功");
    return info;
  } finally {
    uploading.value = false;
  }
}

function handleRemove() {
  emit("update:modelValue", "");
}
</script>

<style scoped>
.single-upload :deep(.el-upload--picture-card) {
  position: relative;
  width: 120px;
  height: 120px;
  overflow: hidden;
}

.single-upload__img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.single-upload__del {
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
</style>
