import http from "./http";

/**
 * 农家乐项目接口封装。
 * 公开浏览：GET /projects、GET /projects/{id}
 * 商家自助：GET/POST /farmer/projects、PUT /farmer/projects/{id}、PUT /farmer/projects/{id}/shelf
 */

export function getProjects(params) {
  return http.get("/projects", { params });
}

export function getProject(id) {
  return http.get(`/projects/${id}`);
}

export function getMyProjects(params) {
  return http.get("/farmer/projects", { params });
}

export function createProject(data) {
  return http.post("/farmer/projects", data);
}

export function updateProject(id, data) {
  return http.put(`/farmer/projects/${id}`, data);
}

export function shelfProject(id, shelfStatus) {
  return http.put(`/farmer/projects/${id}/shelf`, { shelfStatus });
}
