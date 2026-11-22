import request from "@/utils/request";

const PRODUCT_BASE_URL = "/api/v1/products";
const CATEGORY_BASE_URL = "/api/v1/categories";
const FARMER_PRODUCT_BASE_URL = "/api/v1/farmer/products";
const ADMIN_PRODUCT_BASE_URL = "/api/v1/admin/products";

export interface ProductQueryParams {
  pageNum?: number;
  pageSize?: number;
  categoryId?: string;
  keyword?: string;
  minPrice?: number;
  maxPrice?: number;
  isPoverty?: number;
  sortBy?: string;
}

export interface ProductItem {
  id: string;
  farmerId?: string;
  farmerName?: string;
  shopName?: string;
  categoryId?: string;
  categoryName?: string;
  name: string;
  cover?: string;
  price?: number;
  originalPrice?: number;
  stock?: number;
  unit?: string;
  weight?: number;
  origin?: string;
  isPoverty?: number;
  saleCount?: number;
  shelfStatus: number;
  createTime?: string;
}

export interface ProductDetailItem extends ProductItem {
  images?: string[];
  description?: string;
  entrustId?: string;
}

export interface ProductForm {
  categoryId?: string;
  name?: string;
  cover?: string;
  images?: string[];
  price?: number;
  originalPrice?: number;
  stock?: number;
  unit?: string;
  weight?: number;
  origin?: string;
  isPoverty?: number;
  description?: string;
  shelfStatus?: number;
}

export interface CategoryItem {
  id: string;
  name: string;
  parentId?: string;
  icon?: string;
  sort?: number;
  /** 状态：1启用 0禁用 */
  status?: number;
  children?: CategoryItem[];
}

export interface CategoryForm {
  name?: string;
  parentId?: string;
  icon?: string;
  sort?: number;
  status?: number;
}

const ProductAPI = {
  /* ---------------- 公开（API-040 / API-041 / API-042） ---------------- */
  getCategoryTree(onlyEnabled: boolean = true) {
    return request<any, CategoryItem[]>({
      url: CATEGORY_BASE_URL,
      method: "get",
      params: { onlyEnabled },
    });
  },

  getPage(queryParams: ProductQueryParams) {
    return request<any, PageResult<ProductItem>>({
      url: PRODUCT_BASE_URL,
      method: "get",
      params: queryParams,
    });
  },

  getDetail(id: string) {
    return request<any, ProductDetailItem>({
      url: `${PRODUCT_BASE_URL}/${id}`,
      method: "get",
    });
  },

  /* ---------------- 农户商家（API-043 ~ API-046） ---------------- */
  getMyPage(queryParams: ProductQueryParams) {
    return request<any, PageResult<ProductItem>>({
      url: FARMER_PRODUCT_BASE_URL,
      method: "get",
      params: queryParams,
    });
  },

  getMyDetail(id: string) {
    return request<any, ProductDetailItem>({
      url: `${FARMER_PRODUCT_BASE_URL}/${id}`,
      method: "get",
    });
  },

  create(data: ProductForm) {
    return request<any, ProductItem>({
      url: FARMER_PRODUCT_BASE_URL,
      method: "post",
      data,
    });
  },

  update(id: string, data: ProductForm) {
    return request<any, ProductItem>({
      url: `${FARMER_PRODUCT_BASE_URL}/${id}`,
      method: "put",
      data,
    });
  },

  shelf(id: string, shelfStatus: number) {
    return request<any, ProductItem>({
      url: `${FARMER_PRODUCT_BASE_URL}/${id}/shelf`,
      method: "put",
      data: { shelfStatus },
    });
  },

  remove(id: string) {
    return request<any, void>({
      url: `${FARMER_PRODUCT_BASE_URL}/${id}`,
      method: "delete",
    });
  },

  /* ---------------- 管理端（商品列表与分类维护） ---------------- */
  getAdminPage(queryParams: ProductQueryParams) {
    return request<any, PageResult<ProductItem>>({
      url: ADMIN_PRODUCT_BASE_URL,
      method: "get",
      params: queryParams,
    });
  },

  getAdminCategories() {
    return request<any, CategoryItem[]>({
      url: `${ADMIN_PRODUCT_BASE_URL}/categories`,
      method: "get",
    });
  },

  createCategory(data: CategoryForm) {
    return request<any, CategoryItem>({
      url: `${ADMIN_PRODUCT_BASE_URL}/categories`,
      method: "post",
      data,
    });
  },

  updateCategory(id: string, data: CategoryForm) {
    return request<any, CategoryItem>({
      url: `${ADMIN_PRODUCT_BASE_URL}/categories/${id}`,
      method: "put",
      data,
    });
  },

  deleteCategory(id: string) {
    return request<any, void>({
      url: `${ADMIN_PRODUCT_BASE_URL}/categories/${id}`,
      method: "delete",
    });
  },
};

export default ProductAPI;
