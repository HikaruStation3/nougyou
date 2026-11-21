<template>
  <div class="portal-shell">
    <header class="portal-nav">
      <div class="nav-inner">
        <router-link to="/" class="brand">
          <span class="brand-logo">N</span>
          <span class="brand-name">Nougyou</span>
        </router-link>

        <nav class="nav-links">
          <router-link to="/" class="nav-link">首页</router-link>
          <router-link to="/profile" class="nav-link">个人中心</router-link>
        </nav>

        <div class="nav-actions">
          <template v-if="store.state.user">
            <el-dropdown @command="handleCommand">
              <span class="user-entry">
                <el-avatar :size="30" class="user-avatar">{{ initials }}</el-avatar>
                <span class="user-name">{{ store.state.user.nickname || store.state.user.username }}</span>
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                  <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
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
import { computed, onMounted } from "vue";
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { useUserStore } from "../store/user";

const router = useRouter();
const store = useUserStore();

const initials = computed(() => {
  const name = store.state.user?.nickname || store.state.user?.username || "U";
  return name.slice(0, 1).toUpperCase();
});

async function handleCommand(command) {
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

onMounted(() => {
  if (store.isLoggedIn() && !store.state.user) {
    store.loadUser().catch(() => {});
  }
});
</script>
