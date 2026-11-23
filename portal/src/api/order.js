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
