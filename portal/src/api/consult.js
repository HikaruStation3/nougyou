import http from "./http";

const BASE = "/service";

/**
 * 发起或复用会话
 * @param {{ targetType: 1 | 2, targetId?: number, productId?: number }} data
 *        targetType 1=平台客服，2=指定农户商家
 */
export function createSession(data) {
  return http.post(`${BASE}/sessions`, data);
}

/** 我的会话列表 */
export function listSessions(params = {}) {
  return http.get(`${BASE}/sessions`, { params });
}

/** 会话消息，lastId 用于增量拉取 */
export function listMessages(id, params = {}) {
  return http.get(`${BASE}/sessions/${id}/messages`, { params });
}

/** 发送文本消息 */
export function sendMessage(id, content) {
  return http.post(`${BASE}/sessions/${id}/messages`, { contentType: 1, content });
}

/** 标记会话已读 */
export function markRead(id) {
  return http.put(`${BASE}/sessions/${id}/read`);
}

/** 可咨询的农户商家列表 */
export function listFarmers() {
  return http.get(`${BASE}/farmers`);
}
