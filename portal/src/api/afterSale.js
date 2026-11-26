import http from "./http";

/**
 * 售后接口封装（API-061 ~ API-065）。
 * 买家：POST /after-sales、GET /after-sales、GET /after-sales/{id}
 * 商家：GET /farmer/after-sales、PUT /farmer/after-sales/{id}/handle
 */
export function createAfterSale(data) {
  return http.post("/after-sales", data);
}

export function getMyAfterSales(params) {
  return http.get("/after-sales", { params });
}

export function getAfterSale(id) {
  return http.get(`/after-sales/${id}`);
}

/** 订单详情页按明细展示售后进度 */
export function getOrderAfterSales(orderId) {
  return http.get(`/after-sales/by-order/${orderId}`);
}

export function getFarmerAfterSales(params) {
  return http.get("/farmer/after-sales", { params });
}

export function handleAfterSale(id, data) {
  return http.put(`/farmer/after-sales/${id}/handle`, data);
}
