<template>
  <div class="app-container">
    <el-alert
      type="info"
      :closable="false"
      show-icon
      title="按角色配置可访问的接口权限；修改后该角色下的用户需重新登录才能生效。"
      class="mb-4"
    />

    <el-card shadow="hover" class="table-section">
      <el-table v-loading="loading" :data="roleList" border>
        <el-table-column prop="name" label="角色名称" min-width="140" />
        <el-table-column prop="code" label="角色编码" min-width="120" />
        <el-table-column prop="sort" label="排序" width="80" align="center" />
        <el-table-column label="操作" width="140" align="center" fixed="right">
          <template #default="{ row }">
            <el-button
              v-hasPerm="['sys:role:update']"
              type="primary"
              link
              icon="setting"
              @click="openPermissionDialog(row)"
            >
              配置权限
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      :title="`配置权限 - ${currentRole?.name ?? ''}`"
      width="680px"
      top="6vh"
    >
      <div v-loading="permissionLoading" class="permission-panel">
        <div v-for="group in groupedPermissions" :key="group.type" class="permission-group">
          <div class="permission-group__title">
            {{ group.label }}
            <el-tag size="small" type="info">{{ group.items.length }}</el-tag>
          </div>
          <el-checkbox-group v-model="selectedIds" class="permission-group__items">
            <el-checkbox v-for="item in group.items" :key="item.id" :value="item.id">
              {{ item.name }}
              <span class="permission-code">{{ item.code }}</span>
            </el-checkbox>
          </el-checkbox-group>
        </div>
      </div>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { ElMessage } from "element-plus";
import RoleAPI, { type RoleItem, type PermissionItem } from "@/api/system/role";

defineOptions({ name: "Role" });

const loading = ref(false);
const roleList = ref<RoleItem[]>([]);
const permissions = ref<PermissionItem[]>([]);
const selectedIds = ref<number[]>([]);

const dialogVisible = ref(false);
const permissionLoading = ref(false);
const saving = ref(false);
const currentRole = ref<RoleItem>();

const groupedPermissions = computed(() => {
  const groups = [
    { type: "MENU", label: "菜单权限" },
    { type: "BUTTON", label: "接口操作权限" },
  ];
  return groups
    .map((group) => ({
      ...group,
      items: permissions.value
        .filter((item) => item.type === group.type)
        .sort((a, b) => (a.sort ?? 0) - (b.sort ?? 0)),
    }))
    .filter((group) => group.items.length > 0);
});

async function loadRoles(): Promise<void> {
  loading.value = true;
  try {
    roleList.value = await RoleAPI.getList();
  } finally {
    loading.value = false;
  }
}

async function loadPermissions(): Promise<void> {
  if (permissions.value.length === 0) {
    permissions.value = await RoleAPI.getPermissions();
  }
}

async function openPermissionDialog(role: RoleItem): Promise<void> {
  currentRole.value = role;
  dialogVisible.value = true;
  permissionLoading.value = true;
  try {
    await loadPermissions();
    selectedIds.value = await RoleAPI.getRolePermissionIds(role.id);
  } finally {
    permissionLoading.value = false;
  }
}

async function handleSave(): Promise<void> {
  if (!currentRole.value) return;
  saving.value = true;
  try {
    await RoleAPI.updateRolePermissions(currentRole.value.id, selectedIds.value);
    ElMessage.success("权限配置已保存，相关用户重新登录后生效");
    dialogVisible.value = false;
  } finally {
    saving.value = false;
  }
}

onMounted(loadRoles);
</script>

<style lang="scss" scoped>
.permission-panel {
  max-height: 60vh;
  overflow-y: auto;
  padding-right: 4px;
}

.permission-group + .permission-group {
  margin-top: 18px;
}

.permission-group__title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.permission-group__items {
  display: flex;
  flex-wrap: wrap;
  gap: 4px 18px;
}

.permission-code {
  margin-left: 4px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
</style>
