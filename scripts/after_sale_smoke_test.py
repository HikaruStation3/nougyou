"""售后模块接口自动化测试（API-061 ~ API-066）。

覆盖范围：
    买家    提交售后（退款/退货/换货/咨询）、重复申请拦截、金额与小计校验、我的售后列表与筛选
    商家    本店售后列表、受理并退款、拒绝（拒绝必填意见）、越权拦截
    管理员  后台售后列表筛「已驳回」、平台介入改判退款 / 驳回
    联动    退货退款回滚库存、部分退款订单仍为已支付、多角色数据隔离
    权限    未登录 401、非本人/非本店 403

用法：
    1) 启动后端时关闭验证码（仅测试环境）：
       mvn spring-boot:run -Dspring-boot.run.arguments="--nougyou.captcha.enabled=false"
    2) 执行：
       python scripts/after_sale_smoke_test.py
       API_BASE=http://localhost:8081 python scripts/after_sale_smoke_test.py

说明：
    - 选择 buyer01 与 farmer01（吴叔山货铺）之间的已完成订单，可重复执行；
    - 脚本产生的售后单都会处理完（完成/驳回），不会长期占用「待处理」；
    - 退款类用例只退 0.01 元，避免把演示订单退成「已退款」。
"""

import json
import os
import urllib.error
import urllib.parse
import urllib.request

BASE = os.environ.get("API_BASE", "http://localhost:8080").rstrip("/")
PASSWORD = os.environ.get("API_PASSWORD", "123456")
FARMER_ID = int(os.environ.get("FARMER_ID", "4"))  # farmer01 吴叔山货铺

PASS, FAIL = "PASS", "FAIL"
results = []


def call(method, path, token=None, body=None):
    url = BASE + urllib.parse.quote(path, safe="/?=&")
    data = json.dumps(body, ensure_ascii=False).encode("utf-8") if body is not None else None
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = "Bearer " + token
    req = urllib.request.Request(url, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req) as resp:
            raw = resp.read().decode("utf-8")
            return resp.status, (json.loads(raw) if raw else None)
    except urllib.error.HTTPError as e:
        raw = e.read().decode("utf-8")
        try:
            return e.code, json.loads(raw)
        except Exception:
            return e.code, raw


def login(username):
    st, res = call("POST", "/api/v1/auth/login", body={"username": username, "password": PASSWORD})
    if res and res.get("code") == "00000":
        return res["data"]["accessToken"]
    if res and res.get("code") == "A0211":
        print("登录失败：后端开启了图形验证码。请用 --nougyou.captcha.enabled=false 重启后端后再跑。")
    return None


def ok(res):
    return bool(res) and res.get("code") == "00000"


def data(res):
    return (res or {}).get("data")


def check(name, passed, detail=""):
    results.append((PASS if passed else FAIL, name, str(detail)))
    print(("[通过] " if passed else "[失败] ") + name + ("  " + str(detail) if detail else ""))


def pick_order(token):
    """从买家订单里挑一个与 farmer01 交易、已支付且可售后的订单。"""
    st, res = call("GET", "/api/v1/orders?pageNum=1&pageSize=100", token)
    orders = data(res) or {}
    for order in orders.get("list") or []:
        if int(order.get("farmerId", 0)) != FARMER_ID:
            continue
        if order.get("payStatus") != 1 or order.get("orderStatus") not in (2, 3):
            continue
        for item in order.get("items") or []:
            if item.get("afterSaleStatus") in (None, 2, 3):
                return order, item
    return None, None


def find_in_page(token, path, keyword):
    st, res = call("GET", f"{path}?pageNum=1&pageSize=100&keyword={urllib.parse.quote(keyword)}", token)
    return [row for row in (data(res) or {}).get("list") or [] if row.get("afterSaleNo") == keyword]


def main():
    print(f"服务地址：{BASE}")
    buyer = login("buyer01")
    buyer2 = login("buyer02")
    farmer = login("farmer01")
    farmer2 = login("farmer02")
    admin = login("admin")
    check("登录买家/商家/管理员账号", all([buyer, buyer2, farmer, farmer2, admin]))

    order, item = pick_order(buyer)
    check("找到 buyer01 与 farmer01 的可售后订单", order is not None,
          f"订单 {order.get('orderNo')} 明细 {item.get('productName')}" if order else "无可用订单")
    if not order:
        return summary()

    order_id, item_id = int(order["id"]), int(item["id"])
    subtotal = float(item.get("subtotal") or 0)
    quantity = int(item.get("quantity") or 1)

    # ---------- 校验：越权与金额上限（此时该明细还没有进行中的售后） ----------
    st, res = call("POST", "/api/v1/after-sales", buyer2, {
        "orderId": order_id, "orderItemId": item_id, "saleType": 4, "reason": "越权测试",
    })
    check("他人订单提交售后被拒", (res or {}).get("code") == "A0301",
          f"code={(res or {}).get('code')} {(res or {}).get('msg')}")

    st, res = call("POST", "/api/v1/after-sales", buyer, {
        "orderId": order_id, "orderItemId": item_id, "saleType": 1, "reason": "超额退款",
        "refundAmount": round(subtotal + 1, 2),
    })
    check("退款金额超过小计被拒", (res or {}).get("code") == "A0400",
          f"code={(res or {}).get('code')} {(res or {}).get('msg')}")

    # ---------- 提交售后（咨询，避免影响退款口径） ----------
    body = {
        "orderId": order_id,
        "orderItemId": item_id,
        "saleType": 4,
        "reason": "售后模块自测-咨询",
        "description": "脚本自动提交，验证售后链路",
    }
    st, res = call("POST", "/api/v1/after-sales", buyer, body)
    check("API-061 提交咨询类售后", st == 200 and ok(res), (res or {}).get("msg"))
    consult = data(res) or {}
    check("提交后状态为待处理(0)", consult.get("status") == 0, consult.get("status"))

    st, res = call("POST", "/api/v1/after-sales", buyer, body)
    check("同一明细重复提交被拦截", (res or {}).get("code") == "A0400",
          f"code={(res or {}).get('code')} {(res or {}).get('msg')}")

    st, res = call("GET", f"/api/v1/after-sales/{consult.get('id')}", buyer)
    check("API-063 买家查看售后详情", st == 200 and ok(res), f"HTTP {st}")

    st, res = call("GET", f"/api/v1/after-sales/{consult.get('id')}", buyer2)
    check("非本人查看详情被拒", (res or {}).get("code") == "A0301",
          f"code={(res or {}).get('code')}")

    st, res = call("GET", "/api/v1/after-sales?pageNum=1&pageSize=10", buyer)
    list_ok = any(row.get("id") == consult.get("id") for row in (data(res) or {}).get("list") or [])
    check("API-062 我的售后列表包含新单", st == 200 and list_ok, f"HTTP {st}")

    st, res = call("GET", "/api/v1/after-sales?pageNum=1&pageSize=10&keyword="
                   + urllib.parse.quote(consult.get("afterSaleNo") or ""), buyer)
    check("售后列表按单号搜索命中", any(row.get("afterSaleNo") == consult.get("afterSaleNo")
                                       for row in (data(res) or {}).get("list") or []))

    st, res = call("GET", f"/api/v1/after-sales/by-order/{order_id}", buyer)
    check("订单维度售后进度可查", st == 200 and len(data(res) or []) >= 1, f"HTTP {st}")

    # ---------- 商家处理 ----------
    st, res = call("GET", "/api/v1/farmer/after-sales?pageNum=1&pageSize=10&status=0", farmer)
    check("API-064 商家待处理列表包含新单",
          any(row.get("id") == consult.get("id") for row in (data(res) or {}).get("list") or []))

    st, res = call("PUT", f"/api/v1/farmer/after-sales/{consult.get('id')}/handle", farmer2,
                   {"handleResult": 3, "handleRemark": "越权测试"})
    check("其他商家处理被拒", (res or {}).get("code") == "A0301",
          f"code={(res or {}).get('code')}")

    st, res = call("PUT", f"/api/v1/farmer/after-sales/{consult.get('id')}/handle", buyer,
                   {"handleResult": 3, "handleRemark": "买家越权"})
    check("买家调用商家处理接口被拒", st in (400, 403), f"HTTP {st}")

    st, res = call("PUT", f"/api/v1/farmer/after-sales/{consult.get('id')}/handle", farmer,
                   {"handleResult": 3})
    check("拒绝时未填意见被拦截", (res or {}).get("code") == "A0400",
          f"code={(res or {}).get('code')} {(res or {}).get('msg')}")

    st, res = call("PUT", f"/api/v1/farmer/after-sales/{consult.get('id')}/handle", farmer,
                   {"handleResult": 3, "handleRemark": "咨询已电话回复"})
    check("API-065 商家拒绝售后", st == 200 and (data(res) or {}).get("status") == 3,
          (data(res) or {}).get("status"))

    # ---------- 管理员介入 ----------
    st, res = call("GET", "/api/v1/admin/after-sales?pageNum=1&pageSize=100&status=3", admin)
    check("后台售后列表可按已驳回筛选",
          any(row.get("id") == consult.get("id") for row in (data(res) or {}).get("list") or []))

    st, res = call("PUT", f"/api/v1/admin/after-sales/{consult.get('id')}/handle", buyer,
                   {"handleResult": 1, "handleRemark": "越权"})
    check("买家调用平台介入被拒", st in (400, 403), f"HTTP {st}")

    st, res = call("PUT", f"/api/v1/admin/after-sales/{consult.get('id')}/handle", admin,
                   {"handleResult": 1, "handleRemark": "经核实咨询已回复，支持结案"})
    check("API-066 平台介入改判完成", st == 200 and (data(res) or {}).get("status") == 2,
          (data(res) or {}).get("status"))

    # ---------- 退货退款：库存回滚 ----------
    product_id = int(item.get("productId"))
    st, res = call("GET", f"/api/v1/products/{product_id}")
    stock_before = (data(res) or {}).get("stock")
    body = {
        "orderId": order_id,
        "orderItemId": item_id,
        "saleType": 2,
        "reason": "售后模块自测-退货退款",
        "description": "验证退货退款回滚库存",
        "refundAmount": 0.01,
    }
    st, res = call("POST", "/api/v1/after-sales", buyer, body)
    check("提交退货退款售后", st == 200 and ok(res), (res or {}).get("msg"))
    refund_case = data(res) or {}

    st, res = call("PUT", f"/api/v1/farmer/after-sales/{refund_case.get('id')}/handle", farmer,
                   {"handleResult": 1, "refundAmount": 0.01, "handleRemark": "已收到退回商品"})
    check("商家受理退货退款", st == 200 and (data(res) or {}).get("status") == 2,
          (data(res) or {}).get("status"))

    st, res = call("GET", f"/api/v1/products/{product_id}")
    stock_after = (data(res) or {}).get("stock")
    check("退货退款回滚库存", stock_after == (stock_before or 0) + quantity,
          f"{stock_before} → {stock_after}（应 +{quantity}）")

    st, res = call("GET", f"/api/v1/orders/{order_id}", buyer)
    check("小额退款后订单仍为已支付", (data(res) or {}).get("payStatus") == 1,
          (data(res) or {}).get("payStatus"))

    # ---------- 换货：无退款金额 ----------
    st, res = call("POST", "/api/v1/after-sales", buyer, {
        "orderId": order_id, "orderItemId": item_id, "saleType": 3, "reason": "售后模块自测-换货",
    })
    check("换货申请不填退款金额也可提交", st == 200 and ok(res), (res or {}).get("msg"))
    exchange = data(res) or {}
    st, res = call("PUT", f"/api/v1/farmer/after-sales/{exchange.get('id')}/handle", farmer,
                   {"handleResult": 1, "handleRemark": "已寄出新品"})
    check("换货受理成功", st == 200 and (data(res) or {}).get("status") == 2,
          (data(res) or {}).get("status"))

    # ---------- 未登录 ----------
    st, res = call("GET", "/api/v1/after-sales")
    check("未登录访问我的售后返回 401", st == 401, f"HTTP {st}")

    summary()


def summary():
    failed = [r for r in results if r[0] == FAIL]
    print("-" * 60)
    print(f"共 {len(results)} 项，通过 {len(results) - len(failed)}，失败 {len(failed)}")
    if failed:
        for _, name, detail in failed:
            print(f"  失败：{name}  {detail}")
        raise SystemExit(1)


if __name__ == "__main__":
    main()
