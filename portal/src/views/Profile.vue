<template>
  <div class="profile-page">
    <el-skeleton v-if="loading" :rows="4" animated />

    <template v-else-if="store.state.user">
      <div class="profile-card">
        <el-avatar :size="72" class="profile-avatar">{{ initials }}</el-avatar>
        <div class="profile-head">
          <h2>{{ store.state.user.nickname || store.state.user.username }}</h2>
          <p v-if="shopName" class="profile-shop">店铺 · {{ shopName }}</p>
          <div class="role-tags">
            <el-tag v-for="role in store.state.user.roles" :key="role" type="success" effect="plain">
              {{ roleLabel(role) }}
            </el-tag>
            <el-tag v-if="!store.state.user.roles?.length" type="info" effect="plain">买家</el-tag>
          </div>
        </div>
      </div>

      <el-card shadow="never" class="info-card">
        <template #header>账号信息</template>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="用户 ID">{{ store.state.user.userId }}</el-descriptions-item>
          <el-descriptions-item label="用户名">{{ store.state.user.username }}</el-descriptions-item>
          <el-descriptions-item label="昵称">{{ store.state.user.nickname || "-" }}</el-descriptions-item>
          <el-descriptions-item v-if="shopName" label="店铺">
            {{ shopName }}
          </el-descriptions-item>
          <el-descriptions-item label="角色">
            {{ roleNames }}
          </el-descriptions-item>
        </el-descriptions>
        <div class="info-actions">
          <el-button type="danger" plain @click="handleLogout">退出登录</el-button>
        </div>
      </el-card>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { useUserStore } from "../store/user";

const router = useRouter();
const store = useUserStore();
const loading = ref(true);

const ROLE_LABELS = {
  ADMIN: "管理员",
  FARMER: "农户商家",
  BUYER: "买家"
};

function roleLabel(role) {
  return ROLE_LABELS[role] || role;
}

const roleNames = computed(() => {
  const roles = store.state.user?.roles || [];
  return roles.length ? roles.map(roleLabel).join("、") : "买家";
});

/** 仅商家账号有店铺名，买家/管理员不展示 */
const shopName = computed(() => store.state.user?.shopName || "");

const initials = computed(() => {
  const name = store.state.user?.nickname || store.state.user?.username || "U";
  return name.slice(0, 1).toUpperCase();
});

function handleLogout() {
  store.logout();
  ElMessage.success("已退出登录");
  router.push("/");
}

onMounted(async () => {
  try {
    await store.loadUser();
  } catch {
    router.replace({ name: "login", query: { redirect: "/profile" } });
  } finally {
    loading.value = false;
  }
});
</script>
