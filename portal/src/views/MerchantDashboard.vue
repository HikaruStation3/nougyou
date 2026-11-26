<template>
  <div class="workbench-page">
    <section class="workbench-hero">
      <div class="hero-copy">
        <div class="hero-tags">
          <span class="hero-eyebrow">商家工作台</span>
          <span v-if="shopName" class="hero-shop">店铺 · {{ shopName }}</span>
        </div>
        <h1>{{ greeting }}，{{ nickname }}</h1>
        <p>今日成交、待办事项与库存预警都汇总在这里，数据范围仅限本店。</p>
      </div>
      <div class="hero-actions">
        <el-button :icon="Refresh" :loading="loading" @click="loadDashboard">刷新数据</el-button>
        <el-button type="primary" :icon="Plus" @click="router.push('/merchant/products')">
          管理商品
        </el-button>
      </div>
    </section>

    <section v-loading="loading" class="kpi-grid">
      <article class="kpi-card kpi-green">
        <div class="kpi-head">
          <span>今日成交额</span>
          <el-icon><Money /></el-icon>
        </div>
        <strong class="kpi-value"><small>￥</small>{{ money(data.todayAmount) }}</strong>
        <p class="kpi-foot">本月累计 ￥{{ money(data.monthAmount) }}</p>
      </article>

      <article class="kpi-card">
        <div class="kpi-head">
          <span>待发货订单</span>
          <el-icon><Van /></el-icon>
        </div>
        <strong class="kpi-value">{{ data.orderWaitShip }}<small> 单</small></strong>
        <p class="kpi-foot">待收货 {{ data.orderWaitReceive }} 单 · 已完成 {{ data.orderFinished }} 单</p>
      </article>

      <article class="kpi-card" :class="{ 'kpi-warn': data.stockWarnCount > 0 }">
        <div class="kpi-head">
          <span>库存预警</span>
          <el-icon><Warning /></el-icon>
        </div>
        <strong class="kpi-value">{{ data.stockWarnCount }}<small> 款</small></strong>
        <p class="kpi-foot">共 {{ data.productTotal }} 款商品，在售 {{ data.onSaleCount }} 款</p>
      </article>

      <article class="kpi-card clickable" @click="router.push('/merchant/after-sales')">
        <div class="kpi-head">
          <span>待处理售后</span>
          <el-icon><Service /></el-icon>
        </div>
        <strong class="kpi-value">{{ data.afterSalePending }}<small> 单</small></strong>
        <p class="kpi-foot">及时处理可减少纠纷与退款 · 点击查看</p>
      </article>
    </section>

    <div class="panel-grid">
      <el-card shadow="never" class="panel-card">
        <template #header>
          <div class="panel-head">
            <span>商品概况</span>
            <el-button text type="primary" @click="router.push('/merchant/products')">
              商品管理
            </el-button>
          </div>
        </template>
        <div class="panel-metrics">
          <div class="panel-metric">
            <strong>{{ data.productTotal }}</strong>
            <span>商品总数</span>
          </div>
          <div class="panel-metric">
            <strong>{{ data.onSaleCount }}</strong>
            <span>在售</span>
          </div>
          <div class="panel-metric">
            <strong>{{ data.offSaleCount }}</strong>
            <span>已下架</span>
          </div>
        </div>
        <div class="stock-bar">
          <div class="stock-bar-head">
            <span>在售占比</span>
            <span>{{ onSaleRate }}%</span>
          </div>
          <el-progress
            :percentage="onSaleRate"
            :stroke-width="8"
            :show-text="false"
            color="#2e9e5b"
          />
        </div>
      </el-card>

      <el-card shadow="never" class="panel-card">
        <template #header>
          <div class="panel-head">
            <span>订单概况</span>
            <el-button text type="primary" @click="router.push('/merchant/orders')">
              商家订单
            </el-button>
          </div>
        </template>
        <ul class="order-list">
          <li>
            <span class="order-dot dot-wait"></span>
            <span class="order-label">待发货</span>
            <strong>{{ data.orderWaitShip }}</strong>
            <el-button v-if="data.orderWaitShip > 0" text type="primary" @click="router.push('/merchant/orders')">
              去发货
            </el-button>
          </li>
          <li>
            <span class="order-dot dot-ship"></span>
            <span class="order-label">待收货</span>
            <strong>{{ data.orderWaitReceive }}</strong>
            <span class="order-hint">等待买家确认</span>
          </li>
          <li>
            <span class="order-dot dot-done"></span>
            <span class="order-label">已完成</span>
            <strong>{{ data.orderFinished }}</strong>
            <span class="order-hint">累计完成订单</span>
          </li>
        </ul>
      </el-card>

      <el-card shadow="never" class="panel-card">
        <template #header>
          <div class="panel-head">
            <span>内容与委托</span>
          </div>
        </template>
        <ul class="order-list">
          <li>
            <span class="order-dot dot-wait"></span>
            <span class="order-label">视频审核中</span>
            <strong>{{ data.videoAuditPending }}</strong>
            <span class="order-hint">等待管理员审核</span>
          </li>
          <li>
            <span class="order-dot dot-reject"></span>
            <span class="order-label">视频被驳回</span>
            <strong>{{ data.videoRejected }}</strong>
            <span class="order-hint">修改后可重新提交</span>
          </li>
          <li>
            <span class="order-dot dot-done"></span>
            <span class="order-label">委托关系中</span>
            <strong>{{ data.entrustCount }}</strong>
            <el-button text type="primary" @click="router.push('/merchant/entrusts')">
              查看
            </el-button>
          </li>
        </ul>
      </el-card>
    </div>

    <el-card shadow="never" class="panel-card stock-card">
      <template #header>
        <div class="panel-head">
          <span>库存预警</span>
          <span class="panel-sub">库存低于 10 件的商品，优先补货或下架</span>
        </div>
      </template>

      <el-empty
        v-if="!loading && !data.stockWarnList.length"
        description="暂无库存预警，商品库存充足"
        :image-size="80"
      />
      <el-table v-else :data="data.stockWarnList" class="stock-table">
        <el-table-column prop="name" label="商品名称" min-width="220" />
        <el-table-column label="剩余库存" width="120" align="center">
          <template #default="{ row }">
            <span class="stock-num" :class="{ 'stock-low': row.stock < 10 }">{{ row.stock }}</span>
          </template>
        </el-table-column>
        <el-table-column label="上架状态" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="row.shelfStatus === 1 ? 'success' : 'info'" effect="plain">
              {{ row.shelfStatus === 1 ? "在售" : "已下架" }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" align="center">
          <template #default>
            <el-button text type="primary" @click="router.push('/merchant/products')">
              去补货
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <section class="shortcut-grid">
      <button
        v-for="item in shortcuts"
        :key="item.title"
        type="button"
        class="shortcut-card"
        @click="router.push(item.path)"
      >
        <el-icon :size="20"><component :is="item.icon" /></el-icon>
        <div>
          <strong>{{ item.title }}</strong>
          <small>{{ item.desc }}</small>
        </div>
        <el-icon class="shortcut-arrow"><ArrowRight /></el-icon>
      </button>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import {
  ArrowRight,
  Box,
  List,
  Money,
  Plus,
  Refresh,
  Service,
  ShoppingBag,
  TrendCharts,
  Van,
  Warning,
} from "@element-plus/icons-vue";
import { getFarmerDashboard } from "../api/home";
import { useUserStore } from "../store/user";
import { money } from "../utils/format";

const router = useRouter();
const store = useUserStore();

const loading = ref(true);
const data = reactive({
  productTotal: 0,
  onSaleCount: 0,
  offSaleCount: 0,
  stockWarnCount: 0,
  orderWaitShip: 0,
  orderWaitReceive: 0,
  orderFinished: 0,
  todayAmount: 0,
  monthAmount: 0,
  afterSalePending: 0,
  videoAuditPending: 0,
  videoRejected: 0,
  entrustCount: 0,
  stockWarnList: [],
});

const nickname = computed(
  () => store.state.user?.nickname || store.state.user?.username || "商家",
);

/** 店铺名（部门名），商家在页面上需要明确自己当前是哪家店 */
const shopName = computed(() => store.state.user?.shopName || "");

const greeting = computed(() => {
  const hour = new Date().getHours();
  if (hour < 6) return "夜深了";
  if (hour < 12) return "早上好";
  if (hour < 14) return "中午好";
  if (hour < 18) return "下午好";
  return "晚上好";
});

const onSaleRate = computed(() => {
  if (!data.productTotal) return 0;
  return Math.round((data.onSaleCount / data.productTotal) * 100);
});

const shortcuts = [
  { title: "上架商品", desc: "新增或编辑农产品", path: "/merchant/products", icon: Box },
  { title: "商家订单", desc: "发货与订单查询", path: "/merchant/orders", icon: List },
  { title: "委托销售", desc: "代销关系与结算", path: "/merchant/entrusts", icon: TrendCharts },
  { title: "我的农家乐", desc: "项目申请与上下架", path: "/merchant/projects", icon: ShoppingBag },
];

async function loadDashboard() {
  loading.value = true;
  try {
    const result = await getFarmerDashboard();
    Object.assign(data, result || {});
    if (!Array.isArray(data.stockWarnList)) {
      data.stockWarnList = [];
    }
  } finally {
    loading.value = false;
  }
}

onMounted(async () => {
  // 工作台展示店铺名，进页面时刷新一次用户信息，避免旧缓存
  if (store.isLoggedIn()) {
    await store.loadUser().catch(() => {});
  }
  await loadDashboard();
});
</script>

<style scoped>
.workbench-page {
  max-width: 1160px;
  margin: 0 auto;
  padding: 36px 24px 80px;
}

.workbench-hero {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24px;
  padding: 32px 36px;
  background: linear-gradient(120deg, #eef8f1, #f8fbf9 60%, #eef4fb);
  border: 1px solid #e1ece5;
  border-radius: 22px;
}

.hero-eyebrow {
  color: #2e9e5b;
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 1.5px;
}

.hero-tags {
  display: flex;
  align-items: center;
  gap: 10px;
}

.hero-shop {
  padding: 2px 10px;
  color: #1f7a47;
  font-size: 12px;
  background: #e6f4ec;
  border: 1px solid #c9e5d5;
  border-radius: 999px;
}

.hero-copy h1 {
  margin: 12px 0 10px;
  color: #1f2d24;
  font-size: 30px;
  letter-spacing: -0.5px;
}

.hero-copy p {
  margin: 0;
  color: #6b7c72;
  font-size: 14px;
}

.hero-actions {
  display: flex;
  gap: 10px;
  flex-shrink: 0;
}

.kpi-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-top: 20px;
  min-height: 120px;
}

.kpi-card {
  padding: 20px 22px;
  background: #fff;
  border: 1px solid #e3ebe6;
  border-radius: 16px;
}

.kpi-card.kpi-green {
  background: linear-gradient(135deg, #2e9e5b, #1f7a47);
  border-color: transparent;
}

.kpi-card.kpi-warn {
  border-color: #f0c78a;
  background: #fffaf0;
}

.kpi-card.clickable {
  cursor: pointer;
  transition: border-color 0.2s, box-shadow 0.2s;
}

.kpi-card.clickable:hover {
  border-color: #8ccfa8;
  box-shadow: 0 6px 18px rgba(46, 158, 91, 0.12);
}

.kpi-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: #7b8c81;
  font-size: 13px;
}

.kpi-green .kpi-head {
  color: rgba(255, 255, 255, 0.78);
}

.kpi-warn .kpi-head {
  color: #b07818;
}

.kpi-value {
  display: block;
  margin-top: 16px;
  color: #1f2d24;
  font-size: 32px;
  line-height: 1;
  letter-spacing: -0.5px;
}

.kpi-value small {
  font-size: 14px;
  font-weight: 500;
}

.kpi-green .kpi-value {
  color: #fff;
}

.kpi-foot {
  margin: 12px 0 0;
  color: #9aa9a0;
  font-size: 12px;
}

.kpi-green .kpi-foot {
  color: rgba(255, 255, 255, 0.7);
}

.panel-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin-top: 20px;
}

.panel-card {
  border-radius: 16px;
}

.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  color: #1f2d24;
  font-size: 15px;
  font-weight: 600;
}

.panel-sub {
  color: #9aa9a0;
  font-size: 12px;
  font-weight: 400;
}

.panel-metrics {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
}

.panel-metric {
  text-align: center;
}

.panel-metric strong {
  display: block;
  color: #1f2d24;
  font-size: 24px;
}

.panel-metric span {
  color: #84958a;
  font-size: 12px;
}

.stock-bar {
  margin-top: 18px;
}

.stock-bar-head {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
  color: #84958a;
  font-size: 12px;
}

.order-list {
  margin: 0;
  padding: 0;
  list-style: none;
}

.order-list li {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 0;
  border-bottom: 1px dashed #e8efea;
}

.order-list li:last-child {
  border-bottom: none;
}

.order-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
}

.dot-wait {
  background: #e6a23c;
}

.dot-ship {
  background: #5b8ff9;
}

.dot-done {
  background: #2e9e5b;
}

.dot-reject {
  background: #e05b4d;
}

.order-label {
  color: #5d7668;
  font-size: 13px;
}

.order-list strong {
  margin-left: auto;
  color: #1f2d24;
  font-size: 18px;
}

.order-hint {
  width: 96px;
  color: #a3b1a8;
  font-size: 12px;
  text-align: right;
}

.order-list .el-button {
  width: 96px;
  justify-content: flex-end;
}

.stock-card {
  margin-top: 20px;
}

.stock-num {
  font-weight: 700;
}

.stock-low {
  color: #d94a3d;
}

.stock-table {
  margin-top: 4px;
}

.shortcut-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-top: 20px;
}

.shortcut-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 18px 20px;
  color: #2e9e5b;
  text-align: left;
  background: #fff;
  border: 1px solid #e3ebe6;
  border-radius: 14px;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
}

.shortcut-card:hover {
  transform: translateY(-2px);
  border-color: #b9dcc6;
  box-shadow: 0 12px 26px rgba(31, 61, 45, 0.09);
}

.shortcut-card strong {
  display: block;
  color: #1f2d24;
  font-size: 14px;
}

.shortcut-card small {
  display: block;
  margin-top: 4px;
  color: #84958a;
  font-size: 12px;
}

.shortcut-arrow {
  margin-left: auto;
  color: #b3c2b8;
}

@media (max-width: 1024px) {
  .kpi-grid,
  .shortcut-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .panel-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .workbench-page {
    padding: 20px 16px 60px;
  }

  .workbench-hero {
    flex-direction: column;
    align-items: flex-start;
    padding: 24px;
  }

  .kpi-grid,
  .shortcut-grid {
    grid-template-columns: 1fr;
  }
}
</style>
