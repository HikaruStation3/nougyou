<template>
  <div class="workbench">
    <!-- 会话队列 -->
    <aside class="session-panel">
      <div class="panel-head">
        <span class="panel-title">咨询队列</span>
        <el-button size="small" :icon="Refresh" circle @click="loadSessions" />
      </div>

      <div class="panel-filter">
        <el-radio-group v-model="query.status" size="small" @change="loadSessions">
          <el-radio-button :value="''">全部</el-radio-button>
          <el-radio-button :value="0">待接入</el-radio-button>
          <el-radio-button :value="1">进行中</el-radio-button>
          <el-radio-button :value="2">已结束</el-radio-button>
        </el-radio-group>
        <div class="filter-row">
          <el-select v-model="query.targetType" size="small" style="width: 116px" @change="loadSessions">
            <el-option label="平台客服" :value="1" />
            <el-option label="商家会话" :value="2" />
          </el-select>
          <el-checkbox v-model="query.mine" size="small" @change="loadSessions">只看我接待</el-checkbox>
        </div>
        <el-input
          v-model="query.keyword"
          size="small"
          clearable
          placeholder="按昵称或会话号搜索"
          @keyup.enter="loadSessions"
          @clear="loadSessions"
        />
      </div>

      <el-scrollbar v-loading="sessionLoading" class="session-scroll">
        <div
          v-for="item in sessions"
          :key="item.id"
          :class="['session-item', { active: item.id === activeId }]"
          @click="openSession(item)"
        >
          <div class="session-row">
            <span class="session-title">{{ item.userNickname }}</span>
            <span class="session-time">{{ formatRelative(item.lastMsgTime) }}</span>
          </div>
          <div class="session-row">
            <span class="session-preview">{{ item.lastMsgPreview || "暂无消息" }}</span>
            <el-badge v-if="item.unreadCount > 0" :value="item.unreadCount" :max="99" />
          </div>
          <div class="session-tags">
            <el-tag size="small" effect="plain" :type="statusTag(item.status)">
              {{ statusText(item.status) }}
            </el-tag>
            <el-tag size="small" effect="plain" type="info">
              {{ item.targetType === 1 ? "平台客服" : "商家会话" }}
            </el-tag>
            <el-tag v-if="item.targetType === 2" size="small" effect="plain" type="warning">
              {{ item.targetName }}
            </el-tag>
          </div>
        </div>
        <el-empty
          v-if="!sessionLoading && sessions.length === 0"
          description="暂无会话"
          :image-size="56"
        />
      </el-scrollbar>
    </aside>

    <!-- 沟通区 -->
    <section class="chat-panel">
      <template v-if="active">
        <header class="chat-header">
          <div class="peer">
            <el-avatar :size="36" :icon="User" />
            <div>
              <div class="peer-name">
                {{ active.userNickname }}
                <el-tag size="small" effect="plain" :type="statusTag(active.status)">
                  {{ statusText(active.status) }}
                </el-tag>
              </div>
              <div class="peer-desc">
                {{ active.sessionNo }} · {{ active.userRole === "farmer" ? "农户商家" : "买家" }}
                <template v-if="active.targetType === 2"> · 咨询商家 {{ active.targetName }}</template>
                <template v-if="active.productName"> · 商品 {{ active.productName }}</template>
                <template v-if="active.adminName"> · 接待 {{ active.adminName }}</template>
              </div>
            </div>
          </div>
          <div class="header-actions">
            <el-button
              v-if="active.status === 0 && active.targetType === 1"
              type="primary"
              size="small"
              :loading="acting"
              @click="handleAccept"
            >
              接入会话
            </el-button>
            <el-button
              v-if="active.status !== 2"
              size="small"
              type="danger"
              plain
              @click="openClose"
            >
              结束会话
            </el-button>
          </div>
        </header>

        <div v-if="active.status === 0" class="notice">该会话尚未接入，接入后即可回复用户</div>
        <div v-else-if="active.status === 2" class="notice closed">
          会话已于 {{ active.endTime || "-" }} 结束
        </div>

        <div ref="scrollRef" v-loading="loadingMessages" class="chat-body">
          <div v-if="!loadingMessages && messages.length === 0" class="empty-chat">
            还没有消息
          </div>
          <div
            v-for="item in messages"
            :key="item.id"
            :class="['msg', item.senderRole === 'admin' ? 'mine' : 'peer']"
          >
            <el-avatar :size="32" :src="item.senderAvatar || undefined" :icon="roleIcon(item)" />
            <div class="msg-main">
              <div class="msg-meta">
                <span class="sender">{{ item.senderName }}</span>
                <span class="role-tag">{{ roleText(item.senderRole) }}</span>
                <span class="time">{{ formatTime(item.createTime) }}</span>
              </div>
              <div class="bubble">
                <el-image
                  v-if="item.contentType === 2 && isImageUrl(item.content)"
                  :src="item.content"
                  :preview-src-list="[item.content]"
                  fit="cover"
                  class="bubble-image"
                />
                <span v-else-if="item.contentType === 2" class="bubble-text">
                  [图片] {{ item.content }}
                </span>
                <span v-else class="bubble-text">{{ item.content }}</span>
              </div>
            </div>
          </div>
        </div>

        <footer class="chat-input">
          <el-input
            v-model="input"
            type="textarea"
            :rows="3"
            resize="none"
            maxlength="500"
            show-word-limit
            :disabled="active.status === 2"
            :placeholder="active.status === 2 ? '会话已结束' : '输入回复内容，Enter 发送，Shift + Enter 换行'"
            @keydown.enter.exact.prevent="handleSend"
          />
          <div class="input-actions">
            <span class="input-hint">回复将以「客服」身份发送给用户</span>
            <el-button
              type="primary"
              :loading="sending"
              :disabled="active.status === 2 || !input.trim()"
              @click="handleSend"
            >
              发送
            </el-button>
          </div>
        </footer>
      </template>

      <div v-else class="chat-empty">
        <el-empty description="从左侧选择一个会话开始接待" />
      </div>
    </section>

    <!-- 结束会话 -->
    <el-dialog v-model="closeVisible" title="结束会话" width="460px">
      <el-form label-width="80px">
        <el-form-item label="处理小结">
          <el-input
            v-model="closeSummary"
            type="textarea"
            :rows="4"
            maxlength="200"
            show-word-limit
            placeholder="可选，填写后会作为一条客服消息留存在会话中"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="closeVisible = false">取消</el-button>
        <el-button type="danger" :loading="acting" @click="handleClose">确认结束</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import { Refresh, Service, Shop, User } from "@element-plus/icons-vue";
import ConsultAPI, {
  type ConsultMessage,
  type ConsultSession,
  type ServiceSessionStatus,
  type ServiceTargetType,
} from "@/api/consult";

const sessions = ref<ConsultSession[]>([]);
const active = ref<ConsultSession | null>(null);
const activeId = computed(() => active.value?.id ?? 0);
const messages = ref<ConsultMessage[]>([]);
const sessionLoading = ref(false);
const loadingMessages = ref(false);
const sending = ref(false);
const acting = ref(false);
const input = ref("");
const scrollRef = ref<HTMLElement>();

const closeVisible = ref(false);
const closeSummary = ref("");

const query = reactive<{
  status: ServiceSessionStatus | "";
  targetType: ServiceTargetType;
  mine: boolean;
  keyword: string;
}>({
  status: 0,
  targetType: 1,
  mine: false,
  keyword: "",
});

let pollTimer: ReturnType<typeof setInterval> | null = null;
let polling = false;

function statusText(status: number) {
  if (status === 0) return "待接入";
  if (status === 1) return "进行中";
  return "已结束";
}

function statusTag(status: number): "warning" | "success" | "info" {
  if (status === 0) return "warning";
  if (status === 1) return "success";
  return "info";
}

function roleText(role: string) {
  if (role === "admin") return "客服";
  if (role === "farmer") return "商家";
  return "买家";
}

function roleIcon(message: ConsultMessage) {
  if (message.senderRole === "admin") return Service;
  return message.senderRole === "farmer" ? Shop : User;
}

function isImageUrl(content: string) {
  return content.startsWith("/") || content.startsWith("http");
}

function formatTime(value?: string | null) {
  if (!value) return "";
  return value.replace("T", " ").slice(5, 16);
}

function formatRelative(value?: string | null) {
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

function scrollToBottom() {
  nextTick(() => {
    const el = scrollRef.value;
    if (el) el.scrollTop = el.scrollHeight;
  });
}

async function loadSessions() {
  sessionLoading.value = sessions.value.length === 0;
  try {
    const data = await ConsultAPI.adminSessions({
      pageNum: 1,
      pageSize: 50,
      status: query.status === "" ? undefined : query.status,
      targetType: query.targetType,
      mine: query.mine || undefined,
      keyword: query.keyword.trim() || undefined,
    });
    sessions.value = data.list ?? [];
    if (active.value) {
      const latest = sessions.value.find((item) => item.id === active.value!.id);
      if (latest) {
        active.value = { ...active.value, ...latest };
      }
    }
  } catch {
    // 拦截器已提示
  } finally {
    sessionLoading.value = false;
  }
}

async function reloadMessages() {
  if (!active.value) return;
  loadingMessages.value = true;
  try {
    const data = await ConsultAPI.adminMessages(active.value.id, { pageSize: 100 });
    messages.value = data.list ?? [];
    await ConsultAPI.adminMarkRead(active.value.id).catch(() => undefined);
    await loadSessions();
    scrollToBottom();
  } catch {
    // 拦截器已提示
  } finally {
    loadingMessages.value = false;
  }
}

async function openSession(session: ConsultSession) {
  if (active.value?.id === session.id) return;
  active.value = session;
  messages.value = [];
  await reloadMessages();
}

async function pollMessages() {
  if (!active.value || polling) return;
  polling = true;
  try {
    const lastId = messages.value.length ? messages.value[messages.value.length - 1].id : 0;
    const data = await ConsultAPI.adminMessages(active.value.id, {
      lastId: lastId || undefined,
      pageSize: 50,
    });
    const incoming = data.list ?? [];
    if (incoming.length > 0) {
      const exists = new Set(messages.value.map((item) => item.id));
      const fresh = incoming.filter((item) => !exists.has(item.id));
      if (fresh.length > 0) {
        messages.value.push(...fresh);
        scrollToBottom();
        await ConsultAPI.adminMarkRead(active.value.id).catch(() => undefined);
        await loadSessions();
      }
    }
  } catch {
    // 静默重试
  } finally {
    polling = false;
  }
}

async function handleAccept() {
  if (!active.value) return;
  acting.value = true;
  try {
    const session = await ConsultAPI.accept(active.value.id);
    active.value = { ...active.value, ...session };
    ElMessage.success("已接入会话");
    // 接入后会话不再是「待接入」，切到「进行中」保证它还在队列里可见
    query.status = 1;
    await loadSessions();
  } catch {
    // 拦截器已提示
  } finally {
    acting.value = false;
  }
}

function openClose() {
  closeSummary.value = "";
  closeVisible.value = true;
}

async function handleClose() {
  if (!active.value) return;
  acting.value = true;
  try {
    const session = await ConsultAPI.close(active.value.id, closeSummary.value.trim() || undefined);
    active.value = { ...active.value, ...session };
    closeVisible.value = false;
    ElMessage.success("会话已结束");
    // 结束后切到「已结束」，保持队列与右侧窗口一致
    query.status = 2;
    await loadSessions();
    await reloadMessages();
  } catch {
    // 拦截器已提示
  } finally {
    acting.value = false;
  }
}

async function handleSend() {
  const text = input.value.trim();
  if (!text || !active.value || sending.value) return;
  if (active.value.status === 2) {
    ElMessage.warning("会话已结束");
    return;
  }
  sending.value = true;
  try {
    const message = await ConsultAPI.adminSend(active.value.id, { contentType: 1, content: text });
    messages.value.push(message);
    input.value = "";
    scrollToBottom();
    await loadSessions();
  } catch {
    // 拦截器已提示
  } finally {
    sending.value = false;
  }
}

onMounted(async () => {
  await loadSessions();
  if (sessions.value.length > 0) {
    await openSession(sessions.value[0]);
  }
  pollTimer = setInterval(() => {
    void pollMessages();
  }, 3000);
});

onBeforeUnmount(() => {
  if (pollTimer) clearInterval(pollTimer);
});
</script>

<style lang="scss" scoped>
.workbench {
  display: flex;
  gap: 16px;
  height: calc(100vh - 110px);
  min-height: 520px;
}

.session-panel {
  display: flex;
  flex-direction: column;
  width: 300px;
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
  padding: 14px 16px 10px;
}

.panel-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.panel-filter {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 0 12px 10px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.filter-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
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

.session-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 4px;
}

.session-title {
  overflow: hidden;
  font-size: 13px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.session-time {
  flex-shrink: 0;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.session-preview {
  overflow: hidden;
  flex: 1;
  font-size: 12px;
  color: var(--el-text-color-secondary);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.session-tags {
  display: flex;
  gap: 6px;
}

.chat-panel {
  display: flex;
  flex: 1;
  flex-direction: column;
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

.peer {
  display: flex;
  align-items: center;
  gap: 10px;
}

.peer-name {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.peer-desc {
  margin-top: 3px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.notice {
  padding: 8px 18px;
  font-size: 12px;
  color: var(--el-color-warning-dark-2);
  background: var(--el-color-warning-light-9);

  &.closed {
    color: var(--el-text-color-secondary);
    background: var(--el-fill-color-light);
  }
}

.chat-body {
  flex: 1;
  padding: 18px;
  overflow-y: auto;
  background: var(--el-fill-color-blank);
}

.empty-chat {
  padding-top: 80px;
  font-size: 13px;
  color: var(--el-text-color-secondary);
  text-align: center;
}

.msg {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;

  &.mine {
    flex-direction: row-reverse;

    .msg-main {
      align-items: flex-end;
    }

    .msg-meta {
      flex-direction: row-reverse;
    }

    .bubble {
      color: #fff;
      background: var(--el-color-primary);
    }
  }
}

.msg-main {
  display: flex;
  flex-direction: column;
  max-width: 62%;
}

.msg-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 4px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.role-tag {
  padding: 0 4px;
  border: 1px solid var(--el-border-color);
  border-radius: 3px;
}

.bubble {
  padding: 9px 12px;
  font-size: 13px;
  line-height: 1.6;
  color: var(--el-text-color-primary);
  word-break: break-word;
  white-space: pre-wrap;
  background: var(--el-fill-color-light);
  border-radius: 8px;
}

.bubble-image {
  max-width: 220px;
  max-height: 220px;
  border-radius: 6px;
}

.chat-input {
  padding: 12px 18px 14px;
  border-top: 1px solid var(--el-border-color-lighter);
}

.input-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 10px;
}

.input-hint {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.chat-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
}
</style>
