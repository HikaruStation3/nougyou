import http from "./http";

/**
 * 内容资讯接口封装（API-021/022/027/028）。
 * 政策与资讯均为公开数据，游客可匿名浏览。
 */

export function getPolicies(params) {
  return http.get("/policies", { params });
}

export function getPolicy(id) {
  return http.get(`/policies/${id}`);
}

export function getNewsList(params) {
  return http.get("/news", { params });
}

export function getNews(id) {
  return http.get(`/news/${id}`);
}

/** 资讯栏目字典，与后端 cms_news.news_type 一致 */
export const NEWS_TYPES = [
  { value: 1, label: "景点宣传" },
  { value: 2, label: "旅游推介" },
  { value: 3, label: "乡镇历史" },
  { value: 4, label: "地域文化" },
  { value: 5, label: "人文地理" },
];
