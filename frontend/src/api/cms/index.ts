import request from "@/utils/request";
import type { PageResult } from "@/api/common";
import type {
  PolicyQueryParams,
  PolicyForm,
  PolicyItem,
  NewsQueryParams,
  NewsForm,
  NewsItem,
} from "./types";

const POLICY_BASE_URL = "/api/v1/admin/policies";
const NEWS_BASE_URL = "/api/v1/admin/news";

/** 扶贫政策后台（API-023~026） */
const PolicyAPI = {
  /** 分页查询：含草稿与已下架 */
  getPage(queryParams?: PolicyQueryParams) {
    return request<any, PageResult<PolicyItem>>({
      url: POLICY_BASE_URL,
      method: "get",
      params: queryParams,
    });
  },
  /** 编辑回显 */
  getFormData(id: string) {
    return request<any, PolicyForm>({ url: `${POLICY_BASE_URL}/${id}/form`, method: "get" });
  },
  /** 新增政策（落草稿） */
  create(data: PolicyForm) {
    return request({ url: POLICY_BASE_URL, method: "post", data });
  },
  /** 编辑政策 */
  update(id: string, data: PolicyForm) {
    return request({ url: `${POLICY_BASE_URL}/${id}`, method: "put", data });
  },
  /** 批量删除，多个以英文逗号(,)分割 */
  deleteByIds(ids: string) {
    return request({ url: `${POLICY_BASE_URL}/${ids}`, method: "delete" });
  },
  /** 发布（status=1）或下架（status=2） */
  publish(id: string, status: 1 | 2 = 1) {
    return request({ url: `${POLICY_BASE_URL}/${id}/publish`, method: "put", params: { status } });
  },
};

/** 扶贫资讯后台（API-029~032） */
const NewsAPI = {
  /** 分页查询：含草稿与已下架 */
  getPage(queryParams?: NewsQueryParams) {
    return request<any, PageResult<NewsItem>>({
      url: NEWS_BASE_URL,
      method: "get",
      params: queryParams,
    });
  },
  /** 编辑回显 */
  getFormData(id: string) {
    return request<any, NewsForm>({ url: `${NEWS_BASE_URL}/${id}/form`, method: "get" });
  },
  /** 新增资讯（落草稿） */
  create(data: NewsForm) {
    return request({ url: NEWS_BASE_URL, method: "post", data });
  },
  /** 编辑资讯 */
  update(id: string, data: NewsForm) {
    return request({ url: `${NEWS_BASE_URL}/${id}`, method: "put", data });
  },
  /** 批量删除，多个以英文逗号(,)分割 */
  deleteByIds(ids: string) {
    return request({ url: `${NEWS_BASE_URL}/${ids}`, method: "delete" });
  },
  /** 发布（status=1）或下架（status=2） */
  publish(id: string, status: 1 | 2 = 1) {
    return request({ url: `${NEWS_BASE_URL}/${id}/publish`, method: "put", params: { status } });
  },
};

export { PolicyAPI, NewsAPI };
export * from "./types";
