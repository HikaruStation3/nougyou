<template>
  <div class="app-container">
    <el-card shadow="never" class="mb-4">
      <div class="flex items-center justify-between">
        <span class="text-sm text-[--el-text-color-secondary]">
          维护农产品分类树，禁用后的分类将不在前台展示且不可用于上架新商品。
        </span>
        <el-button type="primary" icon="Plus" @click="openCreate()">新增一级分类</el-button>
      </div>
    </el-card>

    <el-card shadow="never">
      <el-table
        v-loading="loading"
        :data="categoryTree"
        row-key="id"
        border
        default-expand-all
        :tree-props="{ children: 'children' }"
      >
        <el-table-column label="分类名称" min-width="220">
          <template #default="{ row }">
            <span class="inline-flex items-center gap-2">
              <el-image
                v-if="isImageIcon(row.icon)"
                :src="row.icon"
                fit="cover"
                class="h-5 w-5 rounded"
              />
              <el-icon v-else-if="row.icon"><component :is="row.icon" /></el-icon>
              <span class="font-medium">{{ row.name }}</span>
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="id" label="ID" width="90" align="center" />
        <el-table-column label="图标地址" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">{{ row.icon || "-" }}</template>
        </el-table-column>
        <el-table-column prop="sort" label="排序" width="90" align="center">
          <template #default="{ row }">{{ row.sort ?? 0 }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" effect="plain">
              {{ row.status === 1 ? "启用" : "禁用" }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openCreate(row)">新增子分类</el-button>
            <el-button type="primary" link @click="openEdit(row)">编辑</el-button>
            <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑农产品分类' : '新增农产品分类'"
      width="560px"
      top="8vh"
      @close="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="分类名称" prop="name">
          <el-input v-model="form.name" placeholder="如：水果蔬菜" maxlength="50" show-word-limit />
        </el-form-item>
        <el-form-item label="父级分类">
          <el-select
            v-model="form.parentId"
            placeholder="不选则为一级分类"
            clearable
            style="width: 100%"
          >
            <el-option label="一级分类" value="0" />
            <el-option
              v-for="item in topLevelCategories"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="图标地址">
          <el-input
            v-model="form.icon"
            placeholder="图标地址或 Element Plus 图标名"
            maxlength="255"
          />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" :max="9999" style="width: 100%" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import ProductAPI, { type CategoryForm, type CategoryItem } from "@/api/product";

defineOptions({ name: "ProductCategory" });

const loading = ref(false);
const submitting = ref(false);
const categoryTree = ref<CategoryItem[]>([]);
const dialogVisible = ref(false);
const formRef = ref();
const isEdit = ref(false);
const editingId = ref<string>();

const emptyForm = (): Required<CategoryForm> => ({
  name: "",
  parentId: "0",
  icon: "",
  sort: 0,
  status: 1,
});

const form = reactive<Required<CategoryForm>>(emptyForm());

const rules = {
  name: [{ required: true, message: "请输入分类名称", trigger: "blur" }],
};

const topLevelCategories = computed(() =>
  categoryTree.value.filter((item) => !item.parentId || item.parentId === "0")
);

/** 图标既支持图片地址，也支持 Element Plus 图标名 */
function isImageIcon(icon?: string) {
  if (!icon) return false;
  return icon.startsWith("/") || icon.startsWith("http");
}

async function loadData() {
  loading.value = true;
  try {
    categoryTree.value = await ProductAPI.getAdminCategories();
  } finally {
    loading.value = false;
  }
}

function openCreate(parent?: any) {
  isEdit.value = false;
  editingId.value = undefined;
  Object.assign(form, emptyForm());
  if (parent) {
    form.parentId = parent.id;
  }
  dialogVisible.value = true;
}

function openEdit(row: any) {
  isEdit.value = true;
  editingId.value = row.id;
  Object.assign(form, {
    name: row.name || "",
    parentId: row.parentId || "0",
    icon: row.icon || "",
    sort: row.sort ?? 0,
    status: row.status ?? 1,
  });
  dialogVisible.value = true;
}

async function handleSubmit() {
  try {
    await formRef.value.validate();
  } catch {
    return;
  }
  submitting.value = true;
  try {
    const payload: CategoryForm = { ...form };
    if (isEdit.value && editingId.value) {
      await ProductAPI.updateCategory(editingId.value, payload);
      ElMessage.success("分类已更新");
    } else {
      await ProductAPI.createCategory(payload);
      ElMessage.success("分类已新增");
    }
    dialogVisible.value = false;
    await loadData();
  } finally {
    submitting.value = false;
  }
}

async function handleDelete(row: any) {
  try {
    await ElMessageBox.confirm(
      `确定删除分类「${row.name}」吗？分类下存在商品或子分类时将无法删除。`,
      "提示",
      { type: "warning" }
    );
  } catch {
    return;
  }
  await ProductAPI.deleteCategory(row.id);
  ElMessage.success("分类已删除");
  await loadData();
}

function resetForm() {
  Object.assign(form, emptyForm());
  formRef.value?.clearValidate();
}

onMounted(loadData);
</script>
