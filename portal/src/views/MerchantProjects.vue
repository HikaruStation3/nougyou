<template>
  <div class="merchant-page">
    <section class="page-hero">
      <h1>我的农家乐项目</h1>
      <p>管理您提交的农家乐项目，提交后由管理员审核，通过后即可在前台展示。</p>
    </section>

    <template v-if="!isFarmer">
      <el-alert
        type="warning"
        :closable="false"
        show-icon
        title="仅农户商家可管理农家乐项目"
        class="role-alert"
      >
        <template #default>
          您当前还不是农户商家，请先到
          <router-link to="/join" class="alert-link">「加入我们」</router-link>
          提交入驻申请，审核通过后即可在此管理项目。
        </template>
      </el-alert>
    </template>

    <template v-else>
      <el-card shadow="never" class="filter-card">
        <div class="toolbar">
          <el-form inline @submit.prevent>
            <el-form-item label="审核状态">
              <el-select v-model="query.auditStatus" placeholder="全部" clearable style="width: 140px">
                <el-option label="待审核" :value="0" />
                <el-option label="已通过" :value="1" />
                <el-option label="已驳回" :value="2" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" icon="search" @click="handleSearch">查询</el-button>
              <el-button icon="refresh" @click="handleReset">重置</el-button>
            </el-form-item>
          </el-form>
          <el-button type="success" icon="Plus" @click="openCreate">申请农家乐项目</el-button>
        </div>
      </el-card>

      <el-card shadow="never" class="table-card">
        <el-table v-loading="loading" :data="list" border stripe>
          <el-table-column label="项目" min-width="240">
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
                  <div class="t-title">{{ row.title }}</div>
                  <div class="t-sub">{{ row.address || "-" }}</div>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="审核状态" width="110" align="center">
            <template #default="{ row }">
              <el-tag :type="auditType(row.auditStatus)" effect="plain">
                {{ auditText(row.auditStatus) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="上架状态" width="110" align="center">
            <template #default="{ row }">
              <el-tooltip
                :content="row.auditStatus === 1 ? '' : '仅审核通过的项目可上下架'"
                placement="top"
                :disabled="row.auditStatus === 1"
              >
                <el-switch
                  :model-value="row.shelfStatus"
                  :active-value="1"
                  :inactive-value="0"
                  :disabled="row.auditStatus !== 1"
                  @change="(val) => handleShelf(row, val)"
                />
              </el-tooltip>
            </template>
          </el-table-column>
          <el-table-column prop="viewCount" label="浏览量" width="90" align="center">
            <template #default="{ row }">{{ row.viewCount || 0 }}</template>
          </el-table-column>
          <el-table-column prop="createTime" label="提交时间" min-width="160" />
          <el-table-column label="审核意见" min-width="160" show-overflow-tooltip>
            <template #default="{ row }">
              <span v-if="row.auditRemark" :class="{ reject: row.auditStatus === 2 }">
                {{ row.auditRemark }}
              </span>
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="160" align="center" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" link @click="router.push(`/projects/${row.id}`)">查看</el-button>
              <el-button type="primary" link @click="openEdit(row)">编辑</el-button>
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
        :title="isEdit ? '编辑农家乐项目' : '申请农家乐项目'"
        width="720px"
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
            <el-col :xs="24" :sm="16">
              <el-form-item label="项目名称" prop="title">
                <el-input v-model="form.title" placeholder="5-50 个字符" maxlength="50" show-word-limit />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="8">
              <el-form-item label="项目类型" prop="projectType">
                <el-input v-model="form.projectType" placeholder="农家乐" />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12">
              <el-form-item label="封面图" prop="cover">
                <el-input v-model="form.cover" placeholder="请输入封面图 URL" />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12">
              <el-form-item label="联系电话" prop="contactPhone">
                <el-input v-model="form.contactPhone" placeholder="11 位手机号" maxlength="11" />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="16">
              <el-form-item label="项目地址" prop="address">
                <el-input v-model="form.address" placeholder="详细地址" maxlength="200" />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="8">
              <el-form-item label="可接待人数" prop="capacity">
                <el-input-number v-model="form.capacity" :min="1" :max="500" style="width: 100%" />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12">
              <el-form-item label="营业时间">
                <el-input v-model="form.businessHours" placeholder="如：09:00-21:00" maxlength="50" />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12">
              <el-form-item label="价格说明">
                <el-input v-model="form.priceDesc" placeholder="如：人均 80 元" maxlength="100" />
              </el-form-item>
            </el-col>
            <el-col :span="24">
              <el-form-item label="环境图集">
                <el-input
                  v-model="imagesText"
                  type="textarea"
                  :rows="3"
                  placeholder="每行一张图片 URL，最多 9 张"
                />
                <div class="form-tip">每行填写一张图片地址，留空则不上传图集</div>
              </el-form-item>
            </el-col>
            <el-col :span="24">
              <el-form-item label="项目介绍">
                <el-input
                  v-model="form.intro"
                  type="textarea"
                  :rows="4"
                  maxlength="1000"
                  show-word-limit
                  placeholder="介绍项目特色、餐饮、游玩项目等"
                />
              </el-form-item>
            </el-col>
            <el-col v-if="isEdit" :span="24">
              <el-form-item>
                <el-checkbox v-model="form.resubmit">保存后重新提交审核</el-checkbox>
                <span class="form-tip">（修改被驳回的项目后，勾选此项重新进入审核队列）</span>
              </el-form-item>
            </el-col>
          </el-row>
        </el-form>

        <template #footer>
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="submitting" @click="handleSubmit">
            {{ isEdit ? "保存修改" : "提交申请" }}
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
  createProject,
  getMyProjects,
  shelfProject,
  updateProject
} from "../api/project";
import { useUserStore } from "../store/user";

const router = useRouter();
const store = useUserStore();

const loading = ref(false);
const submitting = ref(false);
const list = ref([]);
const total = ref(0);
const dialogVisible = ref(false);
const formRef = ref();
const isEdit = ref(false);
const editingId = ref(null);
const imagesText = ref("");

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  auditStatus: undefined
});

const emptyForm = () => ({
  title: "",
  projectType: "农家乐",
  cover: "",
  address: "",
  intro: "",
  priceDesc: "",
  capacity: undefined,
  contactPhone: "",
  businessHours: "",
  resubmit: false
});

const form = reactive(emptyForm());

const rules = {
  title: [
    { required: true, message: "请输入项目名称", trigger: "blur" },
    { min: 5, max: 50, message: "项目名称长度需在 5-50 个字符之间", trigger: "blur" }
  ],
  projectType: [{ required: true, message: "请输入项目类型", trigger: "blur" }],
  cover: [{ required: true, message: "请输入封面图地址", trigger: "blur" }],
  address: [{ required: true, message: "请输入项目地址", trigger: "blur" }],
  contactPhone: [
    { required: true, message: "请输入联系电话", trigger: "blur" },
    { pattern: /^1[3-9]\d{9}$/, message: "联系电话格式不正确", trigger: "blur" }
  ]
};

const isFarmer = computed(() => (store.state.user?.roles || []).includes("FARMER"));

function auditType(status) {
  if (status === 1) return "success";
  if (status === 2) return "danger";
  return "warning";
}

function auditText(status) {
  if (status === 1) return "已通过";
  if (status === 2) return "已驳回";
  return "待审核";
}

async function loadData() {
  loading.value = true;
  try {
    const page = await getMyProjects({ ...query });
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
  query.auditStatus = undefined;
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

function openEdit(row) {
  isEdit.value = true;
  editingId.value = row.id;
  Object.assign(form, {
    title: row.title || "",
    projectType: row.projectType || "农家乐",
    cover: row.cover || "",
    address: row.address || "",
    intro: row.intro || "",
    priceDesc: row.priceDesc || "",
    capacity: row.capacity,
    contactPhone: row.contactPhone || "",
    businessHours: row.businessHours || "",
    resubmit: false
  });
  imagesText.value = (row.images || []).join("\n");
  dialogVisible.value = true;
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
      await updateProject(editingId.value, payload);
      ElMessage.success(payload.resubmit ? "已保存并重新提交审核" : "修改已保存");
    } else {
      await createProject(payload);
      ElMessage.success("项目已提交，请等待管理员审核");
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
    await ElMessageBox.confirm(`确定${action}项目「${row.title}」吗？`, "提示", {
      type: "warning"
    });
  } catch {
    loadData();
    return;
  }
  try {
    await shelfProject(row.id, val);
    ElMessage.success(`已${action}`);
    await loadData();
  } catch {
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
  margin: 0 0 8px;
  font-size: 26px;
  color: #1f3d2b;
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

.reject {
  color: #f56c6c;
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
