<template>
  <div class="app-container">
    <el-card shadow="never" class="mb-4">
      <el-form inline @submit.prevent>
        <el-form-item label="审核状态">
          <el-select v-model="queryParams.auditStatus" placeholder="全部" clearable style="width: 140px">
            <el-option label="待审核" :value="0" />
            <el-option label="已通过" :value="1" />
            <el-option label="已驳回" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键字">
          <el-input
            v-model="queryParams.keyword"
            placeholder="项目名称 / 地址"
            clearable
            style="width: 240px"
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="search" @click="handleQuery">查询</el-button>
          <el-button icon="refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="projectList" border stripe>
        <el-table-column label="项目" min-width="220">
          <template #default="{ row }">
            <div class="flex items-center gap-2">
              <el-image
                v-if="row.cover"
                :src="row.cover"
                :preview-src-list="[row.cover]"
                fit="cover"
                class="h-10 w-10 rounded"
                preview-teleported
              />
              <div>
                <div class="font-medium">{{ row.title }}</div>
                <div class="text-xs text-[--el-text-color-secondary]">{{ row.projectType }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="商家" min-width="140">
          <template #default="{ row }">
            <div>{{ row.farmerName || "-" }}</div>
            <div class="text-xs text-[--el-text-color-secondary]">{{ row.shopName || "-" }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="address" label="地址" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">{{ row.address || "-" }}</template>
        </el-table-column>
        <el-table-column prop="capacity" label="接待人数" width="100" align="center">
          <template #default="{ row }">{{ row.capacity ?? "-" }}</template>
        </el-table-column>
        <el-table-column prop="contactPhone" label="联系电话" min-width="130">
          <template #default="{ row }">{{ row.contactPhone || "-" }}</template>
        </el-table-column>
        <el-table-column label="审核状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="auditStatusType(row.auditStatus)" effect="plain">
              {{ auditStatusText(row.auditStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="上架状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.shelfStatus === 1 ? 'success' : 'info'" effect="plain">
              {{ row.shelfStatus === 1 ? "已上架" : "已下架" }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="viewCount" label="浏览量" width="90" align="center">
          <template #default="{ row }">{{ row.viewCount ?? 0 }}</template>
        </el-table-column>
        <el-table-column prop="createTime" label="提交时间" min-width="160" />
        <el-table-column label="操作" width="150" align="center" fixed="right">
          <template #default="{ row }">
            <el-button v-hasPerm="['project:audit']" type="primary" link @click="openDetail(row)">
              详情
            </el-button>
            <el-button
              v-if="row.auditStatus === 0"
              v-hasPerm="['project:audit']"
              type="success"
              link
              @click="openAudit(row)"
            >
              审核
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="mt-4 flex justify-end">
        <el-pagination
          v-model:current-page="queryParams.pageNum"
          v-model:page-size="queryParams.pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          background
          @size-change="loadData"
          @current-change="loadData"
        />
      </div>
    </el-card>

    <el-dialog v-model="detailVisible" title="农家乐项目详情" width="820px" top="6vh">
      <el-descriptions v-if="currentProject" :column="2" border>
        <el-descriptions-item label="项目名称">{{ currentProject.title }}</el-descriptions-item>
        <el-descriptions-item label="项目类型">{{ currentProject.projectType }}</el-descriptions-item>
        <el-descriptions-item label="商家姓名">{{ currentProject.farmerName || "-" }}</el-descriptions-item>
        <el-descriptions-item label="店铺名称">{{ currentProject.shopName || "-" }}</el-descriptions-item>
        <el-descriptions-item label="项目地址" :span="2">{{ currentProject.address || "-" }}</el-descriptions-item>
        <el-descriptions-item label="可接待人数">{{ currentProject.capacity ?? "-" }}</el-descriptions-item>
        <el-descriptions-item label="联系电话">{{ currentProject.contactPhone || "-" }}</el-descriptions-item>
        <el-descriptions-item label="营业时间">{{ currentProject.businessHours || "-" }}</el-descriptions-item>
        <el-descriptions-item label="价格说明">{{ currentProject.priceDesc || "-" }}</el-descriptions-item>
        <el-descriptions-item label="浏览量">{{ currentProject.viewCount ?? 0 }}</el-descriptions-item>
        <el-descriptions-item label="提交时间">{{ currentProject.createTime || "-" }}</el-descriptions-item>
        <el-descriptions-item label="审核状态">
          <el-tag :type="auditStatusType(currentProject.auditStatus)" effect="plain">
            {{ auditStatusText(currentProject.auditStatus) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="上架状态">
          <el-tag :type="currentProject.shelfStatus === 1 ? 'success' : 'info'" effect="plain">
            {{ currentProject.shelfStatus === 1 ? "已上架" : "已下架" }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="审核意见" :span="2">{{ currentProject.auditRemark || "-" }}</el-descriptions-item>
        <el-descriptions-item label="项目介绍" :span="2">{{ currentProject.intro || "-" }}</el-descriptions-item>
        <el-descriptions-item label="封面图" :span="2">
          <el-image
            v-if="currentProject.cover"
            :src="currentProject.cover"
            :preview-src-list="imageList"
            fit="cover"
            class="h-32 w-48 rounded"
            preview-teleported
          />
          <span v-else>-</span>
        </el-descriptions-item>
        <el-descriptions-item v-if="currentProject.images?.length" label="环境图集" :span="2">
          <div class="flex flex-wrap gap-2">
            <el-image
              v-for="(img, idx) in currentProject.images"
              :key="idx"
              :src="img"
              :preview-src-list="imageList"
              :initial-index="idx"
              fit="cover"
              class="h-24 w-24 rounded"
              preview-teleported
            />
          </div>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <el-dialog v-model="auditVisible" title="审核农家乐项目" width="560px" top="8vh">
      <el-descriptions v-if="currentProject" :column="2" border size="small" class="mb-4">
        <el-descriptions-item label="项目名称" :span="2">{{ currentProject.title }}</el-descriptions-item>
        <el-descriptions-item label="商家">{{ currentProject.farmerName || "-" }}</el-descriptions-item>
        <el-descriptions-item label="店铺">{{ currentProject.shopName || "-" }}</el-descriptions-item>
      </el-descriptions>

      <el-form label-width="90px">
        <el-form-item label="审核结果" required>
          <el-radio-group v-model="auditForm.auditStatus">
            <el-radio :value="1">通过</el-radio>
            <el-radio :value="2">驳回</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="审核意见">
          <el-input
            v-model="auditForm.auditRemark"
            type="textarea"
            :rows="4"
            maxlength="255"
            show-word-limit
            :placeholder="auditForm.auditStatus === 1 ? '选填' : '驳回时请填写原因'"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="auditVisible = false">取消</el-button>
        <el-button type="primary" :loading="auditLoading" @click="handleAudit">提交审核</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import ProjectAPI, {
  type ProjectItem,
  type ProjectQueryParams,
} from "@/api/project";

defineOptions({ name: "Project" });

const loading = ref(false);
const auditLoading = ref(false);
const projectList = ref<ProjectItem[]>([]);
const total = ref(0);

const queryParams = reactive<ProjectQueryParams>({
  pageNum: 1,
  pageSize: 10,
  auditStatus: undefined,
  keyword: "",
});

const detailVisible = ref(false);
const auditVisible = ref(false);
const currentProject = ref<ProjectItem>();
const auditForm = reactive({ auditStatus: 1, auditRemark: "" });

const imageList = computed(() => {
  const project = currentProject.value;
  if (!project) return [];
  return [project.cover, ...(project.images ?? [])].filter((url): url is string => Boolean(url));
});

function auditStatusType(status: number) {
  if (status === 1) return "success";
  if (status === 2) return "danger";
  return "warning";
}

function auditStatusText(status: number) {
  if (status === 1) return "已通过";
  if (status === 2) return "已驳回";
  return "待审核";
}

async function loadData() {
  loading.value = true;
  try {
    const page = await ProjectAPI.getAdminPage(queryParams);
    projectList.value = page.list;
    total.value = page.total;
  } finally {
    loading.value = false;
  }
}

function handleQuery() {
  queryParams.pageNum = 1;
  loadData();
}

function handleReset() {
  queryParams.auditStatus = undefined;
  queryParams.keyword = "";
  queryParams.pageNum = 1;
  loadData();
}

function openDetail(row: ProjectItem) {
  currentProject.value = row;
  detailVisible.value = true;
}

function openAudit(row: ProjectItem) {
  currentProject.value = row;
  auditForm.auditStatus = 1;
  auditForm.auditRemark = "";
  auditVisible.value = true;
}

async function handleAudit() {
  if (!currentProject.value) return;
  if (auditForm.auditStatus === 2 && !auditForm.auditRemark.trim()) {
    ElMessage.warning("驳回时请填写审核意见");
    return;
  }
  auditLoading.value = true;
  try {
    await ProjectAPI.audit(currentProject.value.id, { ...auditForm });
    ElMessage.success(auditForm.auditStatus === 1 ? "已通过，项目将在前台展示" : "已驳回");
    auditVisible.value = false;
    await loadData();
  } finally {
    auditLoading.value = false;
  }
}

onMounted(loadData);
</script>
