<template>
  <div class="home-page">
    <!-- 首屏 -->
    <section class="home-hero">
      <div class="hero-glow hero-glow-a"></div>
      <div class="hero-glow hero-glow-b"></div>
      <div class="hero-inner">
        <div class="hero-copy">
          <span class="hero-eyebrow">钟山镇 · 扶贫助农</span>
          <h1>山货出山，<br />好日子进村。</h1>
          <p class="hero-desc">
            把钟山镇的茶叶、米粉、蜂蜜和山水一起搬上货架。农户直供、产地直发，
            每一笔订单都能查到它来自哪家农户、哪片山坡。
          </p>
          <div class="hero-actions">
            <el-button type="primary" size="large" @click="router.push('/products')">
              去逛农产品商城
              <el-icon class="btn-icon"><ArrowRight /></el-icon>
            </el-button>
            <el-button size="large" @click="router.push('/projects')">
              看看农家乐
            </el-button>
          </div>
          <div class="hero-trust">
            <span><el-icon><Van /></el-icon> 产地直发</span>
            <span><el-icon><PriceTag /></el-icon> 农户定价</span>
            <span><el-icon><Sunny /></el-icon> 当季山货</span>
          </div>
        </div>

        <aside class="hero-card">
          <div class="hero-card-head">
            <span>本期精选</span>
            <em>实时更新</em>
          </div>
          <div class="hero-metrics">
            <div class="hero-metric">
              <strong>{{ home.policies.length }}</strong>
              <span>扶贫政策</span>
            </div>
            <div class="hero-metric">
              <strong>{{ home.news.length }}</strong>
              <span>乡镇资讯</span>
            </div>
            <div class="hero-metric">
              <strong>{{ home.projects.length }}</strong>
              <span>农家乐</span>
            </div>
            <div class="hero-metric">
              <strong>{{ home.hotProducts.length }}</strong>
              <span>热销好物</span>
            </div>
          </div>
          <div class="hero-card-note">
            <span class="dot"></span>
            数据来自平台公开内容，每次进站自动刷新
          </div>
        </aside>
      </div>
    </section>

    <!-- 快捷入口 -->
    <section class="quick-grid">
      <button
        v-for="item in quickEntries"
        :key="item.title"
        type="button"
        class="quick-card"
        @click="item.action"
      >
        <span class="quick-icon">
          <el-icon :size="22"><component :is="item.icon" /></el-icon>
        </span>
        <span class="quick-text">
          <strong>{{ item.title }}</strong>
          <small>{{ item.desc }}</small>
        </span>
        <el-icon class="quick-arrow"><ArrowRight /></el-icon>
      </button>
    </section>

    <!-- 扶贫政策 -->
    <section id="policies" class="home-section">
      <header class="section-head">
        <div>
          <h2>扶贫政策</h2>
          <p>产业奖补、信贷贴息、公益岗位，把政策讲清楚再落地。</p>
        </div>
        <div class="section-side">
          <span class="section-count">共 {{ home.policies.length }} 条最新政策</span>
          <router-link class="section-more" to="/policies">
            查看全部<el-icon><ArrowRight /></el-icon>
          </router-link>
        </div>
      </header>

      <div v-loading="loading" class="policy-layout">
        <el-empty v-if="!loading && !home.policies.length" description="暂无已发布政策" />
        <template v-else-if="home.policies.length">
          <article class="policy-feature" @click="openPolicy(featuredPolicy.id)">
            <el-icon class="policy-watermark" :size="180"><Document /></el-icon>
            <div class="policy-feature-top">
              <span class="policy-badge">最新发布</span>
              <span class="policy-date">{{ formatDate(featuredPolicy.publishTime) }}</span>
            </div>
            <h3>{{ featuredPolicy.title }}</h3>
            <p>{{ featuredPolicy.summary || "政策正文已通过审核，可在政策专栏查看。" }}</p>
            <div class="policy-feature-foot">
              <span>{{ featuredPolicy.issueDept || "钟山镇人民政府" }}</span>
              <span v-if="featuredPolicy.policyNo">文号 {{ featuredPolicy.policyNo }}</span>
              <span class="policy-views">
                <el-icon><View /></el-icon>{{ featuredPolicy.viewCount || 0 }}
              </span>
            </div>
          </article>

          <ul class="policy-list">
            <li
              v-for="item in restPolicies"
              :key="item.id"
              class="policy-item"
              @click="openPolicy(item.id)"
            >
              <time>{{ formatDay(item.publishTime) }}</time>
              <div class="policy-item-main">
                <h4>{{ item.title }}</h4>
                <p>{{ item.issueDept || "钟山镇人民政府" }}</p>
              </div>
              <span class="policy-views">
                <el-icon><View /></el-icon>{{ item.viewCount || 0 }}
              </span>
            </li>
          </ul>
        </template>
      </div>
    </section>

    <!-- 乡镇资讯 -->
    <section class="home-section">
      <header class="section-head">
        <div>
          <h2>乡镇资讯</h2>
          <p>景点、线路、历史与人文，先认识钟山，再走进钟山。</p>
        </div>
        <div class="section-side">
          <span class="section-count">{{ home.news.length }} 条资讯</span>
          <router-link class="section-more" to="/news">
            查看全部<el-icon><ArrowRight /></el-icon>
          </router-link>
        </div>
      </header>

      <div v-loading="loading" class="news-grid">
        <el-empty v-if="!loading && !home.news.length" description="暂无已发布资讯" />
        <article
          v-for="item in home.news"
          :key="item.id"
          class="news-card"
          :class="`news-type-${item.newsType}`"
          @click="openNews(item.id)"
        >
          <div class="news-card-top">
            <span class="news-type">{{ item.newsTypeName }}</span>
            <time>{{ formatDate(item.publishTime) }}</time>
          </div>
          <h3>{{ item.title }}</h3>
          <p>{{ item.summary || "钟山镇乡镇资讯。" }}</p>
          <div class="news-card-foot">
            <span v-if="item.location" class="news-location">
              <el-icon><Location /></el-icon>{{ item.location }}
            </span>
            <span class="policy-views">
              <el-icon><View /></el-icon>{{ item.viewCount || 0 }}
            </span>
          </div>
        </article>
      </div>
    </section>

    <!-- 推荐农家乐 -->
    <section class="home-section">
      <header class="section-head">
        <div>
          <h2>推荐农家乐</h2>
          <p>住进村里，吃在农家，把周末交给山水。</p>
        </div>
        <el-button text type="primary" @click="router.push('/projects')">
          查看全部<el-icon class="btn-icon"><ArrowRight /></el-icon>
        </el-button>
      </header>

      <div v-loading="loading" class="project-grid">
        <el-empty v-if="!loading && !home.projects.length" description="暂无上架项目" />
        <article
          v-for="item in home.projects"
          :key="item.id"
          class="project-card"
          @click="router.push(`/projects/${item.id}`)"
        >
          <div class="project-cover">
            <el-image v-if="item.cover" :src="item.cover" fit="cover" lazy>
              <template #error>
                <div class="cover-fallback"><el-icon :size="28"><House /></el-icon></div>
              </template>
            </el-image>
            <div v-else class="cover-fallback"><el-icon :size="28"><House /></el-icon></div>
            <span class="project-price">{{ item.priceDesc || "价格到店咨询" }}</span>
          </div>
          <div class="project-body">
            <h3>{{ item.title }}</h3>
            <p class="project-address">
              <el-icon><Location /></el-icon>{{ item.address || "钟山镇" }}
            </p>
            <div class="project-meta">
              <span>可接待 {{ item.capacity || "-" }} 人</span>
              <span>{{ item.shopName || item.farmerName || "本地农户" }}</span>
            </div>
          </div>
        </article>
      </div>
    </section>

    <!-- 热销农产品 -->
    <section class="home-section">
      <header class="section-head">
        <div>
          <h2>热销农产品</h2>
          <p>按累计销量排序，卖得最好的往往也是最地道的。</p>
        </div>
        <el-button text type="primary" @click="router.push('/products')">
          进入商城<el-icon class="btn-icon"><ArrowRight /></el-icon>
        </el-button>
      </header>

      <div v-loading="loading" class="goods-grid">
        <el-empty v-if="!loading && !home.hotProducts.length" description="暂无在售商品" />
        <article
          v-for="item in home.hotProducts"
          :key="item.id"
          class="goods-card"
          @click="router.push(`/products/${item.id}`)"
        >
          <div class="goods-cover">
            <el-image v-if="item.cover" :src="item.cover" fit="cover" lazy>
              <template #error>
                <div class="cover-fallback"><el-icon :size="26"><Goods /></el-icon></div>
              </template>
            </el-image>
            <div v-else class="cover-fallback"><el-icon :size="26"><Goods /></el-icon></div>
            <span v-if="item.isPoverty === 1" class="goods-flag">扶贫产品</span>
          </div>
          <div class="goods-body">
            <h3>{{ item.name }}</h3>
            <p class="goods-origin">
              <el-icon><Location /></el-icon>{{ item.origin || item.shopName || "钟山镇" }}
            </p>
            <div class="goods-foot">
              <span class="goods-price">
                <small>￥</small>{{ money(item.price) }}
                <em>/{{ item.unit || "件" }}</em>
              </span>
              <span class="goods-sale">已售 {{ item.saleCount || 0 }}</span>
            </div>
          </div>
        </article>
      </div>
    </section>

    <!-- 商家入口 -->
    <section class="merchant-banner">
      <div class="merchant-copy">
        <span class="merchant-eyebrow">{{ isFarmer ? "农户商家" : "想卖山货？" }}</span>
        <h2>{{ isFarmer ? "今天的生意，一眼看清" : "把自家山货，摆上平台货架" }}</h2>
        <p v-if="isFarmer">
          商品库存、待发货订单、今日成交与售后待办都在商家工作台，登录后直接看数。
        </p>
        <p v-else>
          提交贫困户资料完成入驻审核，即可上架农产品、申请农家乐项目，并由平台协助对接买家。
        </p>
        <el-button
          type="primary"
          size="large"
          @click="router.push(isFarmer ? '/merchant/workbench' : '/join')"
        >
          {{ isFarmer ? "进入商家工作台" : "申请入驻" }}
          <el-icon class="btn-icon"><ArrowRight /></el-icon>
        </el-button>
      </div>
      <div class="merchant-steps">
        <div v-for="(step, index) in merchantSteps" :key="step.title" class="merchant-step">
          <span class="step-index">0{{ index + 1 }}</span>
          <div>
            <strong>{{ step.title }}</strong>
            <small>{{ step.desc }}</small>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import {
  ArrowRight,
  Document,
  Goods,
  House,
  Location,
  PriceTag,
  Shop,
  ShoppingBag,
  Sunny,
  Van,
  View,
} from "@element-plus/icons-vue";
import { getHome } from "../api/home";
import { useUserStore } from "../store/user";
import { money } from "../utils/format";

const router = useRouter();
const store = useUserStore();

const loading = ref(true);
const home = reactive({
  policies: [],
  news: [],
  projects: [],
  hotProducts: [],
});

const isFarmer = computed(() =>
  (store.state.user?.roles || []).includes("FARMER"),
);

const featuredPolicy = computed(() => home.policies[0] || {});
const restPolicies = computed(() => home.policies.slice(1));

const quickEntries = computed(() => [
  {
    title: "农产品商城",
    desc: "山货直供，产地直发",
    icon: ShoppingBag,
    action: () => router.push("/products"),
  },
  {
    title: "农家乐项目",
    desc: "采摘、住宿、乡村体验",
    icon: House,
    action: () => router.push("/projects"),
  },
  {
    title: "扶贫政策",
    desc: "奖补、贴息与公益岗位",
    icon: Document,
    action: () => router.push("/policies"),
  },
  isFarmer.value
    ? {
        title: "商家工作台",
        desc: "库存、订单与今日成交",
        icon: Shop,
        action: () => router.push("/merchant/workbench"),
      }
    : {
        title: "加入我们",
        desc: "贫困户入驻，平台帮扶",
        icon: Shop,
        action: () => router.push("/join"),
      },
]);

const merchantSteps = [
  { title: "提交资料", desc: "身份证与贫困户证明" },
  { title: "平台审核", desc: "管理员核验并开通商家能力" },
  { title: "上架经营", desc: "农产品、农家乐与委托代销" },
];

function formatDate(value) {
  return value ? String(value).slice(0, 10) : "--";
}

function formatDay(value) {
  return value ? String(value).slice(5, 10) : "--";
}

function openPolicy(id) {
  if (id) {
    router.push(`/policies/${id}`);
  }
}

function openNews(id) {
  if (id) {
    router.push(`/news/${id}`);
  }
}

async function loadHome() {
  loading.value = true;
  try {
    const data = await getHome();
    home.policies = data?.policies || [];
    home.news = data?.news || [];
    home.projects = data?.projects || [];
    home.hotProducts = data?.hotProducts || [];
  } finally {
    loading.value = false;
  }
}

onMounted(loadHome);
</script>

<style scoped>
.home-page {
  max-width: 1160px;
  margin: 0 auto;
  padding: 32px 24px 80px;
}

/* ---------- 首屏 ---------- */
.home-hero {
  position: relative;
  overflow: hidden;
  padding: 56px 48px 48px;
  border-radius: 30px;
  background: linear-gradient(135deg, #edf7f0 0%, #f9fcfa 48%, #eef4fb 100%);
  border: 1px solid #e1ece5;
}

.hero-glow {
  position: absolute;
  border-radius: 50%;
  filter: blur(10px);
  pointer-events: none;
}

.hero-glow-a {
  width: 340px;
  height: 340px;
  top: -140px;
  right: 90px;
  background: radial-gradient(circle, rgba(46, 158, 91, 0.22), transparent 68%);
}

.hero-glow-b {
  width: 300px;
  height: 300px;
  bottom: -160px;
  left: -60px;
  background: radial-gradient(circle, rgba(83, 146, 255, 0.14), transparent 70%);
}

.hero-inner {
  position: relative;
  display: grid;
  grid-template-columns: 1.15fr 0.85fr;
  gap: 48px;
  align-items: center;
}

.hero-eyebrow {
  display: inline-flex;
  align-items: center;
  padding: 6px 14px;
  color: #257e48;
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 1px;
  background: rgba(46, 158, 91, 0.1);
  border: 1px solid rgba(46, 158, 91, 0.22);
  border-radius: 999px;
}

.hero-copy h1 {
  margin: 20px 0 18px;
  color: #16301f;
  font-size: 52px;
  line-height: 1.18;
  letter-spacing: -1px;
}

.hero-desc {
  max-width: 520px;
  margin: 0;
  color: #5d7668;
  font-size: 16px;
  line-height: 1.85;
}

.hero-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 28px;
}

.btn-icon {
  margin-left: 4px;
}

.hero-trust {
  display: flex;
  flex-wrap: wrap;
  gap: 20px;
  margin-top: 26px;
  color: #6b7c72;
  font-size: 13px;
}

.hero-trust span {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.hero-card {
  padding: 24px 26px;
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid #e1ece5;
  border-radius: 22px;
  box-shadow: 0 24px 56px rgba(31, 61, 45, 0.12);
  backdrop-filter: blur(6px);
}

.hero-card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: #1f2d24;
  font-size: 15px;
  font-weight: 700;
}

.hero-card-head em {
  color: #2e9e5b;
  font-size: 12px;
  font-style: normal;
  font-weight: 500;
}

.hero-metrics {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
  margin-top: 18px;
}

.hero-metric {
  padding: 16px 14px;
  background: #f5faf7;
  border-radius: 14px;
}

.hero-metric strong {
  display: block;
  color: #1c6f42;
  font-size: 30px;
  line-height: 1;
  letter-spacing: -0.5px;
}

.hero-metric span {
  display: block;
  margin-top: 8px;
  color: #6b7c72;
  font-size: 12px;
}

.hero-card-note {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 18px;
  color: #84958a;
  font-size: 12px;
}

.dot {
  width: 6px;
  height: 6px;
  background: #2e9e5b;
  border-radius: 50%;
}

/* ---------- 快捷入口 ---------- */
.quick-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-top: 24px;
}

.quick-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 18px 20px;
  text-align: left;
  background: #fff;
  border: 1px solid #e3ebe6;
  border-radius: 16px;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
}

.quick-card:hover {
  transform: translateY(-2px);
  border-color: #b9dcc6;
  box-shadow: 0 12px 26px rgba(31, 61, 45, 0.09);
}

.quick-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 44px;
  width: 44px;
  height: 44px;
  color: #2e9e5b;
  background: #eef8f1;
  border-radius: 13px;
}

.quick-text {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.quick-text strong {
  color: #1f2d24;
  font-size: 15px;
}

.quick-text small {
  color: #84958a;
  font-size: 12px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.quick-arrow {
  margin-left: auto;
  color: #b3c2b8;
}

/* ---------- 通用区块 ---------- */
.home-section {
  margin-top: 64px;
}

.section-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 22px;
}

.section-head h2 {
  margin: 0 0 8px;
  color: #1f2d24;
  font-size: 26px;
  letter-spacing: -0.3px;
}

.section-head p {
  margin: 0;
  color: #7b8c81;
  font-size: 14px;
}

.section-count {
  color: #9aa9a0;
  font-size: 13px;
  white-space: nowrap;
}

.section-side {
  display: flex;
  align-items: center;
  gap: 14px;
}

.section-more {
  display: inline-flex;
  gap: 2px;
  align-items: center;
  color: #2e9e5b;
  font-size: 13px;
  text-decoration: none;
  white-space: nowrap;
}

.section-more:hover {
  color: #1a6440;
}

.section-more .el-icon {
  font-size: 12px;
  transition: transform 0.16s ease;
}

.section-more:hover .el-icon {
  transform: translateX(2px);
}

/* ---------- 政策 ---------- */
.policy-layout {
  display: grid;
  grid-template-columns: 0.95fr 1.05fr;
  gap: 20px;
  min-height: 240px;
}

.policy-layout > .el-empty {
  grid-column: 1 / -1;
}

.policy-feature {
  position: relative;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  padding: 30px 32px;
  color: #fff;
  background: linear-gradient(150deg, #2e9e5b, #1a6440);
  border-radius: 20px;
  box-shadow: 0 18px 40px rgba(26, 100, 64, 0.22);
  cursor: pointer;
  transition: transform 0.18s ease, box-shadow 0.18s ease;
}

.policy-feature:hover {
  transform: translateY(-2px);
  box-shadow: 0 22px 48px rgba(26, 100, 64, 0.3);
}

.policy-watermark {
  position: absolute;
  right: -26px;
  bottom: -32px;
  color: rgba(255, 255, 255, 0.08);
  pointer-events: none;
}

.policy-feature > :not(.policy-watermark) {
  position: relative;
  z-index: 1;
}

.policy-feature-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.policy-badge {
  padding: 4px 10px;
  font-size: 12px;
  background: rgba(255, 255, 255, 0.18);
  border-radius: 999px;
}

.policy-date {
  color: rgba(255, 255, 255, 0.72);
  font-size: 12px;
}

.policy-feature h3 {
  margin: 22px 0 14px;
  font-size: 24px;
  line-height: 1.5;
}

.policy-feature p {
  flex: 1;
  margin: 0;
  color: rgba(255, 255, 255, 0.82);
  font-size: 14px;
  line-height: 1.85;
}

.policy-feature-foot {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  margin-top: 24px;
  color: rgba(255, 255, 255, 0.72);
  font-size: 12px;
}

.policy-list {
  display: flex;
  flex-direction: column;
  margin: 0;
  padding: 6px 22px;
  list-style: none;
  background: #fff;
  border: 1px solid #e3ebe6;
  border-radius: 20px;
}

.policy-item {
  display: flex;
  align-items: center;
  gap: 18px;
  margin: 0 -10px;
  padding: 18px 10px;
  border-bottom: 1px dashed #e8efea;
  border-radius: 10px;
  cursor: pointer;
  transition: background 0.16s ease;
}

.policy-item:hover {
  background: #f5faf7;
}

.policy-item:last-child {
  border-bottom: none;
}

.policy-item time {
  flex: 0 0 46px;
  color: #2e9e5b;
  font-size: 14px;
  font-weight: 600;
}

.policy-item-main {
  flex: 1;
  min-width: 0;
}

.policy-item-main h4 {
  margin: 0 0 6px;
  color: #1f2d24;
  font-size: 15px;
  font-weight: 600;
  line-height: 1.6;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
}

.policy-item-main p {
  margin: 0;
  color: #84958a;
  font-size: 12px;
}

.policy-views {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: #9aa9a0;
  font-size: 12px;
  white-space: nowrap;
}

/* ---------- 资讯 ---------- */
.news-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  min-height: 180px;
}

.news-grid > .el-empty {
  grid-column: 1 / -1;
}

.news-card {
  display: flex;
  flex-direction: column;
  padding: 20px 20px 18px;
  background: #fff;
  border: 1px solid #e3ebe6;
  border-top: 3px solid #2e9e5b;
  border-radius: 16px;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.news-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 16px 32px rgba(31, 61, 45, 0.1);
}

.news-type-2 {
  border-top-color: #e6a23c;
}

.news-type-3 {
  border-top-color: #5b8ff9;
}

.news-type-4 {
  border-top-color: #8b6bd9;
}

.news-type-5 {
  border-top-color: #3aa6a0;
}

.news-card-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.news-type {
  color: #2e9e5b;
  font-size: 12px;
  font-weight: 600;
}

.news-type-2 .news-type {
  color: #c07a12;
}

.news-type-3 .news-type {
  color: #3f6fd1;
}

.news-type-4 .news-type {
  color: #7050c4;
}

.news-type-5 .news-type {
  color: #2b8a85;
}

.news-card-top time {
  color: #a3b1a8;
  font-size: 11px;
}

.news-card h3 {
  margin: 14px 0 10px;
  color: #1f2d24;
  font-size: 15px;
  line-height: 1.65;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.news-card p {
  flex: 1;
  margin: 0;
  color: #7b8c81;
  font-size: 13px;
  line-height: 1.75;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.news-card-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-top: 16px;
}

.news-location {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: #7b8c81;
  font-size: 12px;
  min-width: 0;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

/* ---------- 农家乐 ---------- */
.project-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
  min-height: 180px;
}

.project-grid > .el-empty {
  grid-column: 1 / -1;
}

.project-card {
  overflow: hidden;
  background: #fff;
  border: 1px solid #e3ebe6;
  border-radius: 18px;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.project-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 18px 36px rgba(31, 61, 45, 0.11);
}

.project-cover {
  position: relative;
  height: 170px;
  background: #eef4f0;
}

.project-cover :deep(.el-image) {
  width: 100%;
  height: 100%;
}

.cover-fallback {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  color: #a9c4b4;
  background: #eef4f0;
}

.project-price {
  position: absolute;
  left: 14px;
  bottom: 14px;
  padding: 5px 12px;
  color: #1c6f42;
  font-size: 12px;
  font-weight: 600;
  background: rgba(255, 255, 255, 0.92);
  border-radius: 999px;
}

.project-body {
  padding: 18px 20px 20px;
}

.project-body h3 {
  margin: 0 0 10px;
  color: #1f2d24;
  font-size: 17px;
  line-height: 1.5;
}

.project-address {
  display: flex;
  align-items: center;
  gap: 5px;
  margin: 0 0 14px;
  color: #7b8c81;
  font-size: 13px;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.project-meta {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  padding-top: 14px;
  color: #84958a;
  font-size: 12px;
  border-top: 1px dashed #e8efea;
}

/* ---------- 商品 ---------- */
.goods-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  min-height: 180px;
}

.goods-grid > .el-empty {
  grid-column: 1 / -1;
}

.goods-card {
  overflow: hidden;
  background: #fff;
  border: 1px solid #e3ebe6;
  border-radius: 16px;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.goods-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 16px 32px rgba(31, 61, 45, 0.1);
}

.goods-cover {
  position: relative;
  height: 170px;
  background: #eef4f0;
}

.goods-cover :deep(.el-image) {
  width: 100%;
  height: 100%;
}

.goods-flag {
  position: absolute;
  left: 12px;
  top: 12px;
  padding: 4px 10px;
  color: #fff;
  font-size: 11px;
  background: linear-gradient(120deg, #e6a23c, #d48806);
  border-radius: 999px;
}

.goods-body {
  padding: 16px 18px 18px;
}

.goods-body h3 {
  margin: 0 0 8px;
  color: #1f2d24;
  font-size: 15px;
  line-height: 1.55;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
}

.goods-origin {
  display: flex;
  align-items: center;
  gap: 4px;
  margin: 0 0 14px;
  color: #84958a;
  font-size: 12px;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.goods-foot {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 8px;
}

.goods-price {
  color: #d94a3d;
  font-size: 22px;
  font-weight: 700;
  letter-spacing: -0.5px;
}

.goods-price small {
  margin-right: 2px;
  font-size: 13px;
  font-weight: 600;
}

.goods-price em {
  margin-left: 2px;
  color: #9aa9a0;
  font-size: 12px;
  font-style: normal;
  font-weight: 400;
}

.goods-sale {
  color: #a3b1a8;
  font-size: 12px;
  white-space: nowrap;
}

/* ---------- 商家入口 ---------- */
.merchant-banner {
  display: grid;
  grid-template-columns: 1.1fr 0.9fr;
  gap: 40px;
  align-items: center;
  margin-top: 70px;
  padding: 40px 44px;
  background: linear-gradient(120deg, #1d3b2a, #2e6b46 62%, #3f7d55);
  border-radius: 24px;
  box-shadow: 0 22px 48px rgba(29, 59, 42, 0.22);
}

.merchant-eyebrow {
  color: #a7d8b9;
  font-size: 13px;
  letter-spacing: 2px;
}

.merchant-copy h2 {
  margin: 14px 0 12px;
  color: #fff;
  font-size: 30px;
  letter-spacing: -0.5px;
}

.merchant-copy p {
  max-width: 520px;
  margin: 0 0 26px;
  color: rgba(255, 255, 255, 0.76);
  font-size: 14px;
  line-height: 1.8;
}

.merchant-steps {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.merchant-step {
  display: flex;
  gap: 14px;
  align-items: center;
  padding: 14px 18px;
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 14px;
}

.step-index {
  color: #a7d8b9;
  font-size: 18px;
  font-weight: 700;
  letter-spacing: 1px;
}

.merchant-step strong {
  display: block;
  color: #fff;
  font-size: 14px;
}

.merchant-step small {
  display: block;
  margin-top: 4px;
  color: rgba(255, 255, 255, 0.62);
  font-size: 12px;
}

@media (max-width: 1024px) {
  .hero-inner {
    grid-template-columns: 1fr;
    gap: 32px;
  }

  .hero-copy h1 {
    font-size: 42px;
  }

  .quick-grid,
  .news-grid,
  .goods-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .project-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .policy-layout,
  .merchant-banner {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .home-page {
    padding: 20px 16px 60px;
  }

  .home-hero {
    padding: 32px 24px;
  }

  .hero-copy h1 {
    font-size: 34px;
  }

  .quick-grid,
  .news-grid,
  .goods-grid,
  .project-grid {
    grid-template-columns: 1fr;
  }

  .section-head {
    flex-direction: column;
    align-items: flex-start;
  }

  .merchant-banner {
    padding: 28px 24px;
  }
}
</style>
