<template>
  <div class="join-page">
    <section class="join-hero">
      <h1>加入我们</h1>
      <p>提交贫困户资料，审核通过后即可开通农户商家能力，上架自家农产品。</p>
    </section>

    <el-alert
      v-if="isFarmer"
      type="success"
      :closable="false"
      show-icon
      title="您已是农户商家，无需重复提交入驻申请。"
      class="join-alert"
    />

    <el-card v-else shadow="never" class="apply-card">
      <template #header>
        <div class="card-title">
          <span>入驻申请</span>
          <span class="card-tip">带 * 为必填项，提交后由管理员审核</span>
        </div>
      </template>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="100px"
        label-position="left"
      >
        <el-row :gutter="18">
          <el-col :xs="24" :sm="12">
            <el-form-item label="真实姓名" prop="realName">
              <el-input v-model="form.realName" placeholder="请输入真实姓名" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="联系电话" prop="phone">
              <el-input v-model="form.phone" placeholder="请输入手机号" maxlength="11" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="身份证号" prop="idCard">
              <el-input v-model="form.idCard" placeholder="请输入 18 位身份证号" maxlength="18" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="所属村" prop="village">
              <el-select v-model="form.village" placeholder="请选择所属村" style="width: 100%">
                <el-option v-for="v in villages" :key="v" :label="v" :value="v" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="贫困属性" prop="povertyLevel">
              <el-select v-model="form.povertyLevel" placeholder="请选择贫困属性" style="width: 100%">
                <el-option v-for="p in povertyLevels" :key="p" :label="p" :value="p" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="家庭人口" prop="householdNum">
              <el-input-number v-model="form.householdNum" :min="1" :max="30" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="贫困编号" prop="povertyCardNo">
              <el-input v-model="form.povertyCardNo" placeholder="选填，建档立卡编号" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="拟经营品类" prop="mainProduct">
              <el-input v-model="form.mainProduct" placeholder="如：茶叶、蜂蜜、土鸡蛋" maxlength="120" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="证件材料">
              <div class="proof-uploads">
                <div class="proof-upload">
                  <span class="proof-upload__label">身份证正面</span>
                  <SingleImageUpload v-model="form.idCardFront" biz-type="idcard" />
                </div>
                <div class="proof-upload">
                  <span class="proof-upload__label">身份证反面</span>
                  <SingleImageUpload v-model="form.idCardBack" biz-type="idcard" />
                </div>
                <div class="proof-upload">
                  <span class="proof-upload__label">贫困证明 <i>*</i></span>
                  <SingleImageUpload v-model="form.povertyProof" biz-type="proof" />
                </div>
              </div>
              <div class="proof-uploads__tip">
                支持 jpg / jpeg / png，单张不超过 5MB；贫困证明用于管理员审核，必须上传
              </div>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="申请说明" prop="applyDesc">
              <el-input
                v-model="form.applyDesc"
                type="textarea"
                :rows="4"
                maxlength="500"
                show-word-limit
                placeholder="请简要说明家庭情况与拟经营的农产品"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <div class="form-actions">
          <el-button type="primary" :loading="submitting" @click="handleSubmit">提交申请</el-button>
          <el-button @click="resetForm">重置</el-button>
        </div>
      </el-form>
    </el-card>

    <el-card shadow="never" class="history-card">
      <template #header>
        <div class="card-title">
          <span>我的申请记录</span>
          <el-button text :loading="historyLoading" @click="loadHistory">刷新</el-button>
        </div>
      </template>

      <el-empty v-if="!historyLoading && !applications.length" description="暂无入驻申请" />
      <el-table v-else v-loading="historyLoading" :data="applications" border stripe>
        <el-table-column prop="applyNo" label="申请编号" min-width="170" />
        <el-table-column prop="realName" label="真实姓名" min-width="100" />
        <el-table-column prop="village" label="所属村" min-width="110" />
        <el-table-column prop="mainProduct" label="拟经营品类" min-width="140" show-overflow-tooltip />
        <el-table-column label="审核状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="statusType(row.auditStatus)" effect="plain">
              {{ row.auditStatusText }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="提交时间" min-width="160" />
        <el-table-column prop="auditRemark" label="审核意见" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">{{ row.auditRemark || "-" }}</template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import { getMyFarmerApplies, submitFarmerApply } from "../api/farmer";
import { useUserStore } from "../store/user";
import SingleImageUpload from "../components/SingleImageUpload.vue";

const store = useUserStore();
const formRef = ref();
const submitting = ref(false);
const historyLoading = ref(false);
const applications = ref([]);

const villages = ["钟山村委会", "湖亭村", "西林村", "其他村"];
const povertyLevels = ["建档立卡贫困户", "低保户", "特困供养户", "一般贫困户"];

const emptyForm = () => ({
  realName: "",
  phone: "",
  idCard: "",
  village: "",
  povertyLevel: "",
  householdNum: 1,
  povertyCardNo: "",
  mainProduct: "",
  idCardFront: "",
  idCardBack: "",
  povertyProof: "",
  applyDesc: ""
});

const form = reactive(emptyForm());

const rules = {
  realName: [{ required: true, message: "请输入真实姓名", trigger: "blur" }],
  phone: [
    { required: true, message: "请输入手机号", trigger: "blur" },
    { pattern: /^1[3-9]\d{9}$/, message: "手机号格式不正确", trigger: "blur" }
  ],
  idCard: [
    { required: true, message: "请输入身份证号", trigger: "blur" },
    { pattern: /^\d{17}[\dXx]$/, message: "身份证号格式不正确", trigger: "blur" }
  ],
  village: [{ required: true, message: "请选择所属村", trigger: "change" }],
  povertyLevel: [{ required: true, message: "请选择贫困属性", trigger: "change" }],
  householdNum: [{ required: true, message: "请输入家庭人口数", trigger: "change" }],
  povertyProof: [{ required: true, message: "请上传贫困证明", trigger: "change" }]
};

const isFarmer = computed(() => (store.state.user?.roles || []).includes("FARMER"));

function statusType(status) {
  if (status === 1) return "success";
  if (status === 2) return "danger";
  return "warning";
}

async function loadHistory() {
  historyLoading.value = true;
  try {
    applications.value = await getMyFarmerApplies();
  } catch {
    applications.value = [];
  } finally {
    historyLoading.value = false;
  }
}

async function handleSubmit() {
  try {
    await formRef.value.validate();
  } catch {
    return;
  }
  submitting.value = true;
  try {
    await submitFarmerApply({ ...form });
    ElMessage.success("入驻申请已提交，请等待管理员审核");
    resetForm();
    await loadHistory();
  } finally {
    submitting.value = false;
  }
}

function resetForm() {
  Object.assign(form, emptyForm());
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
  await loadHistory();
});
</script>

<style scoped>
.join-page {
  max-width: 1000px;
  margin: 0 auto;
  padding: 32px 20px 48px;
}

.join-hero {
  text-align: center;
  margin-bottom: 24px;
}

.join-hero h1 {
  margin: 0 0 10px;
  font-size: 28px;
  color: #1f3d2b;
}

.join-hero p {
  margin: 0;
  color: #5d7668;
}

.join-alert {
  margin-bottom: 18px;
}

.apply-card,
.history-card {
  border-radius: 12px;
}

.history-card {
  margin-top: 20px;
}

.card-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-weight: 600;
  color: #1f3d2b;
}

.card-tip {
  font-size: 12px;
  font-weight: 400;
  color: #8a9d92;
}

.proof-uploads {
  display: flex;
  flex-wrap: wrap;
  gap: 24px;
}

.proof-upload {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.proof-upload__label {
  font-size: 12px;
  color: #5d7668;
}

.proof-upload__label i {
  color: var(--el-color-danger);
  font-style: normal;
}

.proof-uploads__tip {
  margin-top: 4px;
  font-size: 12px;
  line-height: 1.5;
  color: #8a9d92;
}

.form-actions {
  display: flex;
  justify-content: center;
  gap: 12px;
  margin-top: 8px;
}
</style>
