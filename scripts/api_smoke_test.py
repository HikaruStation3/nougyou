"""农产品 + 购物车模块接口冒烟测试（对应接口文档 API-040 ~ API-051）。

用法：
    1. 后端启动时关闭验证码（仅测试用）：
       mvn spring-boot:run -Dspring-boot.run.arguments="--nougyou.captcha.enabled=false"
    2. 运行：
       python scripts/api_smoke_test.py
       如需指定地址：API_BASE=http://localhost:8081 python scripts/api_smoke_test.py

脚本会自建一个测试商品与测试分类，结束时自动清理，不会污染演示数据。
"""

import json
import os
import urllib.error
import urllib.parse
import urllib.request

BASE = os.environ.get("API_BASE", "http://localhost:8080").rstrip("/")
PASSWORD = os.environ.get("API_PASSWORD", "123456")

PASS, FAIL = "PASS", "FAIL"
results = []


def call(method, path, token=None, body=None):
    """发起请求，返回 (HTTP 状态码, 响应体)。"""
    url = BASE + urllib.parse.quote(path, safe="/?=&")
    data = json.dumps(body).encode("utf-8") if body is not None else None
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
    _, res = call("POST", "/api/v1/auth/login", body={"username": username, "password": PASSWORD})
    if res and res.get("code") == "00000":
        return res["data"]["accessToken"]
    return None


def ok(res):
    """业务成功：code 为 00000。"""
    return bool(res) and res.get("code") == "00000"


def check(name, passed, detail=""):
    results.append((PASS if passed else FAIL, name, str(detail)))
    print(("[通过] " if passed else "[失败] ") + name + ("  " + str(detail) if detail else ""))


def main():
    buyer = login("buyer01")
    farmer = login("farmer01")
    admin = login("admin")
    check("登录：买家/商家/管理员", all([buyer, farmer, admin]),
          "buyer=%s farmer=%s admin=%s" % (bool(buyer), bool(farmer), bool(admin)))
    if not all([buyer, farmer, admin]):
        print("\n登录失败，请确认后端已关闭验证码：--nougyou.captcha.enabled=false")
        return 1

    pid = None
    cart_id = None
    cid = None
    try:
        # ---------- 分类 ----------
        st, res = call("GET", "/api/v1/categories?onlyEnabled=false")
        cats = (res or {}).get("data") or []
        check("API-040 分类树", ok(res) and len(cats) >= 6,
              "一级=%d 有二级=%s" % (len(cats), any(c.get("children") for c in cats)))

        # ---------- 商品列表 / 筛选 ----------
        cases = [
            ("", None), ("?categoryId=3", None), ("?keyword=红", None),
            ("?minPrice=20&maxPrice=60", None), ("?isPoverty=1", None),
            ("?sortBy=price_asc", "asc"), ("?sortBy=price_desc", "desc"),
            ("?sortBy=sale_desc", None), ("?pageSize=2&pageNum=2", None),
        ]
        for qs, order in cases:
            st, res = call("GET", "/api/v1/products" + qs)
            data = (res or {}).get("data") or []
            listed = data.get("list") if isinstance(data, dict) else data
            prices = [float(i.get("price") or 0) for i in (listed or [])]
            sorted_ok = True
            if order == "asc":
                sorted_ok = prices == sorted(prices)
            elif order == "desc":
                sorted_ok = prices == sorted(prices, reverse=True)
            check("API-041 列表" + (qs or "(默认)"), ok(res) and sorted_ok,
                  "共%s条 价格%s" % (data.get("total") if isinstance(data, dict) else len(listed), prices))

        # ---------- 详情 ----------
        st, res = call("GET", "/api/v1/products/1")
        d = (res or {}).get("data") or {}
        check("API-042 商品详情", ok(res) and d.get("name"),
              "%s 图集%d张" % (d.get("name"), len(d.get("images") or [])))
        st, res = call("GET", "/api/v1/products/99999")
        check("API-042 详情-不存在返回业务错误", not ok(res), (res or {}).get("msg"))

        # ---------- 权限 ----------
        st, res = call("GET", "/api/v1/farmer/products", token=buyer)
        check("买家访问商家接口 HTTP 403", st == 403, "http=%s" % st)
        st, res = call("GET", "/api/v1/cart")
        check("未登录访问购物车 HTTP 401", st == 401, "http=%s" % st)

        # ---------- 商家：我的商品 / 新增 ----------
        st, res = call("GET", "/api/v1/farmer/products", token=farmer)
        check("商家我的商品列表", ok(res), "共%s条" % ((res or {}).get("data") or {}).get("total"))

        body = {
            "categoryId": "1", "name": "接口自测商品", "cover": "/img/goods/g1.jpg",
            "images": ["/img/goods/g1.jpg"], "price": 19.9, "originalPrice": 29.9,
            "stock": 10, "unit": "袋", "weight": 0.5, "origin": "钟山镇湖亭村",
            "isPoverty": 1, "description": "<p>自测</p>", "shelfStatus": 1,
        }
        st, res = call("POST", "/api/v1/farmer/products", token=farmer, body=body)
        pid = ((res or {}).get("data") or {}).get("id")
        check("API-043 商家上架商品", ok(res) and pid, "id=%s" % pid)

        for bad, desc in [
            ({"name": "测试商品", "price": 0, "stock": 1, "unit": "袋", "categoryId": "1"}, "价格为0"),
            ({"name": "测试商品", "cover": "/img/goods/g1.jpg", "price": 10, "stock": 1,
              "unit": "袋", "categoryId": "9999"}, "分类不存在"),
            ({"name": "测试商品", "cover": "/img/goods/g1.jpg", "price": 10, "stock": 1,
              "unit": "袋"}, "缺分类"),
            ({"name": "x", "price": 10, "stock": 1, "unit": "袋", "categoryId": "1"}, "名称过短"),
        ]:
            st, res = call("POST", "/api/v1/farmer/products", token=farmer, body=bad)
            check("API-043 非法入参-" + desc, not ok(res), (res or {}).get("msg"))

        if pid:
            # ---------- 编辑 / 越权编辑 ----------
            st, res = call("PUT", "/api/v1/farmer/products/" + pid, token=farmer,
                           body={"price": 15.5, "stock": 20})
            d = (res or {}).get("data") or {}
            check("API-044 编辑商品", ok(res) and float(d.get("price") or 0) == 15.5,
                  "price=%s stock=%s" % (d.get("price"), d.get("stock")))

            farmer2 = login("farmer02")
            st, res = call("PUT", "/api/v1/farmer/products/" + pid, token=farmer2, body={"price": 1})
            check("API-044 跨商家编辑被拒", not ok(res), (res or {}).get("msg"))

            # ---------- 上架 / 下架 ----------
            st, res = call("PUT", "/api/v1/farmer/products/%s/shelf" % pid, token=farmer,
                           body={"shelfStatus": 0})
            check("API-045 下架商品", ok(res) and ((res or {}).get("data") or {}).get("shelfStatus") == 0)
            st, res = call("PUT", "/api/v1/farmer/products/%s/shelf" % pid, token=farmer,
                           body={"shelfStatus": 5})
            check("API-045 非法上下架状态被拒", not ok(res), (res or {}).get("msg"))

            st, res = call("POST", "/api/v1/cart", token=buyer, body={"productId": pid, "quantity": 1})
            check("API-048 下架商品不可加购", not ok(res), (res or {}).get("msg"))

            call("PUT", "/api/v1/farmer/products/%s/shelf" % pid, token=farmer, body={"shelfStatus": 1})

        # ---------- 购物车 ----------
        target = pid or "1"

        def add(pid_value, qty):
            st, res = call("POST", "/api/v1/cart", token=buyer,
                           body={"productId": pid_value, "quantity": qty})
            return (res or {}).get("data") or {}, res

        c, res = add(target, 2)
        cart_id = c.get("id")
        check("API-048 加入购物车", ok(res) and c.get("quantity") == 2,
              "数量=%s 小计=%s 店铺=%s" % (c.get("quantity"), c.get("subtotal"), c.get("shopName")))

        c, res = add(target, 1)
        check("API-048 重复加购累加", ok(res) and c.get("quantity") == 3, "数量=%s" % c.get("quantity"))

        _, res = add(target, 50)
        check("API-048 超库存被拦截", not ok(res), (res or {}).get("msg"))

        _, res = add(target, 999)
        check("API-048 单次上限 999 被拦截", not ok(res), (res or {}).get("msg"))

        st, res = call("PUT", "/api/v1/cart/" + str(cart_id), token=buyer,
                       body={"quantity": 3, "selected": 0})
        c = (res or {}).get("data") or {}
        check("API-049 改数量并取消勾选", ok(res) and c.get("selected") == 0,
              "数量=%s 勾选=%s" % (c.get("quantity"), c.get("selected")))

        st, res = call("PUT", "/api/v1/cart/" + str(cart_id), token=buyer, body={"quantity": 0})
        check("API-049 数量为0被拒", not ok(res), (res or {}).get("msg"))

        buyer2 = login("buyer02")
        st, res = call("PUT", "/api/v1/cart/" + str(cart_id), token=buyer2, body={"quantity": 1})
        check("API-049 跨用户改购物车被拒", not ok(res), (res or {}).get("msg"))

        st, res = call("GET", "/api/v1/cart", token=buyer)
        check("API-047 购物车列表", ok(res), "行数=%d" % len((res or {}).get("data") or []))

        st, res = call("DELETE", "/api/v1/cart", token=buyer, body={"ids": [int(cart_id)]})
        check("API-051 批量删除指定行", ok(res), (res or {}).get("msg"))
        cart_id = None

        st, res = call("DELETE", "/api/v1/cart", token=buyer)
        check("API-051 清空购物车", ok(res), (res or {}).get("msg"))
        st, res = call("GET", "/api/v1/cart", token=buyer)
        check("清空后购物车为空", ok(res) and len((res or {}).get("data") or []) == 0)

        # ---------- 删除商品 ----------
        if pid:
            st, res = call("DELETE", "/api/v1/farmer/products/" + pid, token=farmer)
            check("API-046 删除商品", ok(res), (res or {}).get("msg"))
            st, res = call("GET", "/api/v1/products/" + pid)
            check("删除后详情不可访问", not ok(res), (res or {}).get("msg"))
            pid = None

        # ---------- 管理端 ----------
        st, res = call("GET", "/api/v1/admin/products", token=admin)
        check("管理端商品列表", ok(res), "共%s条" % ((res or {}).get("data") or {}).get("total"))

        st, res = call("POST", "/api/v1/admin/products/categories", token=admin,
                       body={"name": "自测分类", "parentId": "0", "sort": 99, "status": 1})
        cid = ((res or {}).get("data") or {}).get("id")
        check("分类新增", ok(res) and cid, "id=%s" % cid)

        if cid:
            st, res = call("PUT", "/api/v1/admin/products/categories/" + cid, token=admin,
                           body={"name": "自测分类-改", "status": 0})
            check("分类编辑", ok(res), ((res or {}).get("data") or {}).get("name"))
            st, res = call("DELETE", "/api/v1/admin/products/categories/" + cid, token=admin)
            check("分类删除", ok(res), (res or {}).get("msg"))
            cid = None

        # ---------- 二级分类约束 ----------
        st, res = call("POST", "/api/v1/admin/products/categories", token=admin,
                       body={"name": "非法父级", "parentId": "9999", "sort": 1, "status": 1})
        check("分类父级不存在被拒", not ok(res), (res or {}).get("msg"))
    finally:
        # 清理测试数据，避免污染演示环境
        if pid:
            call("DELETE", "/api/v1/farmer/products/" + str(pid), token=farmer)
        if cid:
            call("DELETE", "/api/v1/admin/products/categories/" + str(cid), token=admin)
        call("DELETE", "/api/v1/cart", token=buyer)

    total = len(results)
    passed = sum(1 for r in results if r[0] == PASS)
    print("\n合计 %d 项，通过 %d 项，失败 %d 项" % (total, passed, total - passed))
    return 0 if passed == total else 2


if __name__ == "__main__":
    raise SystemExit(main())
