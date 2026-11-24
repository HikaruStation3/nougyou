import http from "./http";

/**
 * 首页与工作台接口。
 * 首页聚合：GET /home（免登录）
 * 商家概览：GET /farmer/dashboard（需 dashboard:view）
 */

export function getHome() {
  return http.get("/home");
}

export function getFarmerDashboard() {
  return http.get("/farmer/dashboard");
}
