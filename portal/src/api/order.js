import http from "./http";

/**
 * 订单接口封装。
 * 买家：POST /orders、GET /orders、GET /orders/{id}、PUT /orders/{id}/pay|cancel|confirm
 * 商家：GET /farmer/orders、PUT /farmer/orders/{id}/ship
 */
export function createOrder(data) {
  return http.post("/orders", data);
}

export function getMyOrders(params) {
  return http.get("/orders", { params });
}

export function getOrder(id) {
  return http.get(`/orders/${id}`);
}

export function payOrder(id, data) {
  return http.put(`/orders/${id}/pay`, data || {});
}

export function cancelOrder(id, data) {
  return http.put(`/orders/${id}/cancel`, data || {});
}

export function confirmOrder(id) {
  return http.put(`/orders/${id}/confirm`);
}

export function getFarmerOrders(params) {
  return http.get("/farmer/orders", { params });
}

export function shipOrder(id, data) {
  return http.put(`/farmer/orders/${id}/ship`, data);
}

/* ============ 支付宝沙箱（电脑网站支付） ============ */

/** 探测沙箱是否已配置，未配置时前端只保留「模拟支付」 */
export function getAlipayConfig() {
  return http.get("/pay/alipay/config");
}

/** 获取收银台跳转地址（后端签名后返回完整 URL） */
export function getAlipayPayUrl(orderId) {
  return http.get("/pay/alipay/page", { params: { orderId } });
}

/** 从收银台跳回后主动查单，查询要连支付宝网关，放宽超时 */
export function queryAlipayTrade(orderNo) {
  return http.get("/pay/alipay/query", { params: { orderNo }, timeout: 25000 });
}
