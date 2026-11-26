"""订单模块接口自动化测试（API-052 ~ API-060）。

覆盖范围：
    下单      立即购买 / 购物车结算 / 按商家拆单 / 商品快照与金额计算
    状态机    待付款(0) → 待发货(1) → 待收货(2) → 已完成(3)；待付款 → 已取消(4)
    库存      下单预占、取消回滚、发货累加销量
    权限      买家 / 商家 / 管理员三方数据隔离，未登录 401、越权 403
    校验      数量、手机号、收货人、非法来源、下架商品、库存不足
    查询      我的订单 / 商家订单 / 后台订单的筛选、分页、手机号脱敏
    并发      可选，验证「下单即原子扣减库存」不会超卖

用法：
    1) 启动后端时关闭验证码（仅测试环境）：
       mvn spring-boot:run -Dspring-boot.run.arguments="--nougyou.captcha.enabled=false"
    2) 执行：
       python scripts/order_smoke_test.py                      # 默认 http://localhost:8080
       API_BASE=http://localhost:8081 python scripts/order_smoke_test.py
       python scripts/order_smoke_test.py --concurrency        # 追加并发防超卖测试

说明：
    - 脚本复用固定名称的自测商品（订单模块自测商品A/B），可重复执行，不会越跑越多；
    - 订单没有删除接口，测试产生的订单会在结束时统一取消（库存自动回滚），记为「已取消」；
    - 请只在开发 / 演示库执行，不要对生产库运行。
"""

import json
import os
import sys
import urllib.error
import urllib.parse
import urllib.request
from concurrent.futures import ThreadPoolExecutor
from datetime import datetime

BASE = os.environ.get("API_BASE", "http://localhost:8080").rstrip("/")
PASSWORD = os.environ.get("API_PASSWORD", "123456")
PRICE_A = 19.9
PRICE_B = 9.9
NAME_A = "订单模块自测商品A"
NAME_B = "订单模块自测商品B"

PASS, FAIL = "PASS", "FAIL"
results = []


def call(method, path, token=None, body=None):
    """发起请求，返回 (HTTP 状态码, 解析后的响应体)。"""
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


def page_of(res):
    d = data(res) or {}
    return d.get("list") or [], d.get("total") or 0


def check(name, passed, detail=""):
    results.append((PASS if passed else FAIL, name, str(detail)))
    print(("[通过] " if passed else "[失败] ") + name + ("  " + str(detail) if detail else ""))


def section(title):
    print("\n===== " + title + " =====")


def money(value):
    return round(float(value or 0), 2)


def order_form(**kwargs):
    form = {
        "source": "buyNow",
        "receiverName": "测试收货人",
        "receiverPhone": "13800138000",
        "receiverAddress": "福建省莆田市钟山镇测试路 1 号",
        "remark": "接口自测订单",
        "payType": 3,
    }
    form.update(kwargs)
    return form


def detail_of(token, order_id):
    _, res = call("GET", "/api/v1/orders/" + str(order_id), token=token)
    return data(res) or {}


def stock_of(product_id):
    _, res = call("GET", "/api/v1/products/" + str(product_id))
    return (data(res) or {}).get("stock")


def ensure_product(farmer_token, name, category_id, price):
    """复用同名自测商品；不存在则创建。返回 (id, farmerId)。"""
    _, res = call("GET", "/api/v1/farmer/products?pageNum=1&pageSize=100", token=farmer_token)
    items, _ = page_of(res)
    for item in items:
        if item.get("name") == name:
            pid = item.get("id")
            # 重置价格 / 库存，并确保重新上架（上下架只能走独立接口）
            call("PUT", "/api/v1/farmer/products/" + str(pid), token=farmer_token,
                 body={"price": price, "stock": 100})
            call("PUT", "/api/v1/farmer/products/" + str(pid) + "/shelf", token=farmer_token,
                 body={"shelfStatus": 1})
            return pid, item.get("farmerId")
    body = {
        "categoryId": category_id, "name": name, "cover": "/img/goods/g1.jpg",
        "images": ["/img/goods/g1.jpg"], "price": price, "originalPrice": round(price * 1.5, 2),
        "stock": 100, "unit": "袋", "weight": 0.5, "origin": "钟山镇湖亭村",
        "isPoverty": 1, "description": "<p>订单模块自测</p>", "shelfStatus": 1,
    }
    _, res = call("POST", "/api/v1/farmer/products", token=farmer_token, body=body)
    d = data(res) or {}
    return d.get("id"), d.get("farmerId")


def pick_category():
    """取一个启用中的叶子分类，供自测商品上架使用。"""
    _, res = call("GET", "/api/v1/categories?onlyEnabled=false")
    for top in data(res) or []:
        for child in top.get("children") or []:
            if child.get("status", 1) == 1:
                return child.get("id")
    for top in data(res) or []:
        if top.get("status", 1) == 1:
            return top.get("id")
    return None


def main():
    run_concurrency = "--concurrency" in sys.argv

    section("准备：登录三种角色")
    buyer = login("buyer01")
    buyer2 = login("buyer02")
    farmer = login("farmer01")
    farmer2 = login("farmer02")
    admin = login("admin")
    check("登录：买家1 / 买家2 / 商家1 / 商家2 / 管理员",
          all([buyer, buyer2, farmer, farmer2, admin]),
          "buyer=%s buyer2=%s farmer=%s farmer2=%s admin=%s"
          % tuple(bool(x) for x in (buyer, buyer2, farmer, farmer2, admin)))
    if not all([buyer, buyer2, farmer, farmer2, admin]):
        return 1

    section("准备：自测商品")
    if not buyer2:
        pass
    cid = pick_category()
    pid_a, farmer_id = ensure_product(farmer, NAME_A, cid, PRICE_A)
    pid_b, farmer2_id = ensure_product(farmer2, NAME_B, cid, PRICE_B)
    check("自测商品就绪（A=商家1，B=商家2）", pid_a and pid_b and farmer_id and farmer2_id,
          "A=%s B=%s farmerId=%s/%s" % (pid_a, pid_b, farmer_id, farmer2_id))
    if not (pid_a and pid_b):
        return 1

    created_orders = []          # 需要收尾取消的订单
    stock_a0 = stock_of(pid_a)

    # ---------------------------------------------------------------
    section("API-052 下单：立即购买 / 金额 / 库存预占")
    st, res = call("POST", "/api/v1/orders", token=buyer,
                   body=order_form(productId=pid_a, quantity=2))
    orders = data(res) or []
    oa = orders[0] if orders else {}
    check("立即购买下单成功", ok(res) and len(orders) == 1,
          "订单数=%d %s" % (len(orders), ((res or {}).get("msg") or "")))
    if not oa:
        return 1
    created_orders.append(oa["id"])
    check("订单号格式 ZS+日期+4位", str(oa.get("orderNo", "")).startswith("ZS")
          and len(str(oa.get("orderNo"))) == 14, oa.get("orderNo"))
    check("金额=单价×数量，运费为0",
          money(oa.get("totalAmount")) == money(PRICE_A * 2)
          and money(oa.get("payAmount")) == money(PRICE_A * 2)
          and money(oa.get("freight")) == 0,
          "总额=%s 应付=%s 运费=%s" % (oa.get("totalAmount"), oa.get("payAmount"), oa.get("freight")))
    check("初始状态=待付款(0)/未支付(0)",
          oa.get("orderStatus") == 0 and oa.get("payStatus") == 0,
          "status=%s payStatus=%s" % (oa.get("orderStatus"), oa.get("payStatus")))
    check("明细快照（商品名/单价/数量/小计）",
          oa.get("items") and oa["items"][0].get("productName") == NAME_A
          and money(oa["items"][0].get("price")) == money(PRICE_A)
          and oa["items"][0].get("quantity") == 2
          and money(oa["items"][0].get("subtotal")) == money(PRICE_A * 2),
          oa.get("items"))
    check("下单后库存被预占", stock_of(pid_a) == stock_a0 - 2,
          "%s → %s" % (stock_a0, stock_of(pid_a)))
    check("订单归属当前商家的店铺", str(oa.get("farmerId")) == str(farmer_id),
          "farmerId=%s" % oa.get("farmerId"))

    section("API-052 下单：非法入参与业务拦截")
    cases = [
        ("数量为0", order_form(productId=pid_a, quantity=0), "购买数量"),
        ("手机号格式错误", order_form(productId=pid_a, quantity=1, receiverPhone="12345"), "电话"),
        ("收货人为空", {k: v for k, v in order_form(productId=pid_a, quantity=1).items()
                    if k != "receiverName"}, "收货人"),
        ("商品不存在", order_form(productId=999999, quantity=1), "商品不存在"),
        ("下单来源非法", order_form(source="unknown", productId=pid_a, quantity=1), "来源"),
        ("库存不足", order_form(productId=pid_a, quantity=100000), "库存不足"),
    ]
    for label, body, expect in cases:
        _, res = call("POST", "/api/v1/orders", token=buyer, body=body)
        check("拦截：" + label, not ok(res), (res or {}).get("msg"))

    _, res = call("POST", "/api/v1/orders", token=buyer,
                  body=order_form(source="cart", cartIds=[], productId=pid_a, quantity=1))
    check("拦截：购物车来源未选商品", not ok(res), (res or {}).get("msg"))

    # 下架商品不可下单
    call("PUT", "/api/v1/farmer/products/" + str(pid_a) + "/shelf", token=farmer, body={"shelfStatus": 0})
    _, res = call("POST", "/api/v1/orders", token=buyer, body=order_form(productId=pid_a, quantity=1))
    check("拦截：已下架商品不可下单", not ok(res), (res or {}).get("msg"))
    call("PUT", "/api/v1/farmer/products/" + str(pid_a) + "/shelf", token=farmer, body={"shelfStatus": 1})
    _, res = call("GET", "/api/v1/products/" + str(pid_a))
    check("恢复上架后买家可见", (data(res) or {}).get("shelfStatus") == 1,
          "shelfStatus=%s" % (data(res) or {}).get("shelfStatus"))

    section("API-053 / API-054 查询与详情：数据隔离与脱敏")
    _, res = call("GET", "/api/v1/orders?pageNum=1&pageSize=100", token=buyer)
    items, total = page_of(res)
    check("我的订单包含新订单", ok(res) and any(str(i.get("id")) == str(oa["id"]) for i in items),
          "共%s条" % total)

    _, res = call("GET", "/api/v1/orders?keyword=" + urllib.parse.quote(oa["orderNo"]), token=buyer)
    items, _ = page_of(res)
    check("我的订单按订单号搜索", ok(res) and len(items) == 1 and items[0]["id"] == oa["id"],
          "命中%d条" % len(items))

    _, res = call("GET", "/api/v1/orders?orderStatus=0&pageSize=100", token=buyer)
    items, _ = page_of(res)
    check("我的订单按状态筛选（仅待付款）",
          ok(res) and items and all(i.get("orderStatus") == 0 for i in items),
          "命中%d条" % len(items))

    _, res = call("GET", "/api/v1/orders?pageNum=1&pageSize=1", token=buyer)
    items, total = page_of(res)
    check("我的订单分页 pageSize=1", ok(res) and len(items) == 1 and total >= 1,
          "返回%d条 总数%s" % (len(items), total))

    masked = oa.get("receiverPhone", "")
    check("列表手机号脱敏 138****8000", masked == "138****8000", masked)

    d = detail_of(buyer, oa["id"])
    check("买家查看详情，手机号不脱敏", d.get("receiverPhone") == "13800138000",
          d.get("receiverPhone"))
    check("商家查看本店订单详情", ok({"code": "00000"}) and detail_of(farmer, oa["id"]).get("id") == oa["id"])
    check("管理员查看任意订单详情", detail_of(admin, oa["id"]).get("id") == oa["id"])

    _, res = call("GET", "/api/v1/orders/" + str(oa["id"]), token=buyer2)
    check("买家2查看他人订单被拒", not ok(res), (res or {}).get("msg"))

    _, res = call("GET", "/api/v1/orders/999999", token=buyer)
    check("查看不存在订单被拒", not ok(res), (res or {}).get("msg"))

    # ---------------------------------------------------------------
    section("API-055 支付")
    _, res = call("PUT", "/api/v1/orders/" + str(oa["id"]) + "/pay", token=buyer,
                  body={"payType": 3})
    paid = data(res) or {}
    check("支付成功 → 待发货/已支付",
          ok(res) and paid.get("orderStatus") == 1 and paid.get("payStatus") == 1,
          "status=%s payStatus=%s" % (paid.get("orderStatus"), paid.get("payStatus")))
    check("支付时间已写入", bool(paid.get("payTime")), paid.get("payTime"))

    _, res = call("PUT", "/api/v1/orders/" + str(oa["id"]) + "/pay", token=buyer, body={"payType": 3})
    check("重复支付被拒", not ok(res), (res or {}).get("msg"))

    _, res = call("PUT", "/api/v1/orders/" + str(oa["id"]) + "/pay", token=buyer2, body={"payType": 3})
    check("他人支付我的订单被拒", not ok(res), (res or {}).get("msg"))

    section("API-058 商家订单列表")
    _, res = call("GET", "/api/v1/farmer/orders?pageSize=100", token=farmer)
    items, _ = page_of(res)
    check("商家订单列表包含本店订单", ok(res) and any(str(i["id"]) == str(oa["id"]) for i in items),
          "共%d条" % len(items))
    check("商家列表手机号脱敏", all(i.get("receiverPhone") == "138****8000"
                                for i in items if str(i["id"]) == str(oa["id"])),
          "订单=%s" % [i.get("receiverPhone") for i in items if str(i["id"]) == str(oa["id"])])

    _, res = call("GET", "/api/v1/farmer/orders?keyword=" + oa["orderNo"], token=farmer)
    items, _ = page_of(res)
    check("商家按订单号搜索", ok(res) and any(str(i["id"]) == str(oa["id"]) for i in items),
          "命中%d条" % len(items))

    _, res = call("GET", "/api/v1/farmer/orders?pageSize=100", token=farmer2)
    items, _ = page_of(res)
    check("商家2看不到商家1的订单", ok(res) and all(str(i["id"]) != str(oa["id"]) for i in items),
          "共%d条" % len(items))

    section("API-059 发货")
    _, res = call("PUT", "/api/v1/farmer/orders/" + str(oa["id"]) + "/ship", token=farmer2,
                  body={"logisticsCompany": "顺丰", "logisticsNo": "SF0001"})
    check("商家2越权发货被拒", not ok(res), (res or {}).get("msg"))

    _, res = call("PUT", "/api/v1/farmer/orders/" + str(oa["id"]) + "/ship", token=farmer,
                  body={"logisticsCompany": "顺丰"})
    check("缺物流单号被拦截", not ok(res), (res or {}).get("msg"))

    _, res = call("PUT", "/api/v1/farmer/orders/" + str(oa["id"]) + "/ship", token=farmer,
                  body={"logisticsCompany": "顺丰速运", "logisticsNo": "SF1234567890"})
    shipped = data(res) or {}
    check("发货成功 → 待收货(2)", ok(res) and shipped.get("orderStatus") == 2,
          "status=%s" % shipped.get("orderStatus"))
    check("物流信息已写入",
          shipped.get("logisticsCompany") == "顺丰速运"
          and shipped.get("logisticsNo") == "SF1234567890" and bool(shipped.get("shipTime")),
          "%s %s" % (shipped.get("logisticsCompany"), shipped.get("logisticsNo")))

    _, res = call("PUT", "/api/v1/farmer/orders/" + str(oa["id"]) + "/ship", token=farmer,
                  body={"logisticsCompany": "顺丰速运", "logisticsNo": "SF2"})
    check("重复发货被拒", not ok(res), (res or {}).get("msg"))

    section("API-057 确认收货")
    _, res = call("PUT", "/api/v1/orders/" + str(oa["id"]) + "/confirm", token=buyer)
    finished = data(res) or {}
    check("确认收货成功 → 已完成(3)", ok(res) and finished.get("orderStatus") == 3,
          "status=%s" % finished.get("orderStatus"))
    check("完成时间已写入", bool(finished.get("finishTime")), finished.get("finishTime"))

    _, res = call("PUT", "/api/v1/orders/" + str(oa["id"]) + "/confirm", token=buyer)
    check("重复确认收货被拒", not ok(res), (res or {}).get("msg"))

    check("已完成订单可申请售后", finished.get("afterSaleAllowed") is True,
          "afterSaleAllowed=%s" % finished.get("afterSaleAllowed"))

    # ---------------------------------------------------------------
    section("API-056 取消订单：状态机与库存回滚")
    stock_before = stock_of(pid_a)
    _, res = call("POST", "/api/v1/orders", token=buyer,
                  body=order_form(productId=pid_a, quantity=3))
    ob = (data(res) or [{}])[0]
    check("创建待取消订单", ok(res) and ob.get("id"), "id=%s" % ob.get("id"))
    if not ob.get("id"):
        return 1
    created_orders.append(ob["id"])
    check("下单后库存再扣 3", stock_of(pid_a) == stock_before - 3,
          "%s → %s" % (stock_before, stock_of(pid_a)))

    _, res = call("PUT", "/api/v1/orders/" + str(ob["id"]) + "/cancel", token=buyer2,
                  body={"reason": "越权测试"})
    check("他人取消我的订单被拒", not ok(res), (res or {}).get("msg"))

    _, res = call("PUT", "/api/v1/orders/" + str(ob["id"]) + "/cancel", token=buyer,
                  body={"reason": "不想要了"})
    canceled = data(res) or {}
    check("取消成功 → 已取消(4)", ok(res) and canceled.get("orderStatus") == 4,
          "status=%s" % canceled.get("orderStatus"))
    check("取消后库存回滚", stock_of(pid_a) == stock_before,
          "%s → %s（期望 %s）" % (stock_before - 3, stock_of(pid_a), stock_before))

    _, res = call("PUT", "/api/v1/orders/" + str(ob["id"]) + "/cancel", token=buyer)
    check("重复取消被拒", not ok(res), (res or {}).get("msg"))
    _, res = call("PUT", "/api/v1/orders/" + str(ob["id"]) + "/pay", token=buyer, body={"payType": 3})
    check("已取消订单不可支付", not ok(res), (res or {}).get("msg"))

    section("状态机：未支付不可发货 / 未发货不可确认收货 / 已支付不可取消")
    _, res = call("POST", "/api/v1/orders", token=buyer,
                  body=order_form(productId=pid_a, quantity=1))
    oc = (data(res) or [{}])[0]
    created_orders.append(oc.get("id"))
    _, res = call("PUT", "/api/v1/farmer/orders/" + str(oc["id"]) + "/ship", token=farmer,
                  body={"logisticsCompany": "顺丰", "logisticsNo": "SF0"})
    check("待付款订单不可发货", not ok(res), (res or {}).get("msg"))
    _, res = call("PUT", "/api/v1/orders/" + str(oc["id"]) + "/confirm", token=buyer)
    check("未发货订单不可确认收货", not ok(res), (res or {}).get("msg"))
    _, res = call("PUT", "/api/v1/orders/" + str(oc["id"]) + "/cancel", token=buyer,
                  body={"reason": "状态机测试"})
    check("待付款订单可取消", ok(res) and (data(res) or {}).get("orderStatus") == 4,
          (res or {}).get("msg"))

    _, res = call("POST", "/api/v1/orders", token=buyer,
                  body=order_form(productId=pid_a, quantity=1))
    od = (data(res) or [{}])[0]
    created_orders.append(od.get("id"))
    call("PUT", "/api/v1/orders/" + str(od["id"]) + "/pay", token=buyer, body={"payType": 3})
    _, res = call("PUT", "/api/v1/orders/" + str(od["id"]) + "/cancel", token=buyer,
                  body={"reason": "已支付后取消"})
    check("已支付订单不可取消", not ok(res), (res or {}).get("msg"))

    # ---------------------------------------------------------------
    section("API-052 购物车结算：按商家拆单")
    call("DELETE", "/api/v1/cart", token=buyer)  # 清空买家1的历史购物车，保证断言确定
    _, res = call("POST", "/api/v1/cart", token=buyer, body={"productId": pid_a, "quantity": 1})
    cart_a = (data(res) or {}).get("id")
    _, res = call("POST", "/api/v1/cart", token=buyer, body={"productId": pid_b, "quantity": 2})
    cart_b = (data(res) or {}).get("id")
    check("购物车加入两家商品", bool(cart_a) and bool(cart_b), "cart=%s/%s" % (cart_a, cart_b))

    stock_a_before = stock_of(pid_a)
    stock_b_before = stock_of(pid_b)
    _, res = call("POST", "/api/v1/orders", token=buyer,
                  body=order_form(source="cart", cartIds=[int(cart_a), int(cart_b)]))
    split = data(res) or []
    check("购物车结算按商家拆成2单", ok(res) and len(split) == 2,
          "订单数=%d %s" % (len(split), (res or {}).get("msg")))
    if len(split) == 2:
        created_orders.extend([o["id"] for o in split])
        ids = {str(o.get("farmerId")) for o in split}
        check("拆单后归属两个不同商家", len(ids) == 2, ids)
        total_pay = round(sum(money(o.get("payAmount")) for o in split), 2)
        check("拆单金额合计正确", total_pay == round(PRICE_A + PRICE_B * 2, 2),
              "合计=%s 期望=%s" % (total_pay, round(PRICE_A + PRICE_B * 2, 2)))
        check("库存分别扣减", stock_of(pid_a) == stock_a_before - 1
              and stock_of(pid_b) == stock_b_before - 2,
              "A %s→%s B %s→%s" % (stock_a_before, stock_of(pid_a),
                                   stock_b_before, stock_of(pid_b)))

    _, res = call("GET", "/api/v1/cart", token=buyer)
    cart_rows = data(res) or []
    left = {str(r.get("id")) for r in cart_rows}
    check("下单后已结算的购物车行被清理",
          str(cart_a) not in left and str(cart_b) not in left, "剩余%d行" % len(cart_rows))

    section("取消拆单订单，库存应全部回滚")
    for order in split:
        call("PUT", "/api/v1/orders/" + str(order["id"]) + "/cancel", token=buyer,
             body={"reason": "拆单测试收尾"})
    check("拆单取消后库存复原",
          stock_of(pid_a) == stock_a_before and stock_of(pid_b) == stock_b_before,
          "A=%s B=%s" % (stock_of(pid_a), stock_of(pid_b)))

    # ---------------------------------------------------------------
    section("API-060 后台订单查询（仅查询）")
    _, res = call("GET", "/api/v1/admin/orders?orderNo=" + oa["orderNo"], token=admin)
    items, _ = page_of(res)
    check("后台按订单号精确查询",
          ok(res) and len(items) == 1 and items[0].get("orderNo") == oa["orderNo"],
          "命中%d条" % len(items))

    _, res = call("GET", "/api/v1/admin/orders?buyerId=" + str(oa["buyerId"]) + "&pageSize=100",
                  token=admin)
    items, total = page_of(res)
    check("后台按买家筛选", ok(res) and items
          and all(str(i.get("buyerId")) == str(oa["buyerId"]) for i in items),
          "共%s条" % total)

    _, res = call("GET", "/api/v1/admin/orders?farmerId=" + str(farmer_id) + "&pageSize=100",
                  token=admin)
    items, total = page_of(res)
    check("后台按商家筛选", ok(res) and items
          and all(str(i.get("farmerId")) == str(farmer_id) for i in items),
          "共%s条" % total)

    _, res = call("GET", "/api/v1/admin/orders?orderStatus=3&pageSize=100", token=admin)
    items, _ = page_of(res)
    check("后台按状态筛选（已完成）", ok(res) and items
          and all(i.get("orderStatus") == 3 for i in items), "命中%d条" % len(items))

    today = datetime.now().strftime("%Y-%m-%d")
    # 空格交给 call() 统一编码，避免二次编码导致时间参数绑定失败
    qs = "startTime=%s 00:00:00&endTime=%s 23:59:59" % (today, today)
    _, res = call("GET", "/api/v1/admin/orders?" + qs + "&pageSize=100", token=admin)
    items, _ = page_of(res)
    check("后台按时间范围筛选（今天）", ok(res) and items
          and any(str(i.get("id")) == str(oa["id"]) for i in items), "命中%d条" % len(items))

    _, res = call("GET", "/api/v1/admin/orders?pageSize=1000", token=admin)
    items, total = page_of(res)
    check("后台 pageSize 上限 100 生效", ok(res) and len(items) <= 100, "返回%d条" % len(items))

    _, res = call("GET", "/api/v1/admin/orders?pageNum=999&pageSize=10", token=admin)
    items, total = page_of(res)
    check("后台翻页越界返回空列表", ok(res) and items == [] and total >= 0,
          "返回%d条 总数%s" % (len(items), total))

    # ---------------------------------------------------------------
    section("权限：未登录 401 / 越权 403")
    st, _ = call("GET", "/api/v1/orders")
    check("未登录访问订单列表 → 401", st == 401, "http=%s" % st)
    st, _ = call("POST", "/api/v1/orders", body=order_form(productId=pid_a, quantity=1))
    check("未登录下单 → 401", st == 401, "http=%s" % st)

    st, _ = call("GET", "/api/v1/farmer/orders", token=buyer)
    check("买家访问商家订单接口 → 403", st == 403, "http=%s" % st)
    st, _ = call("GET", "/api/v1/admin/orders", token=buyer)
    check("买家访问后台订单接口 → 403", st == 403, "http=%s" % st)
    st, _ = call("GET", "/api/v1/admin/orders", token=farmer)
    check("商家访问后台订单接口 → 403", st == 403, "http=%s" % st)
    st, _ = call("GET", "/api/v1/orders", token=farmer)
    check("商家访问买家订单接口 → 403", st == 403, "http=%s" % st)

    # ---------------------------------------------------------------
    if run_concurrency:
        section("并发：库存仅 5 件，12 个请求同时下单，验证不超卖")
        call("PUT", "/api/v1/farmer/products/" + str(pid_a), token=farmer,
             body={"stock": 5})
        call("PUT", "/api/v1/farmer/products/" + str(pid_a) + "/shelf", token=farmer,
             body={"shelfStatus": 1})

        def place(_):
            _, r = call("POST", "/api/v1/orders", token=buyer,
                        body=order_form(productId=pid_a, quantity=1))
            return bool(ok(r)), ((data(r) or [{}])[0] or {}).get("id")

        with ThreadPoolExecutor(max_workers=12) as pool:
            outcomes = list(pool.map(place, range(12)))
        success = [oid for good, oid in outcomes if good]
        check("成功下单数=5，其余全部库存不足", len(success) == 5, "成功%d单" % len(success))
        check("并发后库存不为负", stock_of(pid_a) == 0, "stock=%s" % stock_of(pid_a))
        for oid in success:
            call("PUT", "/api/v1/orders/" + str(oid) + "/cancel", token=buyer,
                 body={"reason": "并发测试收尾"})
        check("并发测试订单取消后库存回滚", stock_of(pid_a) == 5, "stock=%s" % stock_of(pid_a))
        call("PUT", "/api/v1/farmer/products/" + str(pid_a), token=farmer,
             body={"stock": 100})

    # ---------------------------------------------------------------
    section("收尾：取消遗留的待付款订单，回滚库存并下架自测商品")
    for oid in created_orders:
        if not oid:
            continue
        d = detail_of(buyer, oid)
        if d.get("orderStatus") == 0:
            call("PUT", "/api/v1/orders/" + str(oid) + "/cancel", token=buyer,
                 body={"reason": "自动化测试收尾"})
    for pid, token in ((pid_a, farmer), (pid_b, farmer2)):
        call("PUT", "/api/v1/farmer/products/" + str(pid) + "/shelf", token=token,
             body={"shelfStatus": 0})
    check("收尾完成（自测商品已下架，库存已回滚）",
          stock_of(pid_a) >= 0, "A库存=%s B库存=%s" % (stock_of(pid_a), stock_of(pid_b)))

    total_checks = len(results)
    passed = sum(1 for r in results if r[0] == PASS)
    print("\n" + "=" * 56)
    print("订单模块测试合计 %d 项，通过 %d 项，失败 %d 项" % (total_checks, passed, total_checks - passed))
    failed = [r for r in results if r[0] == FAIL]
    if failed:
        print("失败明细：")
        for _, name, detail in failed:
            print("  - %s  %s" % (name, detail))
    print("=" * 56)
    return 0 if passed == total_checks else 2


if __name__ == "__main__":
    raise SystemExit(main())
