/**
 * 金额格式化：统一保留两位小数，空值按 0 处理。
 * 后端价格字段为元（DECIMAL(10,2)），前端展示统一走该函数，避免出现 12.5 这类不整齐的价格。
 * @param {number|string|null|undefined} value 金额
 * @returns {string} 形如 12.50
 */
export function money(value) {
  const num = Number(value);
  if (!Number.isFinite(num)) {
    return "0.00";
  }
  return num.toFixed(2);
}

/**
 * 互动数格式化：短视频的点赞、评论、播放量按「万」缩写，避免右侧操作栏被长数字撑开。
 * @param {number|string|null|undefined} value 数量
 * @returns {string} 形如 128 / 1.2万
 */
export function formatCount(value) {
  const num = Number(value);
  if (!Number.isFinite(num) || num <= 0) {
    return "0";
  }
  if (num < 10000) {
    return String(Math.floor(num));
  }
  const wan = num / 10000;
  return `${wan >= 10 ? Math.round(wan) : wan.toFixed(1).replace(/\.0$/, "")}万`;
}

/**
 * 时长格式化：秒 → m:ss，短视频列表与进度条共用。
 * @param {number} seconds 秒
 * @returns {string} 形如 0:10 / 2:05
 */
export function formatDuration(seconds) {
  const total = Math.max(0, Math.floor(Number(seconds) || 0));
  const minute = Math.floor(total / 60);
  const second = total % 60;
  return `${minute}:${String(second).padStart(2, "0")}`;
}

/**
 * 相对时间：评论、发布时间展示成「3小时前」这类短文案。
 * @param {string|Date} value 后端返回的 yyyy-MM-dd HH:mm:ss
 * @returns {string}
 */
export function timeAgo(value) {
  if (!value) {
    return "";
  }
  const time = new Date(String(value).replace(/-/g, "/")).getTime();
  if (!Number.isFinite(time)) {
    return String(value);
  }
  const diff = Date.now() - time;
  if (diff < 60 * 1000) {
    return "刚刚";
  }
  if (diff < 60 * 60 * 1000) {
    return `${Math.floor(diff / 60000)}分钟前`;
  }
  if (diff < 24 * 60 * 60 * 1000) {
    return `${Math.floor(diff / 3600000)}小时前`;
  }
  if (diff < 30 * 24 * 60 * 60 * 1000) {
    return `${Math.floor(diff / 86400000)}天前`;
  }
  return String(value).slice(0, 10);
}
