<template>
  <div class="merchant-page">
    <section class="page-hero">
      <h1>
        我的农产品
        <span v-if="shopName" class="hero-shop">店铺 · {{ shopName }}</span>
      </h1>
      <p>
        上架您家出产的农产品，上架后买家即可在商城选购，商品仅归属您的店铺。
      </p>
    </section>

    <template v-if="!isFarmer">
      <el-alert
        type="warning"
        :closable="false"
        show-icon
        title="仅农户商家可管理农产品"
        class="role-alert"
      >
        <template #default>
          您当前还不是农户商家，请先到
          <router-link to="/join" class="alert-link">「加入我们」</router-link>
          提交入驻申请，审核通过后即可在此管理商品。
        </template>
      </el-alert>
    </template>

    <template v-else>
      <el-card shadow="never" class="filter-card">
        <div class="toolbar">
          <el-form inline @submit.prevent>
            <el-form-item label="商品名称">
              <el-input
                v-model="query.keyword"
                placeholder="搜索商品 / 产地"
                clearable
                style="width: 200px"
                @keyup.enter="handleSearch"
              />
            </el-form-item>
            <el-form-item label="分类">
              <el-tree-select
                v-model="query.categoryId"
                :data="categoryOptions"
                :props="{ value: 'value', label: 'name', children: 'children' }"
                node-key="id"
                check-strictly
                placeholder="全部分类"
                clearable
                style="width: 180px"
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" icon="search" @click="handleSearch"
                >查询</el-button
              >
              <el-button icon="refresh" @click="handleReset">重置</el-button>
            </el-form-item>
          </el-form>
          <el-button type="success" icon="Plus" @click="openCreate"
            >上架农产品</el-button
          >
        </div>
      </el-card>

      <el-card shadow="never" class="table-card">
        <el-table v-loading="loading" :data="list" border stripe>
          <el-table-column label="商品" min-width="240">
            <template #default="{ row }">
              <div class="title-cell">
                <el-image
                  v-if="row.cover"
                  :src="row.cover"
                  fit="cover"
                  class="thumb"
                  :preview-src-list="[row.cover]"
                  preview-teleported
                />
                <div>
                  <div class="t-title">{{ row.name }}</div>
                  <div class="t-sub">{{ row.categoryName || "未分类" }}</div>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="售价" width="120" align="right">
            <template #default="{ row }">
              <div class="t-price">￥{{ money(row.price) }}</div>
              <div v-if="row.originalPrice" class="t-origin">
                ￥{{ money(row.originalPrice) }}
              </div>
            </template>
          </el-table-column>
          <el-table-column label="库存" width="100" align="center">
            <template #default="{ row }">
              {{ row.stock ?? 0 }} {{ row.unit || "" }}
            </template>
          </el-table-column>
          <el-table-column
            prop="saleCount"
            label="销量"
            width="80"
            align="center"
          >
            <template #default="{ row }">{{ row.saleCount || 0 }}</template>
          </el-table-column>
          <el-table-column label="扶贫产品" width="100" align="center">
            <template #default="{ row }">
              <el-tag v-if="row.isPoverty === 1" type="danger" effect="plain"
                >扶贫</el-tag
              >
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column label="上架状态" width="110" align="center">
            <template #default="{ row }">
              <el-switch
                :model-value="row.shelfStatus"
                :active-value="1"
                :inactive-value="0"
                @change="(val) => handleShelf(row, val)"
              />
            </template>
          </el-table-column>
          <el-table-column
            prop="origin"
            label="产地"
            min-width="110"
            show-overflow-tooltip
          >
            <template #default="{ row }">{{ row.origin || "-" }}</template>
          </el-table-column>
          <el-table-column
            label="操作"
            width="170"
            align="center"
            fixed="right"
          >
            <template #default="{ row }">
              <el-button
                type="primary"
                link
                @click="router.push(`/products/${row.id}`)"
                >查看</el-button
              >
              <el-button type="primary" link @click="openEdit(row)"
                >编辑</el-button
              >
              <el-button type="danger" link @click="handleDelete(row)"
                >删除</el-button
              >
            </template>
          </el-table-column>
        </el-table>

        <div class="pagination-wrap">
          <el-pagination
            v-model:current-page="query.pageNum"
            v-model:page-size="query.pageSize"
            :total="total"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next"
            background
            @size-change="loadData"
            @current-change="loadData"
          />
        </div>
      </el-card>

      <el-dialog
        v-model="dialogVisible"
        :title="isEdit ? '编辑农产品' : '上架农产品'"
        width="760px"
        top="6vh"
        @close="resetForm"
      >
        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-width="100px"
          label-position="right"
        >
          <el-row :gutter="18">
            <el-col :xs="24" :sm="12">
              <el-form-item label="商品名称" prop="name">
                <el-input
                  v-model="form.name"
                  placeholder="如：钟山蜜桔"
                  maxlength="150"
                  show-word-limit
                />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12">
              <el-form-item label="商品分类" prop="categoryId">
                <el-tree-select
                  v-model="form.categoryId"
                  :data="categoryOptions"
                  :props="formCategoryProps"
                  node-key="id"
                  check-strictly
                  placeholder="请选择分类（末级）"
                  style="width: 100%"
                />
              </el-form-item>
            </el-col>
            <el-col :span="24">
              <el-form-item label="封面图" prop="cover">
                <el-input
                  v-model="form.cover"
                  placeholder="请输入封面图 URL"
                  maxlength="255"
                />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12">
              <el-form-item label="售价" prop="price">
                <el-input-number
                  v-model="form.price"
                  :min="0.01"
                  :precision="2"
                  :step="1"
                  style="width: 100%"
                />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12">
              <el-form-item label="原价">
                <el-input-number
                  v-model="form.originalPrice"
                  :min="0"
                  :precision="2"
                  :step="1"
                  style="width: 100%"
                />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12">
              <el-form-item label="库存" prop="stock">
                <el-input-number
                  v-model="form.stock"
                  :min="0"
                  :max="999999"
                  style="width: 100%"
                />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12">
              <el-form-item label="计量单位" prop="unit">
                <el-select
                  v-model="form.unit"
                  placeholder="请选择"
                  style="width: 100%"
                >
                  <el-option label="件" value="件" />
                  <el-option label="斤" value="斤" />
                  <el-option label="公斤" value="公斤" />
                  <el-option label="箱" value="箱" />
                  <el-option label="袋" value="袋" />
                  <el-option label="份" value="份" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12">
              <el-form-item label="规格重量">
                <el-input-number
                  v-model="form.weight"
                  :min="0"
                  :precision="2"
                  :step="0.5"
                  style="width: 100%"
                />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12">
              <el-form-item label="产地">
                <el-input
                  v-model="form.origin"
                  placeholder="如：钟山镇"
                  maxlength="100"
                />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12">
              <el-form-item label="扶贫产品">
                <el-switch
                  v-model="form.isPoverty"
                  :active-value="1"
                  :inactive-value="0"
                />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12">
              <el-form-item label="上架状态">
                <el-switch
                  v-model="form.shelfStatus"
                  :active-value="1"
                  :inactive-value="0"
                />
              </el-form-item>
            </el-col>
            <el-col :span="24">
              <el-form-item label="商品图集">
                <el-input
                  v-model="imagesText"
                  type="textarea"
                  :rows="3"
                  placeholder="每行一张图片 URL，最多 9 张"
                />
                <div class="form-tip">
                  每行填写一张图片地址，留空则不上传图集
                </div>
              </el-form-item>
            </el-col>
            <el-col :span="24">
              <el-form-item label="商品描述">
                <el-input
                  v-model="form.description"
                  type="textarea"
                  :rows="4"
                  maxlength="1000"
                  show-word-limit
                  placeholder="介绍种植方式、品质特点、食用方法等"
                />
              </el-form-item>
            </el-col>
          </el-row>
        </el-form>

        <template #footer>
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="submitting" @click="handleSubmit">
            {{ isEdit ? "保存修改" : "确认上架" }}
          </el-button>
        </template>
      </el-dialog>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { ElMessage, ElMessageBox } from "element-plus";
import { Plus } from "@element-plus/icons-vue";
import {
  createProduct,
  deleteProduct,
  getCategories,
  getMyProducts,
  shelfProduct,
  updateProduct,
} from "../api/product";
import { useUserStore } from "../store/user";
import { money } from "../utils/format";

const router = useRouter();
const store = useUserStore();

const loading = ref(false);
const submitting = ref(false);
const list = ref([]);
const total = ref(0);
const categories = ref([]);
const dialogVisible = ref(false);
const formRef = ref();
const isEdit = ref(false);
const editingId = ref(null);
const imagesText = ref("");

/** 统一补齐 value 字段，兼容 el-tree-select 的取值约定 */
function toTreeOptions(nodes) {
  return nodes.map((node) => ({
    id: node.id,
    value: node.id,
    name: node.name,
    children:
      node.children && node.children.length
        ? toTreeOptions(node.children)
        : undefined,
  }));
}

const categoryOptions = computed(() => toTreeOptions(categories.value));

/**
 * 上架/编辑时只允许选末级分类：商品挂在一级会导致其二级分类筛选不到商品。
 * 查询条件仍可按一级筛选（后端会自动包含子分类）。
 */
const formCategoryProps = {
  value: "value",
  label: "name",
  children: "children",
  disabled: (data) => Array.isArray(data.children) && data.children.length > 0,
};

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: "",
  categoryId: "",
});

const emptyForm = () => ({
  categoryId: "",
  name: "",
  cover: "",
  price: undefined,
  originalPrice: undefined,
  stock: 0,
  unit: "件",
  weight: undefined,
  origin: "",
  isPoverty: 1,
  description: "",
  shelfStatus: 1,
});

const form = reactive(emptyForm());

const rules = {
  name: [{ required: true, message: "请输入商品名称", trigger: "blur" }],
  categoryId: [
    { required: true, message: "请选择商品分类", trigger: "change" },
  ],
  cover: [{ required: true, message: "请输入封面图地址", trigger: "blur" }],
  price: [{ required: true, message: "请输入售价", trigger: "blur" }],
  stock: [{ required: true, message: "请输入库存", trigger: "blur" }],
  unit: [{ required: true, message: "请选择计量单位", trigger: "change" }],
};

const isFarmer = computed(() =>
  (store.state.user?.roles || []).includes("FARMER"),
);

/** 店铺名，明确商品归属哪家店 */
const shopName = computed(() => (isFarmer.value ? store.state.user?.shopName || "" : ""));

async function loadCategories() {
  try {
    categories.value = await getCategories({ onlyEnabled: true });
  } catch {
    categories.value = [];
  }
}

async function loadData() {
  loading.value = true;
  try {
    const params = { ...query };
    if (!params.categoryId) delete params.categoryId;
    const page = await getMyProducts(params);
    list.value = page.list || [];
    total.value = page.total || 0;
  } catch {
    list.value = [];
    total.value = 0;
  } finally {
    loading.value = false;
  }
}

function handleSearch() {
  query.pageNum = 1;
  loadData();
}

function handleReset() {
  query.keyword = "";
  query.categoryId = "";
  query.pageNum = 1;
  loadData();
}

function buildImages() {
  return imagesText.value
    .split("\n")
    .map((s) => s.trim())
    .filter(Boolean)
    .slice(0, 9);
}

function openCreate() {
  isEdit.value = false;
  editingId.value = null;
  Object.assign(form, emptyForm());
  imagesText.value = "";
  dialogVisible.value = true;
}

async function openEdit(row) {
  isEdit.value = true;
  editingId.value = row.id;
  Object.assign(form, {
    categoryId: row.categoryId || "",
    name: row.name || "",
    cover: row.cover || "",
    price: row.price,
    originalPrice: row.originalPrice,
    stock: row.stock ?? 0,
    unit: row.unit || "件",
    weight: row.weight,
    origin: row.origin || "",
    isPoverty: row.isPoverty ?? 0,
    description: "",
    shelfStatus: row.shelfStatus ?? 1,
  });
  imagesText.value = "";
  dialogVisible.value = true;
  await loadDetail(row);
}

async function loadDetail(row) {
  try {
    const detail = await getMyProduct(row.id);
    form.description = detail.description || "";
    imagesText.value = (detail.images || []).join("\n");
  } catch {
    // 详情加载失败时保留列表中的基础信息
  }
}

async function handleSubmit() {
  try {
    await formRef.value.validate();
  } catch {
    return;
  }
  const payload = { ...form, images: buildImages() };
  submitting.value = true;
  try {
    if (isEdit.value) {
      await updateProduct(editingId.value, payload);
      ElMessage.success("商品已更新");
    } else {
      await createProduct(payload);
      ElMessage.success("商品已上架");
    }
    dialogVisible.value = false;
    await loadData();
  } finally {
    submitting.value = false;
  }
}

async function handleShelf(row, val) {
  const action = val === 1 ? "上架" : "下架";
  try {
    await ElMessageBox.confirm(`确定${action}商品「${row.name}」吗？`, "提示", {
      type: "warning",
    });
  } catch {
    await loadData();
    return;
  }
  try {
    await shelfProduct(row.id, val);
    ElMessage.success(`已${action}`);
  } finally {
    await loadData();
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确定删除商品「${row.name}」吗？`, "提示", {
      type: "warning",
    });
  } catch {
    return;
  }
  try {
    await deleteProduct(row.id);
    ElMessage.success("商品已删除");
  } finally {
    await loadData();
  }
}

function resetForm() {
  Object.assign(form, emptyForm());
  imagesText.value = "";
  formRef.value?.clearValidate();
}

onMounted(async () => {
  if (store.isLoggedIn()) {
    try {
      await store.loadUser();
    } catch {
      // 登录态失效时由 http 拦截器跳转登录页
    }
  }
  await loadCategories();
  if (isFarmer.value) {
    await loadData();
  }
});
</script>

<style scoped>
.merchant-page {
  max-width: 1100px;
  margin: 0 auto;
  padding: 32px 20px 48px;
}

.page-hero {
  margin-bottom: 20px;
}

.page-hero h1 {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 0 0 8px;
  font-size: 26px;
  color: #1f3d2b;
}

.hero-shop {
  padding: 2px 10px;
  color: #1f7a47;
  font-size: 12px;
  font-weight: 400;
  background: #e6f4ec;
  border: 1px solid #c9e5d5;
  border-radius: 999px;
}

.page-hero p {
  margin: 0;
  color: #5d7668;
}

.role-alert {
  margin-bottom: 16px;
  border-radius: 10px;
}

.alert-link {
  color: #2e9e5b;
  font-weight: 600;
}

.filter-card,
.table-card {
  border-radius: 12px;
}

.filter-card {
  margin-bottom: 16px;
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 10px;
}

.title-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.thumb {
  width: 48px;
  height: 48px;
  border-radius: 6px;
  flex-shrink: 0;
}

.t-title {
  font-weight: 600;
  color: #1f3d2b;
}

.t-sub {
  font-size: 12px;
  color: #8a9d92;
}

.t-price {
  color: #e05b4c;
  font-weight: 600;
}

.t-origin {
  font-size: 12px;
  color: #b0b8b3;
  text-decoration: line-through;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.form-tip {
  font-size: 12px;
  color: #8a9d92;
  margin-left: 6px;
}
</style>
