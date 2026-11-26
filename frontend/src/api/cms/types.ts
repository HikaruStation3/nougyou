/**
 * 内容资讯（政策 / 资讯）类型定义
 */

import type { BaseQueryParams } from "@/api/common";

/** 政策分页查询参数（API-023 支撑接口） */
export interface PolicyQueryParams extends BaseQueryParams {
  /** 匹配标题与政策文号 */
  keyword?: string;
  /** 发布单位 */
  issueDept?: string;
  /** 发布状态：0 草稿 / 1 已发布 / 2 已下架 */
  publishStatus?: number;
}

/** 政策表单对象 */
export interface PolicyForm {
  id?: string;
  title?: string;
  /** 政策文号，如 钟政发〔2026〕7号 */
  policyNo?: string;
  /** 发布单位 */
  issueDept?: string;
  cover?: string;
  summary?: string;
  /** 正文按纯文本存储，换行分段 */
  content?: string;
}

/** 政策列表行 */
export interface PolicyItem {
  id: string;
  title: string;
  policyNo?: string;
  issueDept?: string;
  cover?: string;
  summary?: string;
  /** 0 草稿 / 1 已发布 / 2 已下架 */
  publishStatus: number;
  publishTime?: string;
  viewCount?: number;
  authorId?: string;
  createTime?: string;
}

/** 资讯分页查询参数（API-029 支撑接口） */
export interface NewsQueryParams extends BaseQueryParams {
  /** 匹配标题与摘要 */
  keyword?: string;
  /** 栏目：1 景点宣传 / 2 旅游推介 / 3 乡镇历史 / 4 地域文化 / 5 人文地理 */
  newsType?: number;
  /** 发布状态：0 草稿 / 1 已发布 / 2 已下架 */
  publishStatus?: number;
}

/** 资讯表单对象 */
export interface NewsForm {
  id?: string;
  title?: string;
  newsType?: number;
  cover?: string;
  summary?: string;
  /** 正文按纯文本存储，换行分段 */
  content?: string;
  /** 关联地点（景点 / 村落） */
  location?: string;
}

/** 资讯列表行 */
export interface NewsItem {
  id: string;
  title: string;
  newsType: number;
  newsTypeName?: string;
  cover?: string;
  summary?: string;
  location?: string;
  /** 0 草稿 / 1 已发布 / 2 已下架 */
  publishStatus: number;
  publishTime?: string;
  viewCount?: number;
  authorId?: string;
  createTime?: string;
}
