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
            v-model="queryParams.keywords"
            placeholder="姓名 / 手机号 / 申请编号 / 村"
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
      <el-table v-loading="loading" :data="applyList" border stripe>
        <el-table-column prop="applyNo" label="申请编号" min-width="170" />
        <el-table-column prop="realName" label="申请人" min-width="110">
          <template #default="{ row }">
            <div>{{ row.realName }}</div>
            <div class="text-xs text-[--el-text-color-secondary]">{{ row.username }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="phone" label="手机号" min-width="120" />
        <el-table-column prop="village" label="所属村" min-width="110" />
        <el-table-column prop="povertyLevel" label="贫困属性" min-width="120">
          <template #default="{ row }">{{ row.povertyLevel || "-" }}</template>
        </el-table-column>
        <el-table-column prop="householdNum" label="家庭人口" width="90" align="center" />
        <el-table-column prop="mainProduct" label="拟经营品类" min-width="130" show-overflow-tooltip>
          <template #default="{ row }">{{ row.mainProduct || "-" }}</template>
        </el-table-column>
        <el-table-column label="审核状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="statusType(row.auditStatus)" effect="plain">
              {{ row.auditStatusText }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="提交时间" min-width="160" />
        <el-table-column label="操作" width="150" align="center" fixed="right">
          <template #default="{ row }">
            <el-button v-hasPerm="['farmerApply:detail']" type="primary" link @click="openDetail(row)">
              详情
            </el-button>
            <el-button
              v-if="row.auditStatus === 0"
              v-hasPerm="['farmerApply:audit']"
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

    <el-dialog v-model="detailVisible" title="入驻申请详情" width="760px" top="6vh">
      <el-descriptions v-if="currentApply" :column="2" border>
        <el-descriptions-item label="申请编号">{{ currentApply.applyNo }}</el-descriptions-item>
        <el-descriptions-item label="申请账号">{{ currentApply.username || "-" }}</el-descriptions-item>
        <el-descriptions-item label="真实姓名">{{ currentApply.realName }}</el-descriptions-item>
        <el-descriptions-item label="手机号">{{ currentApply.phone }}</el-descriptions-item>
        <el-descriptions-item label="身份证号">{{ currentApply.idCard }}</el-descriptions-item>
        <el-descriptions-item label="所属村">{{ currentApply.village }}</el-descriptions-item>
        <el-descriptions-item label="贫困属性">{{ currentApply.povertyLevel || "-" }}</el-descriptions-item>
        <el-descriptions-item label="贫困编号">{{ currentApply.povertyCardNo || "-" }}</el-descriptions-item>
        <el-descriptions-item label="家庭人口">{{ currentApply.householdNum ?? "-" }}</el-descriptions-item>
        <el-descriptions-item label="拟经营品类">{{ currentApply.mainProduct || "-" }}</el-descriptions-item>
        <el-descriptions-item label="提交时间">{{ currentApply.createTime || "-" }}</el-descriptions-item>
        <el-descriptions-item label="审核状态">
          <el-tag :type="statusType(currentApply.auditStatus)" effect="plain">
            {{ currentApply.auditStatusText }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="审核人">{{ currentApply.auditUsername || "-" }}</el-descriptions-item>
        <el-descriptions-item label="审核时间">{{ currentApply.auditTime || "-" }}</el-descriptions-item>
        <el-descriptions-item label="审核意见" :span="2">
          {{ currentApply.auditRemark || "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="申请说明" :span="2">
          {{ currentApply.applyDesc || "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="证件材料" :span="2">
          <template v-if="materials.length">
            <el-link
              v-for="item in materials"
              :key="item.url"
              :href="item.url"
              target="_blank"
              type="primary"
              class="mr-3"
            >
              {{ item.label }}
            </el-link>
          </template>
          <span v-else>-</span>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <el-dialog v-model="auditVisible" title="审核入驻申请" width="560px" top="8vh">
      <el-descriptions v-if="currentApply" :column="2" border size="small" class="mb-4">
        <el-descriptions-item label="申请人">{{ currentApply.realName }}</el-descriptions-item>
        <el-descriptions-item label="所属村">{{ currentApply.village }}</el-descriptions-item>
        <el-descriptions-item label="贫困属性">{{ currentApply.povertyLevel || "-" }}</el-descriptions-item>
        <el-descriptions-item label="家庭人口">{{ currentApply.householdNum ?? "-" }}</el-descriptions-item>
      </el-descriptions>

      <el-form label-width="90px">
        <el-form-item label="审核结果" required>
          <el-radio-group v-model="auditForm.approved">
            <el-radio :value="true">通过</el-radio>
            <el-radio :value="false">驳回</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="审核意见">
          <el-input
            v-model="auditForm.remark"
            type="textarea"
            :rows="4"
            maxlength="255"
            show-word-limit
            :placeholder="auditForm.approved ? '选填' : '驳回时请填写原因'"
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
import FarmerApplyAPI, {
  type FarmerApplyItem,
  type FarmerApplyQueryParams,
} from "@/api/farmer";

defineOptions({ name: "FarmerApply" });

const loading = ref(false);
const auditLoading = ref(false);
const applyList = ref<FarmerApplyItem[]>([]);
const total = ref(0);

const queryParams = reactive<FarmerApplyQueryParams>({
  pageNum: 1,
  pageSize: 10,
  auditStatus: undefined,
  keywords: "",
});

const detailVisible = ref(false);
const auditVisible = ref(false);
const currentApply = ref<FarmerApplyItem>();
const auditForm = reactive({ approved: true, remark: "" });

const materials = computed(() => {
  const apply = currentApply.value;
  if (!apply) return [];
  return [
    { label: "身份证正面", url: apply.idCardFront },
    { label: "身份证反面", url: apply.idCardBack },
    { label: "贫困证明", url: apply.povertyProof },
  ].filter((item) => Boolean(item.url)) as { label: string; url: string }[];
});

function statusType(status: number) {
  if (status === 1) return "success";
  if (status === 2) return "danger";
  return "warning";
}

async function loadData() {
  loading.value = true;
  try {
    const page = await FarmerApplyAPI.getPage(queryParams);
    applyList.value = page.list;
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
  queryParams.keywords = "";
  queryParams.pageNum = 1;
  loadData();
}

function openDetail(row: FarmerApplyItem) {
  currentApply.value = row;
  detailVisible.value = true;
}

function openAudit(row: FarmerApplyItem) {
  currentApply.value = row;
  auditForm.approved = true;
  auditForm.remark = "";
  auditVisible.value = true;
}

async function handleAudit() {
  if (!currentApply.value) return;
  if (!auditForm.approved && !auditForm.remark.trim()) {
    ElMessage.warning("驳回时请填写审核意见");
    return;
  }
  auditLoading.value = true;
  try {
    await FarmerApplyAPI.audit(currentApply.value.id, { ...auditForm });
    ElMessage.success(auditForm.approved ? "已通过，该用户已升级为农户商家" : "已驳回");
    auditVisible.value = false;
    await loadData();
  } finally {
    auditLoading.value = false;
  }
}

onMounted(loadData);
</script>
