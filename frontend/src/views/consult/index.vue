<template>
  <div class="consult">
    <!-- 会话列表 -->
    <aside class="session-panel">
      <div class="panel-head">
        <span class="panel-title">我的会话</span>
        <el-button
          v-if="canCreate"
          type="primary"
          :icon="Plus"
          circle
          size="small"
          @click="openCreate"
        />
      </div>

      <div class="panel-filter">
        <el-radio-group v-model="statusFilter" size="small" @change="loadSessions">
          <el-radio-button :value="''">全部</el-radio-button>
          <el-radio-button :value="1">进行中</el-radio-button>
          <el-radio-button :value="2">已结束</el-radio-button>
        </el-radio-group>
      </div>

      <el-scrollbar v-loading="sessionLoading" class="session-scroll">
        <div
          v-for="item in sessions"
          :key="item.id"
          :class="['session-item', { active: item.id === activeId }]"
          @click="openSession(item)"
        >
          <div class="session-row">
            <span class="session-title">{{ item.title }}</span>
            <span class="session-time">{{ formatRelative(item.lastMsgTime) }}</span>
          </div>
          <div class="session-row">
            <span class="session-preview">{{ item.lastMsgPreview || "暂无消息" }}</span>
            <el-badge v-if="item.unreadCount > 0" :value="item.unreadCount" :max="99" class="unread" />
          </div>
          <div class="session-tags">
            <el-tag size="small" effect="plain" :type="statusTag(item.status)">
              {{ statusText(item.status) }}
            </el-tag>
            <el-tag v-if="item.productName" size="small" effect="plain" type="warning">
              {{ item.productName }}
            </el-tag>
          </div>
        </div>
        <el-empty
          v-if="!sessionLoading && sessions.length === 0"
          description="还没有会话，联系客服或商家试试"
          :image-size="56"
        />
      </el-scrollbar>
    </aside>

    <!-- 会话窗口 -->
    <section class="chat-panel">
      <template v-if="active">
        <header class="chat-header">
          <div class="peer">
            <el-avatar :size="36" :icon="active.targetType === 1 ? Service : Shop" />
            <div>
              <div class="peer-name">
                {{ active.title }}
                <el-tag size="small" effect="plain" :type="statusTag(active.status)">
                  {{ statusText(active.status) }}
                </el-tag>
              </div>
              <div class="peer-desc">
                会话号 {{ active.sessionNo }}
                <template v-if="active.adminName"> · 接待客服 {{ active.adminName }}</template>
                <template v-if="active.productName"> · 咨询商品 {{ active.productName }}</template>
              </div>
            </div>
          </div>
          <div class="header-actions">
            <el-button size="small" :icon="Refresh" @click="reloadMessages">刷新</el-button>
          </div>
        </header>

        <div v-if="active.status === 0" class="notice">
          已提交咨询，正在等待平台客服接入，请稍候…
        </div>
        <div v-else-if="active.status === 2" class="notice closed">
          会话已于 {{ active.endTime || "-" }} 结束，如需继续咨询请重新发起。
        </div>

        <div ref="scrollRef" v-loading="loadingMessages" class="chat-body">
          <div v-if="!loadingMessages && messages.length === 0" class="empty-chat">
            还没有消息，先打个招呼吧
          </div>

          <div
            v-for="item in messages"
            :key="item.id"
            :class="['msg', isMine(item) ? 'mine' : 'peer']"
          >
            <el-avatar :size="32" :src="item.senderAvatar || undefined" :icon="avatarIcon(item)" />
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
            :placeholder="active.status === 2 ? '会话已结束，无法继续发送' : '输入消息，Enter 发送，Shift + Enter 换行'"
            @keydown.enter.exact.prevent="handleSend"
          />
          <div class="input-actions">
            <span class="input-hint">
              {{
                active.status === 2
                  ? "会话已结束，如需继续咨询请重新发起"
                  : active.targetType === 1
                    ? "平台客服会尽快回复你"
                    : "直接与商家沟通，注意保护个人信息"
              }}
            </span>
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
        <el-empty description="请选择左侧会话，或发起一次新的咨询" />
      </div>
    </section>

    <!-- 发起咨询 -->
    <el-dialog v-model="createVisible" title="发起咨询" width="460px">
      <el-form label-width="84px">
        <el-form-item label="咨询对象">
          <el-radio-group v-model="createForm.targetType">
            <el-radio :value="1">平台客服</el-radio>
            <el-radio :value="2">联系商家</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="createForm.targetType === 2" label="选择商家">
          <el-select
            v-model="createForm.targetId"
            placeholder="请选择要咨询的农户商家"
            style="width: 100%"
          >
            <el-option
              v-for="farmer in farmers"
              :key="farmer.id"
              :label="farmer.nickname"
              :value="farmer.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="submitCreate">开始咨询</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import { Plus, Refresh, Service, Shop, User } from "@element-plus/icons-vue";
import ConsultAPI, {
  type ConsultMessage,
  type ConsultSession,
  type ConsultTarget,
  type ServiceSessionStatus,
  type ServiceTargetType,
} from "@/api/consult";
import { hasPerm } from "@/utils/auth";
import { useUserStore } from "@/stores/user";

const userStore = useUserStore();
const canCreate = computed(() => hasPerm("service:create"));

const sessions = ref<ConsultSession[]>([]);
const active = ref<ConsultSession | null>(null);
const activeId = computed(() => active.value?.id ?? 0);
const messages = ref<ConsultMessage[]>([]);
const statusFilter = ref<ServiceSessionStatus | "">("");
const sessionLoading = ref(false);
const loadingMessages = ref(false);
const sending = ref(false);
const input = ref("");
const scrollRef = ref<HTMLElement>();

const createVisible = ref(false);
const creating = ref(false);
const farmers = ref<ConsultTarget[]>([]);
const createForm = reactive<{ targetType: ServiceTargetType; targetId?: number }>({
  targetType: 1,
  targetId: undefined,
});

let pollTimer: ReturnType<typeof setInterval> | null = null;
let polling = false;

function isMine(message: ConsultMessage) {
  return String(message.senderId) === String(userStore.userInfo.userId ?? "");
}

function avatarIcon(message: ConsultMessage) {
  if (isMine(message)) return User;
  return message.senderRole === "admin" ? Service : Shop;
}

function roleText(role: string) {
  if (role === "admin") return "客服";
  if (role === "farmer") return "商家";
  return "买家";
}

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
    const data = await ConsultAPI.listSessions({
      pageNum: 1,
      pageSize: 50,
      status: statusFilter.value === "" ? undefined : statusFilter.value,
    });
    sessions.value = data.list ?? [];
    // 同步当前会话状态（可能已被客服结束）
    if (active.value) {
      const latest = sessions.value.find((item) => item.id === active.value!.id);
      if (latest) {
        active.value.status = latest.status;
        active.value.adminName = latest.adminName;
        active.value.endTime = latest.endTime;
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
    const data = await ConsultAPI.listMessages(active.value.id, { pageSize: 100 });
    messages.value = data.list ?? [];
    // 已结束的会话也可能有未读（如客服的结束小结），一样清掉红点
    await ConsultAPI.markRead(active.value.id).catch(() => undefined);
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

function currentLastId() {
  return messages.value.length ? messages.value[messages.value.length - 1].id : 0;
}

/**
 * 增量拉取：只取比本地最后一条更新的消息，避免整页重拉造成闪烁
 */
async function pollMessages() {
  if (!active.value || polling) return;
  polling = true;
  try {
    const lastId = currentLastId();
    const data = await ConsultAPI.listMessages(active.value.id, {
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
        await ConsultAPI.markRead(active.value.id).catch(() => undefined);
        await loadSessions();
      }
    }
  } catch {
    // 轮询失败保持静默，下一轮继续
  } finally {
    polling = false;
  }
}

async function handleSend() {
  const text = input.value.trim();
  if (!text || !active.value || sending.value) return;
  if (active.value.status === 2) {
    ElMessage.warning("会话已结束，无法继续发送");
    return;
  }
  sending.value = true;
  try {
    const message = await ConsultAPI.send(active.value.id, { contentType: 1, content: text });
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

async function openCreate() {
  createVisible.value = true;
  if (farmers.value.length === 0) {
    try {
      farmers.value = await ConsultAPI.listFarmers();
    } catch {
      // 拦截器已提示
    }
  }
}

async function submitCreate() {
  if (createForm.targetType === 2 && !createForm.targetId) {
    ElMessage.warning("请选择要咨询的商家");
    return;
  }
  creating.value = true;
  try {
    const session = await ConsultAPI.createOrGet({
      targetType: createForm.targetType,
      targetId: createForm.targetType === 2 ? createForm.targetId : undefined,
    });
    createVisible.value = false;
    await loadSessions();
    const latest = sessions.value.find((item) => item.id === session.id) ?? session;
    active.value = null;
    await openSession(latest);
    ElMessage.success(
      session.status === 0 ? "已提交咨询，等待平台客服接入" : "已建立会话，可以开始沟通了"
    );
  } catch {
    // 拦截器已提示
  } finally {
    creating.value = false;
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
.consult {
  display: flex;
  gap: 16px;
  height: calc(100vh - 110px);
  min-height: 520px;
}

/* 会话列表 */
.session-panel {
  display: flex;
  flex-direction: column;
  width: 268px;
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
  padding: 0 12px 10px;
  border-bottom: 1px solid var(--el-border-color-lighter);
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

.unread {
  flex-shrink: 0;
}

.session-tags {
  display: flex;
  gap: 6px;
}

/* 会话窗口 */
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
