<template>
  <div class="ai-chat">
    <!-- 会话列表 -->
    <aside class="session-panel">
      <div class="panel-head">
        <span class="panel-title">会话记录</span>
        <el-tooltip content="新建对话" placement="right">
          <el-button type="primary" :icon="Plus" circle size="small" @click="startNewSession" />
        </el-tooltip>
      </div>

      <el-scrollbar v-loading="sessionLoading" class="session-scroll">
        <div
          v-for="item in sessions"
          :key="item.id"
          :class="['session-item', { active: item.id === sessionId }]"
          @click="openSession(item)"
        >
          <div class="session-title">{{ item.title || "新对话" }}</div>
          <div class="session-meta">
            <el-tag size="small" effect="plain" type="info">{{ item.sceneName }}</el-tag>
            <span class="session-time">{{ formatRelative(item.updateTime) }}</span>
          </div>
        </div>
        <el-empty
          v-if="!sessionLoading && sessions.length === 0"
          description="还没有对话，问一句试试"
          :image-size="56"
        />
      </el-scrollbar>
    </aside>

    <!-- 对话区 -->
    <section class="chat-panel">
      <header class="chat-header">
        <div class="bot-info">
          <el-avatar :size="36" class="bot-avatar" :icon="Service" />
          <div>
            <div class="bot-name">助农 AI 客服</div>
            <div class="bot-desc">{{ sceneName || activeSceneLabel }}</div>
          </div>
        </div>
        <el-select
          v-model="scene"
          size="small"
          class="scene-select"
          :disabled="streaming"
          @change="handleSceneChange"
        >
          <el-option v-for="opt in SCENE_OPTIONS" :key="opt.value" :label="opt.label" :value="opt.value" />
        </el-select>
      </header>

      <div ref="scrollRef" class="chat-body">
        <div v-if="messages.length === 0" class="welcome">
          <el-avatar :size="52" class="bot-avatar" :icon="Service" />
          <h3>你好，我是钟山镇助农平台的 AI 客服</h3>
          <p>商品上下架、订单处理、委托销售、售后流程，都可以问我</p>
          <div class="examples">
            <span v-for="example in examples" :key="example" class="example" @click="sendText(example)">
              {{ example }}
            </span>
          </div>
        </div>

        <div v-for="(item, index) in messages" :key="index" :class="['msg', item.role]">
          <el-avatar
            :size="32"
            class="msg-avatar"
            :icon="item.role === 'user' ? User : Service"
          />
          <div class="msg-main">
            <div :class="['bubble', { 'bubble-error': item.error }]">
              <span class="bubble-text">{{ item.content }}</span>
              <span v-if="item.streaming" class="caret" />
            </div>
            <el-tag v-if="item.flagged" size="small" type="warning" effect="plain" class="flag-tag">
              已触发内容管控
            </el-tag>
          </div>
        </div>
      </div>

      <footer class="chat-input">
        <el-input
          v-model="input"
          type="textarea"
          :rows="3"
          resize="none"
          maxlength="1000"
          show-word-limit
          placeholder="输入问题，Enter 发送，Shift + Enter 换行"
          @keydown.enter.exact.prevent="handleSend"
        />
        <div class="input-actions">
          <span class="input-hint">AI 生成内容仅供参考，重要信息请与平台客服核实</span>
          <div class="buttons">
            <el-button v-if="streaming" @click="stopStreaming">停止生成</el-button>
            <el-button type="primary" :disabled="!input.trim() || streaming" @click="handleSend">
              发送
            </el-button>
          </div>
        </div>
      </footer>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from "vue";
import { ElMessage } from "element-plus";
import { Plus, Service, User } from "@element-plus/icons-vue";
import AIAPI, { type AiSceneCode, type ChatSession } from "@/api/ai";

interface DisplayMessage {
  role: "user" | "assistant";
  content: string;
  streaming?: boolean;
  flagged?: boolean;
  error?: boolean;
}

const SCENE_OPTIONS: { label: string; value: AiSceneCode }[] = [
  { label: "经营咨询", value: "BIZ_CONSULT" },
  { label: "数据统计", value: "SALE_STAT" },
  { label: "可行性分析", value: "SALE_ANALYSIS" },
];

const examples = [
  "我的地瓜干卖不动，有什么建议？",
  "买家申请退货退款怎么处理？",
  "怎么把山茶油设置成活动价？",
];

const sessions = ref<ChatSession[]>([]);
const sessionId = ref<number | null>(null);
const sceneName = ref("");
const scene = ref<AiSceneCode>("BIZ_CONSULT");
const messages = ref<DisplayMessage[]>([]);
const input = ref("");
const sessionLoading = ref(false);
const streaming = ref(false);
const scrollRef = ref<HTMLElement>();

let abortController: AbortController | null = null;

const activeSceneLabel = computed(
  () => SCENE_OPTIONS.find((item) => item.value === scene.value)?.label ?? "经营咨询"
);

function scrollToBottom() {
  const el = scrollRef.value;
  if (el) {
    el.scrollTop = el.scrollHeight;
  }
}

async function scrollToBottomNextTick() {
  await nextTick();
  scrollToBottom();
}

function formatRelative(value?: string) {
  if (!value) return "";
  const date = new Date(value.replace("T", " ").replace(/-/g, "/"));
  if (Number.isNaN(date.getTime())) return "";
  const diff = Date.now() - date.getTime();
  const minute = 60 * 1000;
  if (diff < minute) return "刚刚";
  if (diff < 60 * minute) return `${Math.floor(diff / minute)} 分钟前`;
  const month = `${date.getMonth() + 1}`.padStart(2, "0");
  const day = `${date.getDate()}`.padStart(2, "0");
  const hour = `${date.getHours()}`.padStart(2, "0");
  const minuteText = `${date.getMinutes()}`.padStart(2, "0");
  return `${month}-${day} ${hour}:${minuteText}`;
}

async function loadSessions() {
  sessionLoading.value = true;
  try {
    const data = await AIAPI.listSessions({ pageNum: 1, pageSize: 30 });
    sessions.value = data.list ?? [];
  } catch {
    // 拦截器已提示
  } finally {
    sessionLoading.value = false;
  }
}

async function openSession(item: ChatSession) {
  if (streaming.value) {
    ElMessage.warning("正在生成回答，请稍候");
    return;
  }
  sessionId.value = item.id;
  sceneName.value = item.sceneName;
  scene.value = item.sceneCode;
  messages.value = [];
  try {
    const data = await AIAPI.listMessages(item.id, { pageNum: 1, pageSize: 100 });
    messages.value = (data.list ?? []).map((msg) => ({
      role: msg.role,
      content: msg.content,
      flagged: msg.flagged,
    }));
    await scrollToBottomNextTick();
  } catch {
    // 拦截器已提示
  }
}

function startNewSession() {
  if (streaming.value) {
    ElMessage.warning("正在生成回答，请稍候");
    return;
  }
  sessionId.value = null;
  sceneName.value = "";
  messages.value = [];
  input.value = "";
}

function handleSceneChange() {
  if (sessionId.value || messages.value.length) {
    startNewSession();
  }
}

async function sendText(text: string) {
  input.value = text;
  await handleSend();
}

async function handleSend() {
  const question = input.value.trim();
  if (!question || streaming.value) return;

  input.value = "";
  streaming.value = true;

  messages.value.push({ role: "user", content: question });
  messages.value.push({ role: "assistant", content: "", streaming: true });
  // 通过数组下标读写消息，保证流式增量能触发视图更新
  const replyIndex = messages.value.length - 1;
  await scrollToBottomNextTick();

  abortController = new AbortController();

  try {
    await AIAPI.chatStream(
      {
        sessionId: sessionId.value ?? undefined,
        sceneCode: scene.value,
        question,
      },
      {
        onMeta(meta) {
          sessionId.value = meta.sessionId;
          sceneName.value = meta.sceneName;
        },
        onDelta(content) {
          const current = messages.value[replyIndex];
          if (current) current.content += content;
          scrollToBottom();
        },
        onDone(done) {
          const current = messages.value[replyIndex];
          if (current) {
            current.content = done.content;
            current.flagged = done.flagged;
            current.streaming = false;
          }
          scrollToBottom();
        },
        onError(message) {
          const current = messages.value[replyIndex];
          if (current) {
            current.content = message;
            current.error = true;
            current.streaming = false;
          }
        },
      },
      abortController.signal
    );
  } catch (error: any) {
    const current = messages.value[replyIndex];
    if (error?.name === "AbortError") {
      if (current) current.content = current.content || "已停止生成";
    } else if (current) {
      current.content = error?.message || "AI 服务暂时不可用，请稍后重试";
      current.error = true;
    }
  } finally {
    const current = messages.value[replyIndex];
    if (current) current.streaming = false;
    streaming.value = false;
    abortController = null;
    await loadSessions();
    await scrollToBottomNextTick();
  }
}

function stopStreaming() {
  abortController?.abort();
}

onMounted(async () => {
  await loadSessions();
  if (sessions.value.length > 0) {
    await openSession(sessions.value[0]);
  }
});

onBeforeUnmount(() => {
  abortController?.abort();
});
</script>

<style lang="scss" scoped>
.ai-chat {
  display: flex;
  gap: 16px;
  height: calc(100vh - 110px);
  min-height: 520px;
}

/* 会话列表 */
.session-panel {
  display: flex;
  flex-direction: column;
  width: 240px;
  flex-shrink: 0;
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 10px;
  overflow: hidden;
}

.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.panel-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.session-scroll {
  flex: 1;
  padding: 8px;
}

.session-item {
  padding: 10px 12px;
  margin-bottom: 6px;
  border-radius: 8px;
  cursor: pointer;
  transition: background-color 0.2s;

  &:hover {
    background: var(--el-fill-color-light);
  }

  &.active {
    background: var(--el-color-primary-light-9);
  }
}

.session-title {
  overflow: hidden;
  font-size: 13px;
  color: var(--el-text-color-primary);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.session-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 6px;
}

.session-time {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

/* 对话区 */
.chat-panel {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-width: 0;
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 10px;
  overflow: hidden;
}

.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 18px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.bot-info {
  display: flex;
  gap: 10px;
  align-items: center;
}

.bot-avatar {
  color: #fff;
  background: linear-gradient(135deg, var(--el-color-primary), #48b07a);
}

.bot-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.bot-desc {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.scene-select {
  width: 130px;
}

.chat-body {
  flex: 1;
  padding: 18px;
  overflow-y: auto;
  background: var(--el-fill-color-blank);
}

.welcome {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 48px 0 24px;
  text-align: center;

  h3 {
    margin: 14px 0 6px;
    font-size: 16px;
    font-weight: 600;
    color: var(--el-text-color-primary);
  }

  p {
    margin: 0;
    font-size: 13px;
    color: var(--el-text-color-secondary);
  }
}

.examples {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  justify-content: center;
  margin-top: 20px;
}

.example {
  padding: 8px 14px;
  font-size: 13px;
  color: var(--el-text-color-regular);
  cursor: pointer;
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 16px;
  transition: all 0.2s;

  &:hover {
    color: var(--el-color-primary);
    border-color: var(--el-color-primary-light-5);
  }
}

.msg {
  display: flex;
  gap: 10px;
  margin-bottom: 18px;

  &.user {
    flex-direction: row-reverse;

    .bubble {
      color: #fff;
      background: var(--el-color-primary);
    }

    .msg-main {
      align-items: flex-end;
    }
  }
}

.msg-avatar {
  flex-shrink: 0;
  background: var(--el-fill-color-dark);
}

.msg-main {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  max-width: 76%;
}

.bubble {
  padding: 10px 14px;
  font-size: 14px;
  line-height: 1.7;
  color: var(--el-text-color-primary);
  word-break: break-word;
  white-space: pre-wrap;
  background: var(--el-fill-color-light);
  border-radius: 10px;

  &.bubble-error {
    color: var(--el-color-danger);
    background: var(--el-color-danger-light-9);
  }
}

.bubble-text {
  white-space: pre-wrap;
}

/* 流式输出光标 */
.caret {
  display: inline-block;
  width: 2px;
  height: 15px;
  margin-left: 2px;
  vertical-align: -2px;
  background: var(--el-color-primary);
  animation: blink 1s steps(1) infinite;
}

@keyframes blink {
  50% {
    opacity: 0;
  }
}

.flag-tag {
  margin-top: 6px;
}

.chat-input {
  padding: 12px 18px 14px;
  border-top: 1px solid var(--el-border-color-lighter);
}

.input-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 8px;
}

.input-hint {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.buttons {
  display: flex;
  gap: 8px;
}
</style>
