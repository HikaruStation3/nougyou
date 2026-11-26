import { ElMessageBox } from "element-plus";
import { getAlipayConfig, getAlipayPayUrl } from "../api/order";

/** 跳转收银台前记下订单号，结果页拿不到回跳参数时可兜底 */
const PENDING_KEY = "alipay_pending_order";

/** 沙箱配置只探测一次，避免每次点支付都多一个请求 */
let cachedStatus = null;

/**
 * 查询支付宝沙箱是否可用。
 * ready=false 表示密钥没填齐，前端只保留「模拟支付」。
 */
export async function alipayStatus(force = false) {
  if (cachedStatus && !force) {
    return cachedStatus;
  }
  try {
    cachedStatus = await getAlipayConfig();
  } catch {
    cachedStatus = { enabled: false, ready: false, missing: "" };
  }
  return cachedStatus;
}

/**
 * 选择支付方式并返回结果。
 * @returns {"alipay" | "mock" | null} null 表示用户关闭了弹窗
 */
export async function choosePayChannel(order, amountText) {
  const status = await alipayStatus();

  // 沙箱没配置：直接按模拟支付确认，并把缺哪几项写进提示，避免误以为支付宝可用
  if (!status.ready) {
    const missing = status.missing
      ? `（缺少 ${status.missing}）`
      : "（application.yml 里 alipay.enabled 未打开）";
    try {
      await ElMessageBox.confirm(
        `确认支付 ￥${amountText}？<br/><span style="color:#909399;font-size:12px">支付宝沙箱未配置${missing}，本次走模拟支付。</span>`,
        "模拟支付",
        { type: "info", confirmButtonText: "确认支付", dangerouslyUseHTMLString: true }
      );
      return "mock";
    } catch {
      return null;
    }
  }

  try {
    await ElMessageBox.confirm(
      `<div style="line-height:1.9">
        订单号：${order.orderNo}<br/>
        应付金额：<b style="color:#e1251b">￥${amountText}</b><br/>
        <span style="color:#909399;font-size:12px">
          「支付宝沙箱」跳转支付宝收银台，用沙箱买家账号付款；
          「模拟支付」直接把订单标记为已支付。
        </span>
      </div>`,
      "选择支付方式",
      {
        type: "info",
        dangerouslyUseHTMLString: true,
        confirmButtonText: "支付宝沙箱",
        cancelButtonText: "模拟支付",
        // 区分「点了取消按钮」和「点了右上角关闭」，前者才当作选择模拟支付
        distinguishCancelAndClose: true,
      }
    );
    return "alipay";
  } catch (error) {
    return error?.action === "cancel" ? "mock" : null;
  }
}

/** 取到收银台地址后整页跳转，离开前记住订单号 */
export async function gotoAlipayCashier(order) {
  const info = await getAlipayPayUrl(order.id);
  sessionStorage.setItem(PENDING_KEY, order.orderNo);
  window.location.href = info.payUrl;
}

/**
 * 从回跳地址里读订单号。
 * return_url 配的是 http://localhost:3100/#/pay/result，支付宝会把
 * out_trade_no 等参数拼在 # 之前，因此必须读 location.search；
 * 少数环境会把参数塞进 hash 内部，这里一并兜底。
 */
export function readReturnOrderNo() {
  const search = new URLSearchParams(window.location.search);
  const fromSearch = search.get("out_trade_no");
  if (fromSearch) {
    return { orderNo: fromSearch, tradeNo: search.get("trade_no") };
  }
  const hashQuery = window.location.hash.split("?")[1];
  if (hashQuery) {
    const params = new URLSearchParams(hashQuery);
    return { orderNo: params.get("out_trade_no"), tradeNo: params.get("trade_no") };
  }
  return { orderNo: null, tradeNo: null };
}

export function readPendingOrderNo() {
  return sessionStorage.getItem(PENDING_KEY);
}

export function clearPendingOrderNo() {
  sessionStorage.removeItem(PENDING_KEY);
}
