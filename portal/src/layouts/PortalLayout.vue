<template>
  <div class="portal-shell">
    <header class="portal-nav">
      <div class="nav-inner">
        <router-link to="/" class="brand">
          <img class="brand-logo" src="/logo.png" alt="Nougyou" />
          <span class="brand-name">Nougyou</span>
        </router-link>

        <nav class="nav-links">
          <router-link to="/" class="nav-link">首页</router-link>
          <router-link to="/projects" class="nav-link">农家乐项目</router-link>
          <router-link to="/products" class="nav-link">农产品商城</router-link>
          <router-link v-if="isFarmer" to="/merchant/workbench" class="nav-link"
            >商家工作台</router-link
          >
          <router-link to="/consult" class="nav-link">我的咨询</router-link>
          <router-link to="/join" class="nav-link">加入我们</router-link>
          <router-link to="/profile" class="nav-link">个人中心</router-link>
        </nav>

        <div class="nav-actions">
          <template v-if="store.state.user">
            <router-link v-if="canUseCart" to="/cart" class="cart-entry">
              <el-badge :value="cartCount" :hidden="cartCount === 0" :max="99">
                <el-button text :icon="ShoppingCart" />
              </el-badge>
            </router-link>
            <el-dropdown @command="handleCommand">
              <span class="user-entry">
                <el-avatar :size="30" class="user-avatar">{{
                  initials
                }}</el-avatar>
                <span class="user-name">{{
                  store.state.user.nickname || store.state.user.username
                }}</span>
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item v-if="canViewOrders" command="orders"
                    >我的订单</el-dropdown-item
                  >
                  <el-dropdown-item v-if="canUseCart" command="cart"
                    >我的购物车</el-dropdown-item
                  >
                  <el-dropdown-item command="consult"
                    >我的咨询</el-dropdown-item
                  >
                  <el-dropdown-item v-if="isFarmer" command="merchant-workbench"
                    >商家工作台</el-dropdown-item
                  >
                  <el-dropdown-item v-if="isFarmer" command="merchant"
                    >我的农家乐</el-dropdown-item
                  >
                  <el-dropdown-item v-if="isFarmer" command="merchant-products"
                    >我的农产品</el-dropdown-item
                  >
                  <el-dropdown-item v-if="isFarmer" command="merchant-orders"
                    >商家订单</el-dropdown-item
                  >
                  <el-dropdown-item v-if="isFarmer" command="merchant-entrusts"
                    >委托销售</el-dropdown-item
                  >
                  <el-dropdown-item command="profile"
                    >个人中心</el-dropdown-item
                  >
                  <el-dropdown-item command="logout" divided
                    >退出登录</el-dropdown-item
                  >
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
          <template v-else>
            <router-link to="/login">
              <el-button text>登录</el-button>
            </router-link>
            <router-link to="/register">
              <el-button type="primary">注册</el-button>
            </router-link>
          </template>
        </div>
      </div>
    </header>

    <main class="portal-main">
      <router-view />
    </main>

    <footer class="portal-footer">
      <span>© 2026 Nougyou · Vue3 + Element Plus + Spring Boot</span>
    </footer>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { ShoppingCart } from "@element-plus/icons-vue";
import { useUserStore } from "../store/user";
import { getCart } from "../api/cart";
import { onCartChanged } from "../utils/cartBus";

const router = useRouter();
const route = useRoute();
const store = useUserStore();
const cartCount = ref(0);

const initials = computed(() => {
  const name = store.state.user?.nickname || store.state.user?.username || "U";
  return name.slice(0, 1).toUpperCase();
});

const isFarmer = computed(() =>
  (store.state.user?.roles || []).includes("FARMER"),
);

/**
 * 购物车入口按权限点判断，而不是按角色写死：
 * 角色权限里授予 cart:list 后即可使用；未返回 perms 的旧缓存按角色兜底。
 */
const canUseCart = computed(() => {
  const perms = store.state.user?.perms || [];
  return perms.length ? perms.includes("cart:list") : !isFarmer.value;
});

/** 我的订单入口按 order:list 权限显示，未返回 perms 的旧缓存按角色兜底 */
const canViewOrders = computed(() => {
  const perms = store.state.user?.perms || [];
  return perms.length ? perms.includes("order:list") : !isFarmer.value;
});

async function loadCartCount() {
  // 无购物车权限（如农户商家）直接跳过，避免无谓的 403 提示
  if (!store.isLoggedIn() || !canUseCart.value) {
    cartCount.value = 0;
    return;
  }
  try {
    const items = await getCart({ silent: true });
    cartCount.value = (items || []).reduce(
      (sum, item) => sum + (item.quantity || 0),
      0,
    );
  } catch {
    cartCount.value = 0;
  }
}

async function handleCommand(command) {
  if (command === "orders") {
    router.push("/orders");
    return;
  }
  if (command === "cart") {
    if (canUseCart.value) router.push("/cart");
    return;
  }
  if (command === "consult") {
    router.push("/consult");
    return;
  }
  if (command === "merchant-workbench") {
    router.push("/merchant/workbench");
    return;
  }
  if (command === "merchant") {
    router.push("/merchant/projects");
    return;
  }
  if (command === "merchant-products") {
    router.push("/merchant/products");
    return;
  }
  if (command === "merchant-orders") {
    router.push("/merchant/orders");
    return;
  }
  if (command === "merchant-entrusts") {
    router.push("/merchant/entrusts");
    return;
  }
  if (command === "profile") {
    router.push("/profile");
    return;
  }
  if (command === "logout") {
    store.logout();
    ElMessage.success("已退出登录");
    router.push("/");
  }
}

let stopCartWatch = null;

onMounted(async () => {
  // 每次进站都刷新一次用户信息，角色权限调整后无需重新登录即可生效
  if (store.isLoggedIn()) {
    await store.loadUser().catch(() => {});
  }
  await loadCartCount();
  // 加购 / 改数量 / 删除后实时刷新徽标
  stopCartWatch = onCartChanged(loadCartCount);
});

onBeforeUnmount(() => {
  if (stopCartWatch) stopCartWatch();
});

watch(
  () => route.fullPath,
  () => {
    loadCartCount();
  },
);
</script>
