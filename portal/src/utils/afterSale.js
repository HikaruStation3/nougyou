/** 售后枚举与展示文案，买家、商家两端共用 */

export const SALE_TYPE = {
  1: { text: "仅退款", tag: "warning" },
  2: { text: "退货退款", tag: "danger" },
  3: { text: "换货", tag: "primary" },
  4: { text: "咨询", tag: "info" },
};

export const AFTER_SALE_STATUS = {
  0: { text: "待处理", tag: "warning" },
  1: { text: "处理中", tag: "primary" },
  2: { text: "已完成", tag: "success" },
  3: { text: "已驳回", tag: "danger" },
};

export function saleTypeText(type) {
  return SALE_TYPE[type]?.text || "-";
}

export function saleTypeTag(type) {
  return SALE_TYPE[type]?.tag || "info";
}

export function afterSaleStatusText(status) {
  return AFTER_SALE_STATUS[status]?.text || "-";
}

export function afterSaleStatusTag(status) {
  return AFTER_SALE_STATUS[status]?.tag || "info";
}

/** 售后进行中（待处理/处理中）时不允许针对同一商品再次申请 */
export function isAfterSalePending(status) {
  return status === 0 || status === 1;
}
