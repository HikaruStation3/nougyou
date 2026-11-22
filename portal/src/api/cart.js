import http from "./http";

/**
 * 购物车接口封装。
 * GET /cart 列表、POST /cart 加购、PUT /cart/{id} 修改、DELETE /cart/{id} 删除、DELETE /cart 批量删除或清空
 */

export function getCart() {
  return http.get("/cart");
}

export function addToCart(data) {
  return http.post("/cart", data);
}

export function updateCartItem(id, data) {
  return http.put(`/cart/${id}`, data);
}

export function removeCartItem(id) {
  return http.delete(`/cart/${id}`);
}

/** ids 为空数组时后端按清空全部处理 */
export function removeCartItems(ids) {
  return http.delete("/cart", { data: { ids: ids || [] } });
}
