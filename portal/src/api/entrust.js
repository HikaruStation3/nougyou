import http from "./http";

/**
 * 委托销售接口封装。
 * 建立 / 委托出去 / 代销 / 详情 / 确认 / 终止 / 统计 / 结算单
 */
export function getEntrustAgents() {
  return http.get("/entrusts/agents");
}

export function createEntrust(data) {
  return http.post("/entrusts", data);
}

export function getSentEntrusts(params) {
  return http.get("/entrusts/sent", { params });
}

export function getReceivedEntrusts(params) {
  return http.get("/entrusts/received", { params });
}

export function getEntrust(id) {
  return http.get(`/entrusts/${id}`);
}

export function confirmEntrust(id, data) {
  return http.put(`/entrusts/${id}/confirm`, data);
}

export function terminateEntrust(id, data) {
  return http.put(`/entrusts/${id}/terminate`, data || {});
}

export function getEntrustStat(id) {
  return http.get(`/entrusts/${id}/stat`);
}

export function getEntrustSettlements(id, params) {
  return http.get(`/entrusts/${id}/settlements`, { params });
}
