<template>
  <div class="app-container">
    <el-card shadow="never" class="mb-4">
      <el-form inline @submit.prevent>
        <el-form-item label="商品分类">
          <el-tree-select
            v-model="queryParams.categoryId"
            :data="categoryOptions"
            :props="treeProps"
            node-key="id"
            check-strictly
            placeholder="全部分类"
            clearable
            style="width: 200px"
          />
        </el-form-item>
        <el-form-item label="关键字">
          <el-input
            v-model="queryParams.keyword"
            placeholder="商品名称 / 产地"
            clearable
            style="width: 220px"
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="价格区间">
          <el-input-number
            v-model="queryParams.minPrice"
            :min="0"
            :precision="2"
            :controls="false"
            placeholder="最低价"
            style="width: 100px"
          />
          <span class="mx-2 text-[--el-text-color-secondary]">-</span>
          <el-input-number
            v-model="queryParams.maxPrice"
            :min="0"
            :precision="2"
            :controls="false"
            placeholder="最高价"
            style="width: 100px"
          />
        </el-form-item>
        <el-form-item label="扶贫商品">
          <el-select v-model="queryParams.isPoverty" placeholder="全部" clearable style="width: 120px">
            <el-option label="扶贫产品" :value="1" />
            <el-option label="普通商品" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item label="排序">
          <el-select v-model="queryParams.sortBy" style="width: 140px">
            <el-option label="最新上架" value="new" />
            <el-option label="价格从低到高" value="price_asc" />
            <el-option label="价格从高到低" value="price_desc" />
            <el-option label="销量优先" value="sale_desc" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="search" @click="handleQuery">查询</el-button>
          <el-button icon="refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="productList" border stripe>
        <el-table-column label="商品" min-width="220">
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
                <div class="font-medium">{{ row.name }}</div>
                <div class="text-xs text-[--el-text-color-secondary]">
                  {{ row.categoryName || "未分类" }}
                </div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="商家" min-width="130">
          <template #default="{ row }">
            <div>{{ row.farmerName || "-" }}</div>
            <div class="text-xs text-[--el-text-color-secondary]">{{ row.shopName || "-" }}</div>
          </template>
        </el-table-column>
        <el-table-column label="售价" width="100" align="right">
          <template #default="{ row }">
            <div class="font-medium text-[--el-color-danger]">￥{{ money(row.price) }}</div>
            <div v-if="row.originalPrice" class="text-xs text-[--el-text-color-secondary] line-through">
              ￥{{ money(row.originalPrice) }}
            </div>
          </template>
        </el-table-column>
        <el-table-column label="库存" width="90" align="center">
          <template #default="{ row }">
            {{ row.stock ?? 0 }} {{ row.unit || "" }}
          </template>
        </el-table-column>
        <el-table-column prop="saleCount" label="销量" width="70" align="center">
          <template #default="{ row }">{{ row.saleCount ?? 0 }}</template>
        </el-table-column>
        <el-table-column label="扶贫产品" width="95" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.isPoverty === 1" type="danger" effect="plain">扶贫</el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="上架状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.shelfStatus === 1 ? 'success' : 'info'" effect="plain">
              {{ row.shelfStatus === 1 ? "已上架" : "已下架" }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="origin" label="产地" width="140" show-overflow-tooltip>
          <template #default="{ row }">{{ row.origin || "-" }}</template>
        </el-table-column>
        <el-table-column label="操作" width="70" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openDetail(row)">详情</el-button>
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

    <el-dialog v-model="detailVisible" title="农产品详情" width="820px" top="6vh">
      <el-descriptions v-if="currentProduct" :column="2" border>
        <el-descriptions-item label="商品名称">{{ currentProduct.name }}</el-descriptions-item>
        <el-descriptions-item label="商品分类">{{ currentProduct.categoryName || "-" }}</el-descriptions-item>
        <el-descriptions-item label="商家姓名">{{ currentProduct.farmerName || "-" }}</el-descriptions-item>
        <el-descriptions-item label="店铺名称">{{ currentProduct.shopName || "-" }}</el-descriptions-item>
        <el-descriptions-item label="售价">￥{{ money(currentProduct.price) }}</el-descriptions-item>
        <el-descriptions-item label="原价">
          {{ currentProduct.originalPrice ? `￥${money(currentProduct.originalPrice)}` : "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="库存">
          {{ currentProduct.stock ?? 0 }} {{ currentProduct.unit || "" }}
        </el-descriptions-item>
        <el-descriptions-item label="重量">
          {{ currentProduct.weight ? `${currentProduct.weight} kg` : "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="产地" :span="2">{{ currentProduct.origin || "-" }}</el-descriptions-item>
        <el-descriptions-item label="销量">{{ currentProduct.saleCount ?? 0 }}</el-descriptions-item>
        <el-descriptions-item label="扶贫产品">
          <el-tag v-if="currentProduct.isPoverty === 1" type="danger" effect="plain">扶贫产品</el-tag>
          <span v-else>普通商品</span>
        </el-descriptions-item>
        <el-descriptions-item label="上架状态">
          <el-tag :type="currentProduct.shelfStatus === 1 ? 'success' : 'info'" effect="plain">
            {{ currentProduct.shelfStatus === 1 ? "已上架" : "已下架" }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ currentProduct.createTime || "-" }}</el-descriptions-item>
        <el-descriptions-item label="商品描述" :span="2">
          {{ currentProduct.description || "-" }}
        </el-descriptions-item>
        <el-descriptions-item label="封面图" :span="2">
          <el-image
            v-if="currentProduct.cover"
            :src="currentProduct.cover"
            :preview-src-list="imageList"
            fit="cover"
            class="h-32 w-48 rounded"
            preview-teleported
          />
          <span v-else>-</span>
        </el-descriptions-item>
        <el-descriptions-item v-if="currentProduct.images?.length" label="商品图集" :span="2">
          <div class="flex flex-wrap gap-2">
            <el-image
              v-for="(img, idx) in currentProduct.images"
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
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import ProductAPI, {
  type CategoryItem,
  type ProductDetailItem,
  type ProductItem,
  type ProductQueryParams,
} from "@/api/product";

defineOptions({ name: "Product" });

const loading = ref(false);
const productList = ref<ProductItem[]>([]);
const total = ref(0);
const categoryTree = ref<CategoryItem[]>([]);

const queryParams = reactive<ProductQueryParams>({
  pageNum: 1,
  pageSize: 10,
  categoryId: undefined,
  keyword: "",
  minPrice: undefined,
  maxPrice: undefined,
  isPoverty: undefined,
  sortBy: "new",
});

const detailVisible = ref(false);
const currentProduct = ref<ProductDetailItem>();

const categoryOptions = computed(() => toTreeOptions(categoryTree.value));

/** 金额统一保留两位小数 */
function money(value?: number | string | null) {
  return Number(value ?? 0).toFixed(2);
}

/** el-tree-select 取值约定：value 取分类 id */
const treeProps = {
  value: "value",
  label: "name",
  children: "children",
} as any;

/** 统一补齐 value 字段，兼容 el-tree-select 的取值约定 */
function toTreeOptions(nodes: CategoryItem[]): any[] {
  return nodes.map((node) => ({
    id: node.id,
    value: node.id,
    name: node.name,
    children: node.children?.length ? toTreeOptions(node.children) : undefined,
  }));
}

const imageList = computed(() => {
  const product = currentProduct.value;
  if (!product) return [];
  return [product.cover, ...(product.images ?? [])].filter((url): url is string => Boolean(url));
});

async function loadData() {
  loading.value = true;
  try {
    const page = await ProductAPI.getAdminPage(queryParams);
    productList.value = page.list;
    total.value = page.total;
  } finally {
    loading.value = false;
  }
}

async function loadCategories() {
  categoryTree.value = await ProductAPI.getAdminCategories();
}

function handleQuery() {
  queryParams.pageNum = 1;
  loadData();
}

function handleReset() {
  queryParams.categoryId = undefined;
  queryParams.keyword = "";
  queryParams.minPrice = undefined;
  queryParams.maxPrice = undefined;
  queryParams.isPoverty = undefined;
  queryParams.sortBy = "new";
  queryParams.pageNum = 1;
  loadData();
}

async function openDetail(row: any) {
  try {
    currentProduct.value = await ProductAPI.getDetail(row.id);
    detailVisible.value = true;
  } catch {
    ElMessage.error("商品详情加载失败");
  }
}

onMounted(async () => {
  await loadCategories();
  await loadData();
});
</script>