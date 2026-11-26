<template>
  <div class="app-container">
    <div class="filter-section">
      <el-form ref="queryFormRef" :model="queryParams" :inline="true" label-suffix=":">
        <el-form-item label="标题/摘要" prop="keyword">
          <el-input
            v-model="queryParams.keyword"
            placeholder="资讯标题或摘要"
            clearable
            @keyup.enter="handleQuery()"
          />
        </el-form-item>

        <el-form-item label="栏目" prop="newsType">
          <el-select v-model="queryParams.newsType" clearable placeholder="全部" style="width: 130px">
            <el-option
              v-for="item in NEWS_TYPES"
              :key="item.value"
              :value="item.value"
              :label="item.label"
            />
          </el-select>
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
            v-hasPerm="['cms:news:create']"
            type="success"
            icon="plus"
            @click="openDialog()"
          >
            新增资讯
          </el-button>
          <el-button
            v-hasPerm="['cms:news:delete']"
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
        <el-table-column label="资讯标题" prop="title" min-width="240" show-overflow-tooltip />
        <el-table-column align="center" label="栏目" width="110">
          <template #default="scope">
            <el-tag :type="TYPE_TAG[scope.row.newsType] || 'info'" effect="plain">
              {{ scope.row.newsTypeName }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="关联地点" prop="location" width="130" show-overflow-tooltip />
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
              v-hasPerm="['cms:news:publish']"
              type="primary"
              size="small"
              link
              @click="handlePublish(scope.row.id, 1)"
            >
              发布
            </el-button>
            <el-button
              v-else
              v-hasPerm="['cms:news:publish']"
              type="warning"
              size="small"
              link
              @click="handlePublish(scope.row.id, 2)"
            >
              下架
            </el-button>
            <el-button
              v-hasPerm="['cms:news:update']"
              type="primary"
              size="small"
              link
              @click="openDialog(scope.row.id)"
            >
              编辑
            </el-button>
            <el-button
              v-hasPerm="['cms:news:delete']"
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
        <el-form-item label="资讯标题" prop="title">
          <el-input v-model="formData.title" placeholder="资讯标题" clearable />
        </el-form-item>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="资讯栏目" prop="newsType">
              <el-select v-model="formData.newsType" placeholder="请选择栏目" style="width: 100%">
                <el-option
                  v-for="item in NEWS_TYPES"
                  :key="item.value"
                  :value="item.value"
                  :label="item.label"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="关联地点" prop="location">
              <el-input v-model="formData.location" placeholder="如 九鲤湖 / 湖亭村" clearable />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="封面地址" prop="cover">
          <el-input v-model="formData.cover" placeholder="选填，如 /img/news/n1.jpg" clearable />
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

        <el-form-item label="资讯正文" prop="content">
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
  name: "NewsManage",
  inheritAttrs: false,
});

import { NewsAPI, type NewsItem, type NewsForm, type NewsQueryParams } from "@/api/cms";
import type { FormInstance, FormRules } from "element-plus";

/** 栏目：与后端 cms_news.news_type、门户 NEWS_TYPES 一致 */
const NEWS_TYPES = [
  { value: 1, label: "景点宣传" },
  { value: 2, label: "旅游推介" },
  { value: 3, label: "乡镇历史" },
  { value: 4, label: "地域文化" },
  { value: 5, label: "人文地理" },
];

/** 栏目对应的标签配色 */
const TYPE_TAG: Record<number, "primary" | "success" | "info" | "warning" | "danger"> = {
  1: "primary",
  2: "success",
  3: "info",
  4: "warning",
  5: "danger",
};

const queryFormRef = ref<FormInstance>();
const dataFormRef = ref<FormInstance>();

// 查询参数
const queryParams = reactive<NewsQueryParams>({
  pageNum: 1,
  pageSize: 10,
});

// 列表数据
const pageData = ref<NewsItem[]>([]);
const total = ref(0);
const loading = ref(false);
const selectIds = ref<number[]>([]);

// 弹窗状态
const dialogState = reactive({
  title: "",
  visible: false,
});

// 表单数据
const formData = reactive<NewsForm>({});

// 验证规则
const rules: FormRules = {
  title: [{ required: true, message: "请输入资讯标题", trigger: "blur" }],
  newsType: [{ required: true, message: "请选择资讯栏目", trigger: "change" }],
  content: [{ required: true, message: "请输入资讯正文", trigger: "blur" }],
};

/**
 * 查询按钮点击事件
 */
function handleQuery(): void {
  queryParams.pageNum = 1;
  fetchData();
}

/**
 * 加载资讯列表数据
 */
function fetchData(): void {
  loading.value = true;
  NewsAPI.getPage(queryParams)
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
function handleSelectionChange(selection: NewsItem[]): void {
  selectIds.value = selection.map((item) => Number(item.id));
}

/**
 * 打开弹窗
 * @param id 资讯ID（编辑时传入）
 */
function openDialog(id?: string): void {
  dialogState.visible = true;
  if (id) {
    dialogState.title = "修改资讯";
    NewsAPI.getFormData(id).then((data) => {
      Object.assign(formData, data);
    });
  } else {
    dialogState.title = "新增资讯";
    Object.assign(formData, {
      id: undefined,
      title: "",
      newsType: 1,
      cover: "",
      summary: "",
      content: "",
      location: "",
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
    const request = id ? NewsAPI.update(id, formData) : NewsAPI.create(formData);
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
 * @param id 资讯ID
 * @param status 1 发布，2 下架
 */
function handlePublish(id: string, status: 1 | 2): void {
  if (status === 1) {
    NewsAPI.publish(id, 1).then(() => {
      ElMessage.success("发布成功，门户资讯列表已可查看");
      fetchData();
    });
    return;
  }
  ElMessageBox.confirm("下架后门户将立即看不到这条资讯，确认下架？", "提示", {
    confirmButtonText: "确定",
    cancelButtonText: "取消",
    type: "warning",
  }).then(
    () => {
      NewsAPI.publish(id, 2).then(() => {
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
 * 删除资讯
 * @param id 资讯ID
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
      NewsAPI.deleteByIds(deleteIds)
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
