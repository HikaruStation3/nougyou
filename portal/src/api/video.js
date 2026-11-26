import http from "./http";

/**
 * 短视频接口封装（API-075 ~ 086）。
 * 观看类接口对游客开放；点赞、评论、关注需要登录，未登录时后端返回 401。
 */

/** 短视频列表：sortBy = new 最新 / hot 最热，farmerId 可按商家筛选 */
export function getVideos(params = {}) {
  return http.get("/videos", { params });
}

/** 播放页数据：含当前用户的点赞与关注状态 */
export function getVideo(id) {
  return http.get(`/videos/${id}`);
}

/** 播放量上报，失败不打扰用户 */
export function reportView(id) {
  return http.post(`/videos/${id}/view`, null, { silent: true });
}

/** 点赞 / 取消点赞（幂等） */
export function toggleLike(id) {
  return http.post(`/videos/${id}/like`);
}

/** 评论列表：一级评论分页，children 为回复 */
export function getComments(id, params = {}) {
  return http.get(`/videos/${id}/comments`, { params });
}

/** 发表评论或回复 */
export function addComment(id, data) {
  return http.post(`/videos/${id}/comments`, data);
}

/** 删除评论 */
export function deleteComment(id) {
  return http.delete(`/videos/comments/${id}`);
}

/** 商家主页：资料、在售商品与视频 */
export function getFarmerProfile(id) {
  return http.get(`/farmers/${id}`);
}

/** 关注 / 取消关注商家（幂等） */
export function toggleFollow(id) {
  return http.post(`/farmers/${id}/follow`);
}

/* --------------------------- 商家自助 --------------------------- */

/** 我的短视频：含待审核与驳回记录 */
export function getMyVideos(params = {}) {
  return http.get("/farmer/videos", { params });
}

/** 发布短视频，发布后进入待审核 */
export function publishVideo(data) {
  return http.post("/farmer/videos", data);
}

/** 删除自己的短视频 */
export function deleteVideo(id) {
  return http.delete(`/farmer/videos/${id}`);
}

// 视频与封面统一走文件上传接口（API-113），保留原有导出名
export { uploadFile } from "./file";
