<template>
  <div class="app-container ai-prompt">
    <!-- 场景切换 -->
    <el-card shadow="never" class="scene-card">
      <el-radio-group v-model="activeScene">
        <el-radio-button v-for="scene in SCENES" :key="scene.code" :value="scene.code">
          {{ scene.label }}
          <el-tag v-if="currentOf(scene.code)" size="small" effect="plain" class="scene-version">
            v{{ currentOf(scene.code)?.version }}
          </el-tag>
        </el-radio-button>
      </el-radio-group>
      <span class="scene-tip">
        提示词决定 AI 在该场景下的角色与输出边界，新增版本后立即生效，旧版本保留可随时回滚
      </span>
    </el-card>

    <!-- 当前生效版本 -->
    <el-card shadow="never" class="current-card">
      <template #header>
        <div class="card-head">
          <div>
            <span class="card-title">当前生效版本</span>
            <el-tag v-if="current" size="small" type="success" effect="dark" class="version-tag">
              v{{ current.version }}
            </el-tag>
          </div>
          <div class="card-actions">
            <el-button
              v-hasPerm="'ai:promptAdd'"
              v-if="current"
              icon="CopyDocument"
              @click="openCreate('clone')"
            >
              基于当前版本新建
            </el-button>
            <el-button
              v-hasPerm="'ai:promptAdd'"
              type="primary"
              icon="Plus"
              @click="openCreate('blank')"
            >
              新增版本
            </el-button>
          </div>
        </div>
      </template>

      <el-skeleton v-if="loading" :rows="4" animated />
      <template v-else-if="current">
        <div class="prompt-meta">
          <span>场景名称：{{ current.sceneName }}</span>
          <span>最后修改：{{ current.updateUserName || "—" }}</span>
          <span>更新时间：{{ current.updateTime }}</span>
          <span>字数：{{ current.promptContent.length }}</span>
        </div>
        <div class="prompt-content">{{ current.promptContent }}</div>
      </template>
      <el-empty v-else description="该场景还没有自定义提示词，当前使用系统内置默认提示词" :image-size="72" />
    </el-card>

    <!-- 版本历史 -->
    <el-card shadow="never" class="table-section">
      <div class="table-section__toolbar">
        <div class="table-section__toolbar--actions">
          <span class="table-title">版本历史</span>
          <span class="table-sub">共 {{ sceneVersions.length }} 个版本，只有一版处于启用状态</span>
        </div>
      </div>

      <el-table v-loading="loading" :data="sceneVersions" border stripe class="table-section__content">
        <el-table-column label="版本" width="90" align="center">
          <template #default="scope">v{{ scope.row.version }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="scope">
            <el-tag :type="scope.row.isEnable === 1 ? 'success' : 'info'" effect="light" size="small">
              {{ scope.row.isEnable === 1 ? "启用中" : "已停用" }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="提示词摘要" prop="promptContent" min-width="320" show-overflow-tooltip />
        <el-table-column label="字数" width="80" align="center">
          <template #default="scope">{{ scope.row.promptContent.length }}</template>
        </el-table-column>
        <el-table-column label="修改人" width="120" align="center">
          <template #default="scope">{{ scope.row.updateUserName || "—" }}</template>
        </el-table-column>
        <el-table-column label="修改时间" prop="updateTime" width="170" align="center" />
        <el-table-column label="操作" width="170" align="center" fixed="right">
          <template #default="scope">
            <el-button type="primary" link icon="View" @click="openView(scope.row)">查看</el-button>
            <el-button
              v-if="scope.row.isEnable !== 1"
              v-hasPerm="'ai:promptEnable'"
              type="warning"
              link
              icon="RefreshLeft"
              @click="handleEnable(scope.row)"
            >
              启用
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 查看版本 -->
    <el-dialog v-model="viewVisible" :title="`版本 v${viewing?.version ?? ''}`" width="680px">
      <div class="prompt-meta">
        <span>场景：{{ viewing?.sceneName }}</span>
        <span>修改人：{{ viewing?.updateUserName || "—" }}</span>
        <span>时间：{{ viewing?.updateTime }}</span>
      </div>
      <div class="prompt-content prompt-content--dialog">{{ viewing?.promptContent }}</div>
    </el-dialog>

    <!-- 新增版本 -->
    <el-dialog v-model="formVisible" :title="`新增提示词版本 · ${sceneLabel(form.sceneCode)}`" width="720px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="场景" prop="sceneCode">
          <el-input :model-value="`${form.sceneCode}（${sceneLabel(form.sceneCode)}）`" disabled />
        </el-form-item>
        <el-form-item label="场景名称" prop="sceneName">
          <el-input v-model="form.sceneName" maxlength="30" show-word-limit placeholder="如：经营业务咨询" />
        </el-form-item>
        <el-form-item label="提示词" prop="promptContent">
          <el-input
            v-model="form.promptContent"
            type="textarea"
            :rows="12"
            resize="vertical"
            maxlength="8000"
            show-word-limit
            placeholder="写明角色定位、可回答范围、禁止事项与输出格式"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSubmit">保存并启用</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from "element-plus";
import AIAPI, { type PromptConfig } from "@/api/ai";

const SCENES = [
  { code: "BIZ_CONSULT", label: "经营咨询", defaultName: "经营业务咨询" },
  { code: "SALE_STAT", label: "数据统计", defaultName: "销售数据统计" },
  { code: "SALE_ANALYSIS", label: "可行性分析", defaultName: "销售可行性分析" },
];

const loading = ref(false);
const saving = ref(false);
const prompts = ref<PromptConfig[]>([]);
const activeScene = ref("BIZ_CONSULT");

const viewVisible = ref(false);
const viewing = ref<PromptConfig | null>(null);
const formVisible = ref(false);
const formRef = ref<FormInstance>();
const form = reactive({
  sceneCode: "BIZ_CONSULT",
  sceneName: "",
  promptContent: "",
});

const rules: FormRules = {
  sceneName: [{ required: true, message: "请输入场景名称", trigger: "blur" }],
  promptContent: [
    { required: true, message: "请输入提示词正文", trigger: "blur" },
    { min: 10, message: "提示词正文至少 10 个字", trigger: "blur" },
  ],
};

const sceneVersions = computed(() =>
  prompts.value
    .filter((item) => item.sceneCode === activeScene.value)
    .slice()
    .sort((a, b) => b.version - a.version)
);

const current = computed(() => sceneVersions.value.find((item) => item.isEnable === 1) ?? null);

function currentOf(sceneCode: string) {
  return prompts.value.find((item) => item.sceneCode === sceneCode && item.isEnable === 1);
}

function sceneLabel(code: string) {
  return SCENES.find((item) => item.code === code)?.label ?? code;
}

async function loadPrompts() {
  loading.value = true;
  try {
    prompts.value = (await AIAPI.listPrompts()) ?? [];
  } catch {
    // 拦截器已提示
  } finally {
    loading.value = false;
  }
}

function openView(row: any) {
  viewing.value = row as PromptConfig;
  viewVisible.value = true;
}

function openCreate(mode: "blank" | "clone") {
  const scene = SCENES.find((item) => item.code === activeScene.value);
  form.sceneCode = activeScene.value;
  form.sceneName = current.value?.sceneName || scene?.defaultName || "";
  form.promptContent = mode === "clone" ? current.value?.promptContent ?? "" : "";
  formVisible.value = true;
  formRef.value?.clearValidate();
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid) return;

  saving.value = true;
  try {
    const saved = await AIAPI.createPrompt({
      sceneCode: form.sceneCode,
      sceneName: form.sceneName,
      promptContent: form.promptContent,
    });
    ElMessage.success(`已发布 v${saved.version} 并启用`);
    formVisible.value = false;
    await loadPrompts();
  } catch {
    // 拦截器已提示
  } finally {
    saving.value = false;
  }
}

async function handleEnable(row: any) {
  try {
    await ElMessageBox.confirm(
      `确认回滚到 v${row.version} 吗？启用后当前版本会自动停用，AI 立即使用该版提示词。`,
      "启用历史版本",
      { type: "warning", confirmButtonText: "确认启用", cancelButtonText: "取消" }
    );
  } catch {
    return;
  }
  try {
    await AIAPI.enablePrompt(row.id);
    ElMessage.success(`已启用 v${row.version}`);
    await loadPrompts();
  } catch {
    // 拦截器已提示
  }
}

onMounted(loadPrompts);
</script>

<style lang="scss" scoped>
.ai-prompt {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.scene-card {
  :deep(.el-card__body) {
    display: flex;
    align-items: center;
    gap: 16px;
    flex-wrap: wrap;
  }

  .scene-version {
    margin-left: 6px;
  }

  .scene-tip {
    font-size: 12px;
    color: var(--el-text-color-secondary);
  }
}

.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.card-title {
  font-weight: 600;
}

.version-tag {
  margin-left: 8px;
}

.prompt-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 18px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-bottom: 12px;
}

.prompt-content {
  white-space: pre-wrap;
  line-height: 1.8;
  font-size: 14px;
  padding: 14px 16px;
  border-radius: 8px;
  background: var(--el-fill-color-lighter);
  border: 1px solid var(--el-border-color-lighter);
  color: var(--el-text-color-primary);

  &--dialog {
    max-height: 52vh;
    overflow: auto;
  }
}

.table-title {
  font-weight: 600;
  margin-right: 10px;
}

.table-sub {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
</style>
