/**
 * 购物车事件总线。
 * 加购、改数量、删除等操作会改变购物车件数，通过该总线通知顶部徽标实时刷新。
 */
const handlers = new Set();

/** 通知所有订阅者购物车已变更 */
export function notifyCartChanged() {
  handlers.forEach((handler) => {
    try {
      handler();
    } catch {
      // 单个订阅者异常不影响其它订阅者
    }
  });
}

/**
 * 订阅购物车变更事件。
 * @param {Function} handler 变更回调
 * @returns {Function} 取消订阅函数
 */
export function onCartChanged(handler) {
  handlers.add(handler);
  return () => handlers.delete(handler);
}
