import http from "./http";

/**
 * 农产品接口封装。
 * 公开浏览：GET /categories、GET /products、GET /products/{id}
 * 商家自助：GET/POST /farmer/products、PUT /farmer/products/{id}、PUT /farmer/products/{id}/shelf、DELETE /farmer/products/{id}
 */

export function getCategories(params) {
  return http.get("/categories", { params });
}

export function getProducts(params) {
  return http.get("/products", { params });
}

export function getProduct(id) {
  return http.get(`/products/${id}`);
}

export function getMyProducts(params) {
  return http.get("/farmer/products", { params });
}

export function getMyProduct(id) {
  return http.get(`/farmer/products/${id}`);
}

export function createProduct(data) {
  return http.post("/farmer/products", data);
}

export function updateProduct(id, data) {
  return http.put(`/farmer/products/${id}`, data);
}

export function shelfProduct(id, shelfStatus) {
  return http.put(`/farmer/products/${id}/shelf`, { shelfStatus });
}

export function deleteProduct(id) {
  return http.delete(`/farmer/products/${id}`);
}
