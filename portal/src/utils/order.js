/**
 * 订单与委托销售的枚举文案，供门户各页面复用。
 */

export const ORDER_STATUS = {
  0: { text: "待付款", tag: "warning" },
  1: { text: "待发货", tag: "primary" },
  2: { text: "待收货", tag: "info" },
  3: { text: "已完成", tag: "success" },
  4: { text: "已取消", tag: "info" },
  5: { text: "售后中", tag: "danger" },
};

export const PAY_STATUS = {
  0: { text: "未支付", tag: "warning" },
  1: { text: "已支付", tag: "success" },
  2: { text: "已退款", tag: "info" },
};

export const PAY_TYPE = {
  1: "微信支付",
  2: "支付宝（沙箱）",
  3: "模拟支付",
};

export const SETTLE_CYCLE = {
  1: "按月",
  2: "按季",
  3: "按单",
};

export const ENTRUST_STATUS = {
  0: { text: "待受托方确认", tag: "warning" },
  1: { text: "生效中", tag: "success" },
  2: { text: "已终止", tag: "info" },
  3: { text: "已拒绝", tag: "danger" },
};

export function orderStatusText(status) {
  return ORDER_STATUS[status]?.text || "未知";
}

export function orderStatusTag(status) {
  return ORDER_STATUS[status]?.tag || "info";
}
