<template>
  <div class="page-container">
    <el-card shadow="never" class="chat-card">
      <template #header>
        <div class="chat-header">
          <span>AI 对话</span>
          <el-tag size="small" type="info" effect="plain">Spring AI</el-tag>
        </div>
      </template>

      <div ref="messageBoxRef" class="chat-body">
        <el-empty v-if="messages.length === 0" description="有什么可以帮你的吗？" />
        <div v-for="(item, index) in messages" :key="index" :class="['message', item.role]">
          <el-avatar :size="32" :icon="item.role === 'user' ? User : MagicStick" />
          <div class="bubble">{{ item.content }}</div>
        </div>
        <div v-if="loading" class="message assistant">
          <el-avatar :size="32" :icon="MagicStick" />
          <div class="bubble">正在思考...</div>
        </div>
      </div>

      <div class="chat-input">
        <el-input
          v-model="input"
          type="textarea"
          :rows="3"
          maxlength="2000"
          show-word-limit
          placeholder="输入问题，Enter 发送，Shift + Enter 换行"
          @keydown.enter.exact.prevent="handleSend"
        />
        <el-button type="primary" :loading="loading" :disabled="!input.trim()" @click="handleSend">
          发送
        </el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { nextTick, ref } from "vue";
import { ElMessage } from "element-plus";
import { MagicStick, User } from "@element-plus/icons-vue";
import AIAPI from "@/api/ai";

interface Message {
  role: "user" | "assistant";
  content: string;
}

const messages = ref<Message[]>([]);
const input = ref("");
const loading = ref(false);
const messageBoxRef = ref<HTMLElement>();

async function scrollToBottom() {
  await nextTick();
  if (messageBoxRef.value) {
    messageBoxRef.value.scrollTop = messageBoxRef.value.scrollHeight;
  }
}

async function handleSend() {
  const content = input.value.trim();
  if (!content || loading.value) return;

  messages.value.push({ role: "user", content });
  input.value = "";
  loading.value = true;
  await scrollToBottom();

  try {
    const data = await AIAPI.chat({ message: content });
    messages.value.push({ role: "assistant", content: data.content });
  } catch {
    // 错误提示已由请求拦截器统一处理
  } finally {
    loading.value = false;
    await scrollToBottom();
  }
}
</script>

<style lang="scss" scoped>
.chat-card {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 120px);
}

.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-weight: 600;
}

.chat-body {
  flex: 1;
  padding: 8px 4px;
  overflow-y: auto;
}

.message {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;

  &.user {
    flex-direction: row-reverse;

    .bubble {
      color: #fff;
      background: var(--el-color-primary);
    }
  }

  .bubble {
    max-width: 70%;
    padding: 10px 14px;
    line-height: 1.6;
    white-space: pre-wrap;
    background: var(--el-fill-color-light);
    border-radius: 10px;
  }
}

.chat-input {
  display: flex;
  gap: 12px;
  align-items: flex-end;
  padding-top: 12px;
  border-top: 1px solid var(--el-border-color-lighter);

  :deep(.el-textarea) {
    flex: 1;
  }
}
</style>
