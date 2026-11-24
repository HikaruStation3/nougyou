<template>
  <div class="dashboard-page">
    <section class="welcome-card">
      <div class="welcome-copy">
        <span class="welcome-eyebrow">平台概览</span>
        <h1>{{ greeting }}，{{ nickname }}</h1>
        <p>{{ todayText }} · 今天有 {{ pendingTotal }} 项审核待处理，订单与商品数据实时统计。</p>
      </div>
      <div class="welcome-actions">
        <el-button :icon="Refresh" :loading="loading" @click="loadAll">刷新数据</el-button>
        <el-button type="primary" :icon="DocumentChecked" @click="router.push('/system/farmer-apply')">
          处理入驻审核
        </el-button>
      </div>
    </section>

    <section v-loading="loading" class="stat-grid">
      <article class="stat-card">
        <div class="stat-head">
          <span>注册用户</span>
          <el-icon><UserFilled /></el-icon>
        </div>
        <strong>{{ stats.users }}</strong>
        <p>买家与农户商家账号</p>
      </article>
      <article class="stat-card">
        <div class="stat-head">
          <span>平台商品</span>
          <el-icon><Goods /></el-icon>
        </div>
        <strong>{{ stats.products }}</strong>
        <p>含在售与已下架农产品</p>
      </article>
      <article class="stat-card">
        <div class="stat-head">
          <span>平台订单</span>
          <el-icon><List /></el-icon>
        </div>
        <strong>{{ stats.orders }}</strong>
        <p>全部订单记录</p>
      </article>
      <article class="stat-card stat-warn">
        <div class="stat-head">
          <span>待审核事项</span>
          <el-icon><Bell /></el-icon>
        </div>
        <strong>{{ pendingTotal }}</strong>
        <p>入驻 {{ stats.applies }} · 农家乐 {{ stats.projects }}</p>
      </article>
    </section>

    <div class="content-grid">
      <el-card shadow="never" class="panel-card">
        <template #header>
          <div class="panel-head">
            <span>最近订单</span>
            <el-button text type="primary" @click="router.push('/system/order')">
              订单查询
            </el-button>
          </div>
        </template>

        <el-empty v-if="!loading && !recentOrders.length" description="暂无订单" :image-size="70" />
        <el-table v-else :data="recentOrders" class="order-table">
          <el-table-column prop="orderNo" label="订单号" min-width="170" />
          <el-table-column label="商家" min-width="130">
            <template #default="{ row }">{{ row.shopName || row.farmerName || "-" }}</template>
          </el-table-column>
          <el-table-column label="金额" width="110" align="right">
            <template #default="{ row }">
              <span class="amount">￥{{ money(row.payAmount) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="ORDER_STATUS[row.orderStatus]?.tag || 'info'" effect="plain">
                {{ ORDER_STATUS[row.orderStatus]?.text || "-" }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="下单时间" min-width="160" />
        </el-table>
      </el-card>

      <div class="side-column">
        <el-card shadow="never" class="panel-card">
          <template #header>
            <div class="panel-head"><span>待办审核</span></div>
          </template>
          <ul class="todo-list">
            <li @click="router.push('/system/farmer-apply')">
              <span class="todo-icon todo-apply"><el-icon><DocumentChecked /></el-icon></span>
              <div class="todo-text">
                <strong>入驻申请</strong>
                <small>贫困户资料与证明材料</small>
              </div>
              <span class="todo-count">{{ stats.applies }}</span>
              <el-icon class="todo-arrow"><ArrowRight /></el-icon>
            </li>
            <li @click="router.push('/system/project')">
              <span class="todo-icon todo-project"><el-icon><House /></el-icon></span>
              <div class="todo-text">
                <strong>农家乐项目</strong>
                <small>审核通过后在前台展示</small>
              </div>
              <span class="todo-count">{{ stats.projects }}</span>
              <el-icon class="todo-arrow"><ArrowRight /></el-icon>
            </li>
            <li @click="router.push('/system/order')">
              <span class="todo-icon todo-order"><el-icon><Van /></el-icon></span>
              <div class="todo-text">
                <strong>订单跟进</strong>
                <small>查看全平台订单状态</small>
              </div>
              <span class="todo-count">{{ stats.orders }}</span>
              <el-icon class="todo-arrow"><ArrowRight /></el-icon>
            </li>
          </ul>
        </el-card>

        <el-card shadow="never" class="panel-card">
          <template #header>
            <div class="panel-head"><span>快捷入口</span></div>
          </template>
          <div class="shortcut-grid">
            <button
              v-for="item in shortcuts"
              :key="item.path"
              type="button"
              class="shortcut"
              @click="router.push(item.path)"
            >
              <el-icon :size="18"><component :is="item.icon" /></el-icon>
              <span>{{ item.title }}</span>
            </button>
          </div>
        </el-card>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: "Dashboard", inheritAttrs: false });

import { computed, onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import {
  ArrowRight,
  Bell,
  DocumentChecked,
  Goods,
  Grid,
  House,
  List,
  Refresh,
  Setting,
  ShoppingCart,
  UserFilled,
  Van,
} from "@element-plus/icons-vue";
import { useUserStore } from "@/stores/user";
import UserAPI from "@/api/system/user";
import ProductAPI from "@/api/product";
import OrderAPI, { type OrderVO } from "@/api/order";
import FarmerApplyAPI from "@/api/farmer";
import ProjectAPI from "@/api/project";

const router = useRouter();
const userStore = useUserStore();

const loading = ref(true);
const stats = reactive({
  users: 0,
  products: 0,
  orders: 0,
  applies: 0,
  projects: 0,
});
const recentOrders = ref<OrderVO[]>([]);

const ORDER_STATUS: Record<
  number,
  { text: string; tag: "primary" | "success" | "info" | "warning" | "danger" }
> = {
  0: { text: "待付款", tag: "warning" },
  1: { text: "待发货", tag: "primary" },
  2: { text: "待收货", tag: "info" },
  3: { text: "已完成", tag: "success" },
  4: { text: "已取消", tag: "info" },
  5: { text: "售后中", tag: "danger" },
};

const shortcuts = [
  { title: "入驻审核", path: "/system/farmer-apply", icon: DocumentChecked },
  { title: "农家乐审核", path: "/system/project", icon: House },
  { title: "农产品管理", path: "/system/product", icon: Goods },
  { title: "订单查询", path: "/system/order", icon: ShoppingCart },
  { title: "委托结算", path: "/system/entrust", icon: List },
  { title: "用户管理", path: "/system/user", icon: UserFilled },
  { title: "角色权限", path: "/system/role", icon: Setting },
  { title: "农产品分类", path: "/system/category", icon: Grid },
];

const nickname = computed(
  () => userStore.userInfo?.nickname || userStore.userInfo?.username || "管理员",
);

const greeting = computed(() => {
  const hour = new Date().getHours();
  if (hour < 6) return "夜深了";
  if (hour < 12) return "上午好";
  if (hour < 14) return "中午好";
  if (hour < 18) return "下午好";
  return "晚上好";
});

const todayText = computed(() => {
  const now = new Date();
  const week = ["日", "一", "二", "三", "四", "五", "六"][now.getDay()];
  return `${now.getFullYear()} 年 ${now.getMonth() + 1} 月 ${now.getDate()} 日 星期${week}`;
});

const pendingTotal = computed(() => stats.applies + stats.projects);

function money(value?: number | string | null) {
  const num = Number(value);
  return Number.isFinite(num) ? num.toFixed(2) : "0.00";
}

async function loadStats() {
  const [users, products, orders, applies, projects] = await Promise.all([
    UserAPI.getPage({ pageNum: 1, pageSize: 1 }).catch(() => null),
    ProductAPI.getAdminPage({ pageNum: 1, pageSize: 1 }).catch(() => null),
    OrderAPI.getAdminPage({ pageNum: 1, pageSize: 1 }).catch(() => null),
    FarmerApplyAPI.getPage({ pageNum: 1, pageSize: 1, auditStatus: 0 }).catch(() => null),
    ProjectAPI.getAdminPage({ pageNum: 1, pageSize: 1, auditStatus: 0 }).catch(() => null),
  ]);
  stats.users = users?.total ?? 0;
  stats.products = products?.total ?? 0;
  stats.orders = orders?.total ?? 0;
  stats.applies = applies?.total ?? 0;
  stats.projects = projects?.total ?? 0;
}

async function loadRecentOrders() {
  const page = await OrderAPI.getAdminPage({ pageNum: 1, pageSize: 6 }).catch(() => null);
  recentOrders.value = page?.list ?? [];
}

async function loadAll() {
  loading.value = true;
  try {
    await Promise.all([loadStats(), loadRecentOrders()]);
  } finally {
    loading.value = false;
  }
}

onMounted(async () => {
  await userStore.getUserInfo().catch(() => {});
  await loadAll();
});
</script>

<style scoped>
.dashboard-page {
  padding: 18px 6px 40px;
}

.welcome-card {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24px;
  padding: 28px 32px;
  background: linear-gradient(120deg, #eef8f1, #f8fbf9 58%, #eef4fb);
  border: 1px solid #e1ece5;
  border-radius: 18px;
}

.welcome-eyebrow {
  color: #2e9e5b;
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 1.5px;
}

.welcome-copy h1 {
  margin: 10px 0 8px;
  color: #1f2d24;
  font-size: 26px;
  letter-spacing: -0.4px;
}

.welcome-copy p {
  margin: 0;
  color: #5d7668;
  font-size: 13px;
}

.welcome-actions {
  display: flex;
  gap: 10px;
  flex-shrink: 0;
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-top: 18px;
}

.stat-card {
  padding: 18px 20px;
  background: #fff;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 14px;
}

.stat-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: #6b7c72;
  font-size: 13px;
}

.stat-card strong {
  display: block;
  margin-top: 14px;
  color: #1f2d24;
  font-size: 30px;
  line-height: 1;
  letter-spacing: -0.5px;
}

.stat-card p {
  margin: 10px 0 0;
  color: #84958a;
  font-size: 12px;
}

.stat-warn {
  background: #fffaf0;
  border-color: #f0d5a8;
}

.stat-warn .stat-head,
.stat-warn strong {
  color: #b07818;
}

.content-grid {
  display: grid;
  grid-template-columns: 1.55fr 1fr;
  gap: 16px;
  margin-top: 18px;
}

.panel-card {
  border-radius: 14px;
}

.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 15px;
  font-weight: 600;
}

.side-column {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.todo-list {
  margin: 0;
  padding: 0;
  list-style: none;
}

.todo-list li {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 4px;
  border-bottom: 1px dashed var(--el-border-color-lighter);
  cursor: pointer;
  transition: background-color 0.2s ease;
}

.todo-list li:last-child {
  border-bottom: none;
}

.todo-list li:hover {
  background: var(--el-fill-color-lighter);
}

.todo-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  border-radius: 10px;
}

.todo-apply {
  color: #2e9e5b;
  background: #eef8f1;
}

.todo-project {
  color: #5b8ff9;
  background: #eef3ff;
}

.todo-order {
  color: #c07a12;
  background: #fff6e8;
}

.todo-text {
  flex: 1;
  min-width: 0;
}

.todo-text strong {
  display: block;
  color: var(--el-text-color-primary);
  font-size: 13px;
}

.todo-text small {
  display: block;
  margin-top: 3px;
  color: var(--el-text-color-placeholder);
  font-size: 12px;
}

.todo-count {
  color: var(--el-text-color-primary);
  font-size: 18px;
  font-weight: 700;
}

.todo-arrow {
  color: var(--el-text-color-placeholder);
}

.shortcut-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
}

.shortcut {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  color: var(--el-text-color-regular);
  font-size: 13px;
  text-align: left;
  background: var(--el-fill-color-lighter);
  border: none;
  border-radius: 10px;
  cursor: pointer;
  transition: color 0.2s ease, background-color 0.2s ease;
}

.shortcut:hover {
  color: #2e9e5b;
  background: #eef8f1;
}

.order-table {
  margin-top: 4px;
}

.amount {
  color: #d94a3d;
  font-weight: 600;
}

@media (max-width: 1100px) {
  .stat-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .content-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .welcome-card {
    flex-direction: column;
    align-items: flex-start;
  }

  .stat-grid {
    grid-template-columns: 1fr;
  }
}
</style>
