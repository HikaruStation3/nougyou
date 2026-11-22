<template>
  <div class="app-container ai-monitor">
    <!-- 概览 -->
    <el-row :gutter="12" class="metric-row">
      <el-col v-for="metric in metrics" :key="metric.label" :xs="12" :sm="8" :md="4">
        <div class="metric-card" :class="{ 'metric-card--warn': metric.warn }">
          <div class="metric-label">{{ metric.label }}</div>
          <div class="metric-value">{{ metric.value }}</div>
        </div>
      </el-col>
    </el-row>

    <!-- 筛选 -->
    <div class="filter-section">
      <el-form :inline="true" label-width="auto" @submit.prevent>
        <el-form-item label="会话标题">
          <el-input
            v-model="query.keyword"
            placeholder="按标题关键字搜索"
            clearable
            style="width: 200px"
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="业务场景">
          <el-select v-model="query.sceneCode" placeholder="全部场景" clearable style="width: 160px">
            <el-option v-for="opt in SCENE_OPTIONS" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="用户 ID">
          <el-input v-model="query.userId" placeholder="全部用户" clearable style="width: 120px" />
        </el-form-item>
        <el-form-item label="创建时间">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            unlink-panels
            range-separator="~"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 250px"
          />
        </el-form-item>
        <el-form-item class="search-buttons">
          <el-button type="primary" icon="search" :loading="loading" @click="handleQuery">搜索</el-button>
          <el-button icon="refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 列表 -->
    <el-card shadow="never" class="table-section">
      <div class="table-section__toolbar">
        <div class="table-section__toolbar--actions">
          <span class="table-title">全平台 AI 会话</span>
          <span class="table-sub">共 {{ total }} 条，点击「查看全文」核对问答内容</span>
        </div>
      </div>

      <el-table v-loading="loading" :data="sessions" border stripe class="table-section__content">
        <el-table-column label="ID" prop="id" width="80" align="center" />
        <el-table-column label="用户" min-width="140">
          <template #default="scope">
            <span>{{ scope.row.userName || `用户#${scope.row.userId}` }}</span>
            <span v-if="scope.row.userName" class="user-id">#{{ scope.row.userId }}</span>
          </template>
        </el-table-column>
        <el-table-column label="场景" width="120" align="center">
          <template #default="scope">
            <el-tag size="small" effect="plain">{{ scope.row.sceneName }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="会话标题" prop="title" min-width="220" show-overflow-tooltip />
        <el-table-column label="消息数" prop="msgCount" width="90" align="center" />
        <el-table-column label="模型" prop="model" width="120" align="center" />
        <el-table-column label="创建时间" prop="createTime" width="170" align="center" />
        <el-table-column label="最后活跃" prop="updateTime" width="170" align="center" />
        <el-table-column label="操作" width="110" align="center" fixed="right">
          <template #default="scope">
            <el-button
              v-hasPerm="'ai:adminMessage'"
              type="primary"
              link
              icon="View"
              @click="openSession(scope.row)"
            >
              查看全文
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="query.pageNum"
        v-model:page-size="query.pageSize"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        class="table-section__pagination"
        @change="loadSessions"
      />
    </el-card>

    <!-- 全文抽屉 -->
    <el-drawer v-model="drawerVisible" size="620px" :title="drawerTitle" destroy-on-close>
      <div v-loading="messageLoading" class="message-list">
        <el-empty v-if="!messageLoading && messages.length === 0" description="暂无消息" :image-size="64" />
        <div
          v-for="item in messages"
          :key="item.id"
          :class="['message-item', item.role, { flagged: item.flagged }]"
        >
          <div class="message-meta">
            <el-tag size="small" :type="item.role === 'user' ? 'info' : 'primary'" effect="plain">
              {{ item.role === "user" ? "用户提问" : "AI 回答" }}
            </el-tag>
            <el-tag v-if="item.flagged" size="small" type="danger" effect="dark">命中管控</el-tag>
            <span class="message-time">{{ item.createTime }}</span>
          </div>
          <div class="message-content">{{ item.content }}</div>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import AIAPI, { type AiOverview, type ChatMessage, type ChatSession } from "@/api/ai";

const SCENE_OPTIONS = [
  { label: "经营咨询", value: "BIZ_CONSULT" },
  { label: "数据统计", value: "SALE_STAT" },
  { label: "可行性分析", value: "SALE_ANALYSIS" },
] as const;

const loading = ref(false);
const sessions = ref<ChatSession[]>([]);
const total = ref(0);
const overview = ref<AiOverview>({
  sessionCount: 0,
  messageCount: 0,
  flaggedCount: 0,
  todaySessions: 0,
  activeUsers: 0,
});
const dateRange = ref<[string, string] | null>(null);
const query = ref<{
  pageNum: number;
  pageSize: number;
  keyword: string;
  sceneCode: string;
  userId: string;
}>({
  pageNum: 1,
  pageSize: 10,
  keyword: "",
  sceneCode: "",
  userId: "",
});

const drawerVisible = ref(false);
const messageLoading = ref(false);
const messages = ref<ChatMessage[]>([]);
const activeSession = ref<ChatSession | null>(null);

const metrics = computed(() => [
  { label: "会话总数", value: overview.value.sessionCount },
  { label: "消息总数", value: overview.value.messageCount },
  { label: "命中管控", value: overview.value.flaggedCount, warn: overview.value.flaggedCount > 0 },
  { label: "今日新增会话", value: overview.value.todaySessions },
  { label: "活跃用户", value: overview.value.activeUsers },
]);

const drawerTitle = computed(() =>
  activeSession.value
    ? `${activeSession.value.title || "会话"} · ${activeSession.value.sceneName}`
    : "会话全文"
);

async function loadOverview() {
  try {
    overview.value = await AIAPI.adminOverview();
  } catch {
    // 拦截器已提示
  }
}

async function loadSessions() {
  loading.value = true;
  try {
    const [start, end] = dateRange.value ?? [];
    const data = await AIAPI.adminSessions({
      pageNum: query.value.pageNum,
      pageSize: query.value.pageSize,
      keyword: query.value.keyword || undefined,
      sceneCode: (query.value.sceneCode || undefined) as any,
      userId: query.value.userId ? Number(query.value.userId) : undefined,
      startTime: start,
      endTime: end,
    });
    sessions.value = data.list ?? [];
    total.value = data.total ?? 0;
  } catch {
    // 拦截器已提示
  } finally {
    loading.value = false;
  }
}

function handleQuery() {
  query.value.pageNum = 1;
  loadSessions();
}

function handleReset() {
  query.value = { pageNum: 1, pageSize: 10, keyword: "", sceneCode: "", userId: "" };
  dateRange.value = null;
  loadSessions();
}

async function openSession(row: any) {
  activeSession.value = row as ChatSession;
  drawerVisible.value = true;
  messageLoading.value = true;
  messages.value = [];
  try {
    const data = await AIAPI.adminMessages(row.id, { pageNum: 1, pageSize: 200 });
    messages.value = data.list ?? [];
  } catch {
    // 拦截器已提示
  } finally {
    messageLoading.value = false;
  }
}

onMounted(() => {
  loadOverview();
  loadSessions();
});
</script>

<style lang="scss" scoped>
.ai-monitor {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.metric-row {
  .metric-card {
    padding: 14px 16px;
    border-radius: 8px;
    background: var(--el-bg-color);
    border: 1px solid var(--el-border-color-light);
    height: 100%;

    &--warn {
      border-color: var(--el-color-danger-light-5);
      background: var(--el-color-danger-light-9);
    }
  }

  .metric-label {
    font-size: 13px;
    color: var(--el-text-color-secondary);
  }

  .metric-value {
    margin-top: 6px;
    font-size: 22px;
    font-weight: 600;
  }
}

.table-title {
  font-weight: 600;
  margin-right: 10px;
}

.table-sub {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.user-id {
  margin-left: 6px;
  font-size: 12px;
  color: var(--el-text-color-placeholder);
}

.message-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-height: 200px;
}

.message-item {
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  padding: 10px 12px;

  &.user {
    background: var(--el-fill-color-lighter);
  }

  &.assistant {
    background: var(--el-bg-color);
  }

  &.flagged {
    border-color: var(--el-color-danger-light-5);
    background: var(--el-color-danger-light-9);
  }
}

.message-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.message-time {
  font-size: 12px;
  color: var(--el-text-color-placeholder);
}

.message-content {
  white-space: pre-wrap;
  line-height: 1.7;
  font-size: 14px;
  color: var(--el-text-color-primary);
  word-break: break-word;
}
</style>
