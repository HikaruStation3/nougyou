<template>
  <div class="consult-page">
    <div class="page-head">
      <div>
        <h1>我的咨询</h1>
        <p>有问题随时问 —— 平台客服或商家都会在这里回复你</p>
      </div>
      <el-button type="primary" :icon="Plus" @click="openCreate">发起咨询</el-button>
    </div>

    <div v-if="!store.isLoggedIn()" class="guest-box">
      <el-empty description="登录后即可与客服、商家沟通">
        <el-button type="primary" @click="goLogin">去登录</el-button>
      </el-empty>
    </div>

    <div v-else class="consult-body">
      <!-- 会话列表 -->
      <aside class="session-panel">
        <div class="panel-filter">
          <el-radio-group v-model="statusFilter" size="small" @change="loadSessions">
            <el-radio-button :value="''">全部</el-radio-button>
            <el-radio-button :value="0">待接入</el-radio-button>
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
            <div class="session-top">
              <span class="session-title">
                <el-icon v-if="item.targetType === 1" class="title-icon"><Service /></el-icon>
                <el-icon v-else class="title-icon"><Shop /></el-icon>
                {{ item.title }}
              </span>
              <span class="session-time">{{ formatRelative(item.lastMsgTime) }}</span>
            </div>
            <div class="session-bottom">
              <span class="session-preview">{{ item.lastMsgPreview || "暂无消息" }}</span>
              <el-badge v-if="item.unreadCount > 0" :value="item.unreadCount" :max="99" />
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
            description="还没有咨询记录"
            :image-size="56"
          />
        </el-scrollbar>
      </aside>

      <!-- 聊天窗口 -->
      <section class="chat-panel">
        <template v-if="active">
          <header class="chat-header">
            <div class="peer">
              <el-avatar :size="38" class="peer-avatar">
                <el-icon><component :is="active.targetType === 1 ? Service : Shop" /></el-icon>
              </el-avatar>
              <div>
                <div class="peer-name">
                  {{ active.title }}
                  <el-tag size="small" effect="plain" :type="statusTag(active.status)">
                    {{ statusText(active.status) }}
                  </el-tag>
                </div>
                <div class="peer-desc">
                  会话号 {{ active.sessionNo }}
                  <template v-if="active.adminName"> · 客服 {{ active.adminName }}</template>
                  <template v-if="active.productName"> · 咨询商品 {{ active.productName }}</template>
                </div>
              </div>
            </div>
            <el-button size="small" :icon="Refresh" circle @click="reloadMessages" />
          </header>

          <div v-if="active.status === 0" class="notice">
            已提交咨询，正在等待平台客服接入，请稍候…
          </div>
          <div v-else-if="active.status === 2" class="notice closed">
            会话已于 {{ formatTime(active.endTime) || "-" }} 结束，如需继续咨询请重新发起。
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
              <el-avatar :size="32" :src="item.senderAvatar || undefined" class="msg-avatar">
                {{ (item.senderName || "?").slice(0, 1) }}
              </el-avatar>
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
              :placeholder="
                active.status === 2 ? '会话已结束，无法继续发送' : '输入消息，Enter 发送，Shift + Enter 换行'
              "
              @keydown.enter.exact.prevent="handleSend"
            />
            <div class="input-actions">
              <span class="input-hint">
                {{
                  active.status === 2
                    ? "会话已结束，如需继续咨询请重新发起"
                    : active.targetType === 1
                      ? "平台客服会尽快回复你"
                      : "直接与商家沟通，请注意保护个人信息"
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
          <el-empty description="选择左侧会话，或发起一次新的咨询" />
        </div>
      </section>
    </div>

    <!-- 发起咨询 -->
    <el-dialog v-model="createVisible" title="发起咨询" width="440px">
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
            :loading="farmerLoading"
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

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { Plus, Refresh, Service, Shop } from "@element-plus/icons-vue";
import { useUserStore } from "../store/user";
import {
  createSession,
  listFarmers,
  listMessages,
  listSessions,
  markRead,
  sendMessage
} from "../api/consult";

const route = useRoute();
const router = useRouter();
const store = useUserStore();

const sessions = ref([]);
const active = ref(null);
const activeId = computed(() => active.value?.id ?? 0);
const messages = ref([]);
const statusFilter = ref("");
const sessionLoading = ref(false);
const loadingMessages = ref(false);
const sending = ref(false);
const input = ref("");
const scrollRef = ref(null);

const createVisible = ref(false);
const creating = ref(false);
const farmerLoading = ref(false);
const farmers = ref([]);
const createForm = reactive({ targetType: 1, targetId: undefined });

let pollTimer = null;
let polling = false;

function currentUserId() {
  return store.state.user?.userId ?? null;
}

function isMine(message) {
  const uid = currentUserId();
  if (uid === null || uid === undefined) return false;
  return String(message.senderId) === String(uid);
}

function roleText(role) {
  if (role === "admin") return "客服";
  if (role === "farmer") return "商家";
  return "买家";
}

function statusText(status) {
  if (status === 0) return "待接入";
  if (status === 1) return "进行中";
  return "已结束";
}

function statusTag(status) {
  if (status === 0) return "warning";
  if (status === 1) return "success";
  return "info";
}

function isImageUrl(content) {
  return content.startsWith("/") || content.startsWith("http");
}

function formatTime(value) {
  if (!value) return "";
  return String(value).replace("T", " ").slice(5, 16);
}

function formatRelative(value) {
  if (!value) return "";
  const date = new Date(String(value).replace("T", " ").replace(/-/g, "/"));
  if (Number.isNaN(date.getTime())) return "";
  const diff = Date.now() - date.getTime();
  const minute = 60 * 1000;
  if (diff < minute) return "刚刚";
  if (diff < 60 * minute) return `${Math.floor(diff / minute)} 分钟前`;
  const pad = (n) => `${n}`.padStart(2, "0");
  return `${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(
    date.getMinutes()
  )}`;
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
    const data = await listSessions({
      pageNum: 1,
      pageSize: 50,
      status: statusFilter.value === "" ? undefined : statusFilter.value
    });
    sessions.value = data?.list ?? [];
    // 同步当前会话状态（客服可能已结束）
    if (active.value) {
      const latest = sessions.value.find((item) => item.id === active.value.id);
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
    const data = await listMessages(active.value.id, { pageSize: 100 });
    messages.value = data?.list ?? [];
    await markRead(active.value.id).catch(() => undefined);
    await loadSessions();
    scrollToBottom();
  } catch {
    // 拦截器已提示
  } finally {
    loadingMessages.value = false;
  }
}

async function openSession(session) {
  if (active.value?.id === session.id) return;
  active.value = session;
  messages.value = [];
  await reloadMessages();
}

function currentLastId() {
  return messages.value.length ? messages.value[messages.value.length - 1].id : 0;
}

async function pollMessages() {
  if (!active.value || polling || active.value.status === 2) return;
  polling = true;
  try {
    const lastId = currentLastId();
    const data = await listMessages(active.value.id, {
      lastId: lastId || undefined,
      pageSize: 50
    });
    const incoming = data?.list ?? [];
    if (incoming.length > 0) {
      const exists = new Set(messages.value.map((item) => item.id));
      const fresh = incoming.filter((item) => !exists.has(item.id));
      if (fresh.length > 0) {
        const nearBottom = isNearBottom();
        messages.value.push(...fresh);
        if (nearBottom) scrollToBottom();
        await markRead(active.value.id).catch(() => undefined);
        await loadSessions();
      }
    }
  } catch {
    // 轮询失败保持静默
  } finally {
    polling = false;
  }
}

function isNearBottom() {
  const el = scrollRef.value;
  if (!el) return true;
  return el.scrollHeight - el.scrollTop - el.clientHeight < 120;
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
    const message = await sendMessage(active.value.id, text);
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

async function loadFarmers() {
  if (farmers.value.length > 0) return;
  farmerLoading.value = true;
  try {
    farmers.value = (await listFarmers()) ?? [];
  } catch {
    // 拦截器已提示
  } finally {
    farmerLoading.value = false;
  }
}

function openCreate() {
  if (!store.isLoggedIn()) {
    goLogin();
    return;
  }
  createVisible.value = true;
  void loadFarmers();
}

async function submitCreate() {
  if (createForm.targetType === 2 && !createForm.targetId) {
    ElMessage.warning("请选择要咨询的商家");
    return;
  }
  creating.value = true;
  try {
    const session = await createSession({
      targetType: createForm.targetType,
      targetId: createForm.targetType === 2 ? createForm.targetId : undefined
    });
    createVisible.value = false;
    await loadSessions();
    const latest = sessions.value.find((item) => item.id === session.id) || session;
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

function goLogin() {
  router.push({ name: "login", query: { redirect: "/consult" } });
}

/** 支持从商品/项目页带参跳转：/consult?targetType=2&targetId=3 */
async function handleQueryEntry() {
  const { targetType, targetId, productId } = route.query;
  if (!targetType) return;
  try {
    const session = await createSession({
      targetType: Number(targetType),
      targetId: targetId ? Number(targetId) : undefined,
      productId: productId ? Number(productId) : undefined
    });
    await loadSessions();
    const latest = sessions.value.find((item) => item.id === session.id) || session;
    active.value = null;
    await openSession(latest);
  } catch {
    // 拦截器已提示
  } finally {
    router.replace({ path: "/consult" });
  }
}

onMounted(async () => {
  if (!store.isLoggedIn()) return;
  try {
    if (!store.state.user) await store.loadUser();
  } catch {
    return;
  }
  await loadSessions();
  if (sessions.value.length > 0) {
    await openSession(sessions.value[0]);
  }
  await handleQueryEntry();
  pollTimer = setInterval(() => {
    void pollMessages();
  }, 3000);
});

onBeforeUnmount(() => {
  if (pollTimer) clearInterval(pollTimer);
});
</script>

<style scoped>
.consult-page {
  max-width: 1120px;
  margin: 0 auto;
  padding: 36px 24px 56px;
}

.page-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
}

.page-head h1 {
  margin: 0 0 8px;
  font-size: 26px;
  letter-spacing: -0.3px;
}

.page-head p {
  margin: 0;
  color: var(--portal-muted);
  font-size: 14px;
}

.guest-box {
  padding: 60px 0;
  background: #fff;
  border: 1px solid var(--portal-border);
  border-radius: 20px;
}

.consult-body {
  display: flex;
  gap: 16px;
  height: calc(100vh - 260px);
  min-height: 520px;
}

/* 会话列表 */
.session-panel {
  display: flex;
  flex-direction: column;
  width: 280px;
  flex-shrink: 0;
  overflow: hidden;
  background: #fff;
  border: 1px solid var(--portal-border);
  border-radius: 18px;
}

.panel-filter {
  padding: 12px;
  border-bottom: 1px solid var(--portal-border);
}

.session-scroll {
  flex: 1;
  padding: 8px;
}

.session-item {
  padding: 11px 12px;
  margin-bottom: 6px;
  cursor: pointer;
  border-radius: 12px;
  transition: background-color 0.2s;
}

.session-item:hover {
  background: #f4f8f5;
}

.session-item.active {
  background: var(--el-color-primary-light-9);
}

.session-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 5px;
}

.session-title {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  overflow: hidden;
  font-size: 13.5px;
  font-weight: 600;
  color: var(--portal-text);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.title-icon {
  color: var(--el-color-primary);
}

.session-time {
  flex-shrink: 0;
  font-size: 12px;
  color: var(--portal-muted);
}

.session-bottom {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 6px;
}

.session-preview {
  overflow: hidden;
  flex: 1;
  font-size: 12px;
  color: var(--portal-muted);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.session-tags {
  display: flex;
  gap: 6px;
}

/* 聊天窗口 */
.chat-panel {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-width: 0;
  overflow: hidden;
  background: #fff;
  border: 1px solid var(--portal-border);
  border-radius: 18px;
}

.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 18px;
  border-bottom: 1px solid var(--portal-border);
}

.peer {
  display: flex;
  align-items: center;
  gap: 10px;
}

.peer-avatar {
  color: #fff;
  background: linear-gradient(135deg, #63b985, #2e9e5b);
}

.peer-name {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14.5px;
  font-weight: 600;
}

.peer-desc {
  margin-top: 3px;
  font-size: 12px;
  color: var(--portal-muted);
}

.notice {
  padding: 8px 18px;
  font-size: 12px;
  color: var(--el-color-warning-dark-2);
  background: var(--el-color-warning-light-9);
}

.notice.closed {
  color: var(--portal-muted);
  background: #f5f7f6;
}

.chat-body {
  flex: 1;
  padding: 18px;
  overflow-y: auto;
  background: #fbfcfb;
}

.empty-chat {
  padding-top: 80px;
  color: var(--portal-muted);
  font-size: 13px;
  text-align: center;
}

.msg {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;
}

.msg.mine {
  flex-direction: row-reverse;
}

.msg-avatar {
  flex-shrink: 0;
  color: #fff;
  background: linear-gradient(135deg, #9fb8a9, #7d9c89);
}

.msg.mine .msg-avatar {
  background: linear-gradient(135deg, #63b985, #2e9e5b);
}

.msg-main {
  display: flex;
  flex-direction: column;
  max-width: 62%;
}

.msg.mine .msg-main {
  align-items: flex-end;
}

.msg-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 4px;
  font-size: 12px;
  color: var(--portal-muted);
}

.msg.mine .msg-meta {
  flex-direction: row-reverse;
}

.role-tag {
  padding: 0 4px;
  border: 1px solid var(--portal-border);
  border-radius: 3px;
}

.bubble {
  padding: 9px 12px;
  font-size: 13.5px;
  line-height: 1.65;
  color: var(--portal-text);
  word-break: break-word;
  white-space: pre-wrap;
  background: #f2f5f3;
  border-radius: 10px;
  border-top-left-radius: 3px;
}

.msg.mine .bubble {
  color: #fff;
  background: var(--el-color-primary);
  border-top-left-radius: 10px;
  border-top-right-radius: 3px;
}

.bubble-image {
  max-width: 220px;
  max-height: 220px;
  border-radius: 6px;
}

.chat-input {
  padding: 12px 18px 14px;
  border-top: 1px solid var(--portal-border);
}

.input-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 10px;
}

.input-hint {
  font-size: 12px;
  color: var(--portal-muted);
}

.chat-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
}

@media (max-width: 900px) {
  .consult-body {
    flex-direction: column;
    height: auto;
  }

  .session-panel {
    width: 100%;
    max-height: 260px;
  }

  .chat-panel {
    min-height: 480px;
  }
}
</style>
