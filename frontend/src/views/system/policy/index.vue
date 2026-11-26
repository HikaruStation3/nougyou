<template>
  <div class="app-container">
    <div class="filter-section">
      <el-form ref="queryFormRef" :model="queryParams" :inline="true" label-suffix=":">
        <el-form-item label="标题/文号" prop="keyword">
          <el-input
            v-model="queryParams.keyword"
            placeholder="政策标题或文号"
            clearable
            @keyup.enter="handleQuery()"
          />
        </el-form-item>

        <el-form-item label="发布状态" prop="publishStatus">
          <el-select
            v-model="queryParams.publishStatus"
            clearable
            placeholder="全部"
            style="width: 110px"
          >
            <el-option :value="0" label="草稿" />
            <el-option :value="1" label="已发布" />
            <el-option :value="2" label="已下架" />
          </el-select>
        </el-form-item>

        <el-form-item class="search-buttons">
          <el-button type="primary" icon="search" @click="handleQuery()">搜索</el-button>
          <el-button icon="refresh" @click="handleResetQuery()">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <el-card shadow="hover" class="table-section">
      <div class="table-section__toolbar">
        <div class="table-section__toolbar--actions">
          <el-button
            v-hasPerm="['cms:policy:create']"
            type="success"
            icon="plus"
            @click="openDialog()"
          >
            新增政策
          </el-button>
          <el-button
            v-hasPerm="['cms:policy:delete']"
            type="danger"
            :disabled="selectIds.length === 0"
            icon="delete"
            @click="handleDelete()"
          >
            删除
          </el-button>
        </div>
      </div>

      <el-table
        v-loading="loading"
        :data="pageData"
        highlight-current-row
        class="table-section__content"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" align="center" />
        <el-table-column type="index" label="序号" width="60" />
        <el-table-column label="政策标题" prop="title" min-width="240" show-overflow-tooltip />
        <el-table-column label="文号" prop="policyNo" width="170" show-overflow-tooltip />
        <el-table-column label="发布单位" prop="issueDept" width="150" show-overflow-tooltip />
        <el-table-column align="center" label="状态" width="100">
          <template #default="scope">
            <el-tag v-if="scope.row.publishStatus === 0" type="info">草稿</el-tag>
            <el-tag v-else-if="scope.row.publishStatus === 1" type="success">已发布</el-tag>
            <el-tag v-else type="warning">已下架</el-tag>
          </template>
        </el-table-column>
        <el-table-column align="center" label="发布时间" prop="publishTime" width="170" />
        <el-table-column align="center" label="浏览量" prop="viewCount" width="90" />
        <el-table-column align="center" fixed="right" label="操作" width="200">
          <template #default="scope">
            <el-button
              v-if="scope.row.publishStatus !== 1"
              v-hasPerm="['cms:policy:publish']"
              type="primary"
              size="small"
              link
              @click="handlePublish(scope.row.id, 1)"
            >
              发布
            </el-button>
            <el-button
              v-else
              v-hasPerm="['cms:policy:publish']"
              type="warning"
              size="small"
              link
              @click="handlePublish(scope.row.id, 2)"
            >
              下架
            </el-button>
            <el-button
              v-hasPerm="['cms:policy:update']"
              type="primary"
              size="small"
              link
              @click="openDialog(scope.row.id)"
            >
              编辑
            </el-button>
            <el-button
              v-hasPerm="['cms:policy:delete']"
              type="danger"
              size="small"
              link
              @click="handleDelete(scope.row.id)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <pagination
        v-if="total > 0"
        v-model:total="total"
        v-model:page="queryParams.pageNum"
        v-model:limit="queryParams.pageSize"
        @pagination="fetchData()"
      />
    </el-card>

    <el-dialog
      v-model="dialogState.visible"
      :show-close="false"
      top="6vh"
      width="70%"
      @close="closeDialog"
    >
      <template #header>
        <div class="flex-x-between">
          <span>{{ dialogState.title }}</span>
          <el-button circle @click="closeDialog">
            <template #icon>
              <Close />
            </template>
          </el-button>
        </div>
      </template>

      <el-form ref="dataFormRef" :model="formData" :rules="rules" label-width="100px">
        <el-form-item label="政策标题" prop="title">
          <el-input v-model="formData.title" placeholder="政策标题" clearable />
        </el-form-item>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="政策文号" prop="policyNo">
              <el-input v-model="formData.policyNo" placeholder="如 钟政发〔2026〕7号" clearable />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="发布单位" prop="issueDept">
              <el-input v-model="formData.issueDept" placeholder="如 钟山镇人民政府" clearable />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="封面地址" prop="cover">
          <el-input v-model="formData.cover" placeholder="选填，如 /img/policy/p1.jpg" clearable />
        </el-form-item>

        <el-form-item label="摘要" prop="summary">
          <el-input
            v-model="formData.summary"
            type="textarea"
            :rows="2"
            maxlength="500"
            show-word-limit
            placeholder="选填，列表页展示的一句话摘要"
          />
        </el-form-item>

        <el-form-item label="政策正文" prop="content">
          <el-input
            v-model="formData.content"
            type="textarea"
            :rows="14"
            placeholder="按纯文本录入，每行一段；门户按换行分段展示，不要粘贴 HTML 标签"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="handleSubmit()">确定</el-button>
          <el-button @click="closeDialog()">取消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
defineOptions({
  name: "PolicyManage",
  inheritAttrs: false,
});

import { PolicyAPI, type PolicyItem, type PolicyForm, type PolicyQueryParams } from "@/api/cms";
import type { FormInstance, FormRules } from "element-plus";

const queryFormRef = ref<FormInstance>();
const dataFormRef = ref<FormInstance>();

// 查询参数
const queryParams = reactive<PolicyQueryParams>({
  pageNum: 1,
  pageSize: 10,
});

// 列表数据
const pageData = ref<PolicyItem[]>([]);
const total = ref(0);
const loading = ref(false);
const selectIds = ref<number[]>([]);

// 弹窗状态
const dialogState = reactive({
  title: "",
  visible: false,
});

// 表单数据
const formData = reactive<PolicyForm>({});

// 验证规则
const rules: FormRules = {
  title: [{ required: true, message: "请输入政策标题", trigger: "blur" }],
  content: [{ required: true, message: "请输入政策正文", trigger: "blur" }],
};

/**
 * 查询按钮点击事件
 */
function handleQuery(): void {
  queryParams.pageNum = 1;
  fetchData();
}

/**
 * 加载政策列表数据
 */
function fetchData(): void {
  loading.value = true;
  PolicyAPI.getPage(queryParams)
    .then((data) => {
      pageData.value = data.list;
      total.value = data.total ?? 0;
    })
    .finally(() => {
      loading.value = false;
    });
}

/**
 * 重置查询
 */
function handleResetQuery(): void {
  queryFormRef.value?.resetFields();
  queryParams.pageNum = 1;
  fetchData();
}

/**
 * 表格选择变化事件
 */
function handleSelectionChange(selection: PolicyItem[]): void {
  selectIds.value = selection.map((item) => Number(item.id));
}

/**
 * 打开弹窗
 * @param id 政策ID（编辑时传入）
 */
function openDialog(id?: string): void {
  dialogState.visible = true;
  if (id) {
    dialogState.title = "修改政策";
    PolicyAPI.getFormData(id).then((data) => {
      Object.assign(formData, data);
    });
  } else {
    dialogState.title = "新增政策";
    Object.assign(formData, {
      id: undefined,
      title: "",
      policyNo: "",
      issueDept: "钟山镇人民政府",
      cover: "",
      summary: "",
      content: "",
    });
  }
}

/**
 * 提交表单
 */
function handleSubmit(): void {
  dataFormRef.value?.validate((valid) => {
    if (!valid) {
      return;
    }
    loading.value = true;
    const id = formData.id;
    const request = id
      ? PolicyAPI.update(id, formData)
      : PolicyAPI.create(formData);
    request
      .then(() => {
        ElMessage.success(id ? "修改成功" : "已保存为草稿，点「发布」后前台可见");
        closeDialog();
        handleResetQuery();
      })
      .finally(() => (loading.value = false));
  });
}

/**
 * 发布 / 下架
 * @param id 政策ID
 * @param status 1 发布，2 下架
 */
function handlePublish(id: string, status: 1 | 2): void {
  if (status === 1) {
    PolicyAPI.publish(id, 1).then(() => {
      ElMessage.success("发布成功，门户政策列表已可查看");
      fetchData();
    });
    return;
  }
  ElMessageBox.confirm("下架后门户将立即看不到这条政策，确认下架？", "提示", {
    confirmButtonText: "确定",
    cancelButtonText: "取消",
    type: "warning",
  }).then(
    () => {
      PolicyAPI.publish(id, 2).then(() => {
        ElMessage.success("已下架");
        fetchData();
      });
    },
    () => {
      ElMessage.info("已取消");
    }
  );
}

/**
 * 删除政策
 * @param id 政策ID
 */
function handleDelete(id?: string): void {
  const deleteIds = [id || selectIds.value].join(",");
  if (!deleteIds) {
    ElMessage.warning("请勾选删除项");
    return;
  }

  ElMessageBox.confirm("确认删除已选中的数据项吗？", "警告", {
    confirmButtonText: "确定",
    cancelButtonText: "取消",
    type: "warning",
  }).then(
    () => {
      loading.value = true;
      PolicyAPI.deleteByIds(deleteIds)
        .then(() => {
          ElMessage.success("删除成功");
          handleResetQuery();
        })
        .finally(() => (loading.value = false));
    },
    () => {
      ElMessage.info("已取消删除");
    }
  );
}

/**
 * 关闭弹窗
 */
function closeDialog(): void {
  dialogState.visible = false;
  dataFormRef.value?.clearValidate();
  formData.id = undefined;
}

onMounted(() => {
  handleQuery();
});
</script>
