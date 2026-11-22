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
