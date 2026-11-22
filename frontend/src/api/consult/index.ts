import request from "@/utils/request";

/** 会话对象：1 平台客服 / 2 农户商家 */
export type ServiceTargetType = 1 | 2;

/** 会话状态：0 排队中 / 1 进行中 / 2 已结束 */
export type ServiceSessionStatus = 0 | 1 | 2;

export interface ConsultSession {
  id: number;
  sessionNo: string;
  userId: number;
  userNickname: string;
  /** buyer / farmer */
  userRole: string;
  targetType: ServiceTargetType;
  targetId: number | null;
  targetName: string;
  adminId: number | null;
  adminName: string | null;
  status: ServiceSessionStatus;
  /** 按当前查看者视角的对方名称 */
  title: string;
  lastMsgTime: string | null;
  unreadCount: number;
  productId: number | null;
  productName: string | null;
  lastMsgPreview: string | null;
  lastMsgType: number | null;
  startTime: string | null;
  endTime: string | null;
}

export interface ConsultMessage {
  id: number;
  sessionId: number;
  senderId: number;
  /** buyer / farmer / admin */
  senderRole: string;
  senderName: string;
  senderAvatar: string | null;
  /** 1 文本 2 图片 */
  contentType: number;
  content: string;
  isRead: number;
  createTime: string;
}

export interface ConsultTarget {
  id: number;
  nickname: string;
  avatar: string | null;
}

export interface PageResult<T> {
  list: T[];
  total: number;
}

const BASE = "/api/v1/service";
const ADMIN_BASE = "/api/v1/admin/service";

const ConsultAPI = {
  /** 创建或获取会话：targetType=1 平台客服，=2 指定商家 */
  createOrGet(data: { targetType: ServiceTargetType; targetId?: number; productId?: number }) {
    return request<any, ConsultSession>({
      url: `${BASE}/sessions`,
      method: "post",
      data,
    });
  },

  /** 我的会话列表（含作为商家被咨询的会话） */
  listSessions(params: { pageNum?: number; pageSize?: number; status?: ServiceSessionStatus } = {}) {
    return request<any, PageResult<ConsultSession>>({
      url: `${BASE}/sessions`,
      method: "get",
      params,
    });
  },

  /** 会话消息：传 lastId 增量拉取 */
  listMessages(id: number, params: { pageNum?: number; pageSize?: number; lastId?: number } = {}) {
    return request<any, PageResult<ConsultMessage>>({
      url: `${BASE}/sessions/${id}/messages`,
      method: "get",
      params,
    });
  },

  /** 发送消息 */
  send(id: number, data: { contentType: 1 | 2; content: string }) {
    return request<any, ConsultMessage>({
      url: `${BASE}/sessions/${id}/messages`,
      method: "post",
      data,
    });
  },

  /** 标记已读 */
  markRead(id: number) {
    return request<any, number>({
      url: `${BASE}/sessions/${id}/read`,
      method: "put",
    });
  },

  /** 可咨询的农户商家 */
  listFarmers() {
    return request<any, ConsultTarget[]>({
      url: `${BASE}/farmers`,
      method: "get",
    });
  },

  /* ----------------------------- 客服工作台 ----------------------------- */

  adminSessions(
    params: {
      pageNum?: number;
      pageSize?: number;
      status?: ServiceSessionStatus;
      targetType?: ServiceTargetType;
      keyword?: string;
      mine?: boolean;
    } = {}
  ) {
    return request<any, PageResult<ConsultSession>>({
      url: `${ADMIN_BASE}/sessions`,
      method: "get",
      params,
    });
  },

  adminMessages(id: number, params: { pageNum?: number; pageSize?: number; lastId?: number } = {}) {
    return request<any, PageResult<ConsultMessage>>({
      url: `${ADMIN_BASE}/sessions/${id}/messages`,
      method: "get",
      params,
    });
  },

  adminSend(id: number, data: { contentType: 1 | 2; content: string }) {
    return request<any, ConsultMessage>({
      url: `${ADMIN_BASE}/sessions/${id}/messages`,
      method: "post",
      data,
    });
  },

  adminMarkRead(id: number) {
    return request<any, number>({
      url: `${ADMIN_BASE}/sessions/${id}/read`,
      method: "put",
    });
  },

  /** 接入会话 */
  accept(id: number) {
    return request<any, ConsultSession>({
      url: `${ADMIN_BASE}/sessions/${id}/accept`,
      method: "put",
    });
  },

  /** 结束会话（可附处理小结） */
  close(id: number, summary?: string) {
    return request<any, ConsultSession>({
      url: `${ADMIN_BASE}/sessions/${id}/close`,
      method: "put",
      data: { summary },
    });
  },
};

export default ConsultAPI;
