# 订单模块接口测试手册（API-052 ~ API-060）

> 适用版本：当前 `backend` 主线代码（含店铺名字段）。
> 实测环境：`http://localhost:8081`（免图形验证码的测试实例，与 8080 共用同一个 `nougyou` 库）。
> 配套文件：`doc/order_api_collection.postman.json`，导入 Postman 后可自动串起整条链路。

---

## 0. 测试前准备

### 0.1 环境与端口

| 用途 | 地址 | 说明 |
|---|---|---|
| 接口测试（推荐） | `http://localhost:8081` | 关闭图形验证码，登录只需账号密码 |
| 页面演示 | `http://localhost:8080` | 开启验证码，用 3100 门户 / 3000 管理端操作 |
| 用户门户 | `http://localhost:3100` | 买家、商家 |
| 管理后台 | `http://localhost:3000` | 管理员 |

如果 8081 没在运行，在 `backend` 目录执行（PowerShell）：

```powershell
$env:JAVA_HOME = "D:\Program Files\Java\jdk-21.0.13.1"
$env:SERVER_PORT = "8081"
& "D:\Program Files\Apache\maven\bin\mvn.cmd" -o "-Dmaven.repo.local=D:\dev\m2repo" spring-boot:run "-Dspring-boot.run.arguments=--nougyou.captcha.enabled=false"
```

> 如果你的 Maven 仓库不在 `D:\dev\m2repo`，把该参数换成自己的路径；网络可用时也可以去掉 `-o`（离线模式）。

> 接口路径同时注册了 `/api/v1/...` 和 `/api/...`，两种写法都能通。本手册统一用 `/api/v1`。

### 0.2 测试账号

| 角色 | 账号 | 密码 | userId | 店铺名 |
|---|---|---|---|---|
| 买家 1 | `buyer01` | `123456` | 1 | —（部门为“商户中心”） |
| 买家 2（越权对照） | `buyer02` | `123456` | 2 | — |
| 商家 1 | `farmer01` | `123456` | 4 | 绿野农场 |
| 商家 2（越权对照） | `farmer02` | `123456` | 5 | 鲜果合作社 |
| 管理员 | `admin` | `123456` | 11 | — |

### 0.3 测试商品

| productId | 商品 | 单价 | 店铺 | 备注 |
|---|---|---|---|---|
| `1` | 钟山手工兴化米粉（2斤装） | 28.00 | 绿野农场 | 自营，适合做主流程 |
| `8` | 钟山高山绿茶（250g） | 76.00 | 鲜果合作社 | 自营，适合做拆单第二单 |
| `6` | 钟山笋干（250g） | 32.00 | 绿野农场 | **注意：处于委托代销，订单实际归鲜果合作社**，不要拿它验证拆单 |

> 拆单验证请用 `1 + 8`。用 `6` 会和绿茶合并成一张单，不是接口出错。

### 0.4 获取 Token

**方式 A（推荐，8081）**：

```bash
curl -s -X POST http://localhost:8081/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"buyer01","password":"123456"}'
```

返回：

```json
{
  "code": "00000",
  "msg": "一切ok",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "refreshToken": "...",
    "tokenType": "Bearer",
    "expiresIn": 7200
  }
}
```

后续请求统一加：

```
Authorization: Bearer <accessToken>
```

**方式 B（8080）**：在 3100 门户登录后按 F12 → Application → Session Storage → `portal_token`，复制值即可。

> Token 有效期 2 小时，过期重新登录。

### 0.5 统一响应与状态码

```json
{ "code": "00000", "msg": "一切ok", "data": {} }
```

| 场景 | HTTP | code | 说明 |
|---|---|---|---|
| 成功 | 200 | `00000` | `data` 为业务数据 |
| 业务校验失败 | 200 | `A0400` | 库存不足、状态不允许等 |
| 数据越权 | 200 | `A0301` | 能进接口，但数据不属于你 |
| 方法级越权 | 403 | `A0301` | 角色没有该权限点，被 `@PreAuthorize` 拦下 |
| 未登录 / Token 失效 | 401 | `A0230` | 请求头缺失或过期 |
| 资源不存在 | 200 | `A0404` | 如订单 ID 不存在 |

> 注意区分：**403 是权限点不够，200 + A0301 是数据归属不对**，测试时要分别断言。

### 0.6 分页与时间

- 分页参数：`pageNum` 从 1 开始，`pageSize` 默认 10、最大 100（传 1000 也只返回 100 条）。
- 时间格式：`yyyy-MM-dd HH:mm:ss`，URL 里空格要编码成 `%20`（用 Postman 的 Params 面板会自动处理）。

---

## 1. API-052 提交订单

**请求**

```
POST /api/v1/orders
Authorization: Bearer {{buyerToken}}
Content-Type: application/json
```

**两种下单来源**

| source | 必填参数 | 说明 |
|---|---|---|
| `buyNow` | `productId`、`quantity` | 立即购买，单商品 |
| `cart` | `cartIds`（数组） | 购物车结算，可多商品、可跨店 |

**公共收货参数**

| 参数 | 必填 | 约束 |
|---|---|---|
| `receiverName` | 是 | 2–20 字 |
| `receiverPhone` | 是 | `^1\d{10}$` |
| `receiverAddress` | 是 | ≤200 字 |
| `remark` | 否 | ≤200 字 |
| `payType` | 否 | 1 微信 / 2 支付宝 / 3 模拟，默认 3 |

**请求体（立即购买）**

```json
{
  "source": "buyNow",
  "productId": 1,
  "quantity": 2,
  "receiverName": "沈明",
  "receiverPhone": "13800138000",
  "receiverAddress": "莆田市仙游县钟山镇湖亭村 3 号",
  "remark": "测试下单",
  "payType": 3
}
```

**必须核对的结果**

1. HTTP 200，`code = 00000`，`data` 是**数组**（不是单个对象）。
2. 单商家下单只返回 1 张订单；`orderStatus = 0`、`payStatus = 0`、`payTime = null`。
3. `freight = 0`，`payAmount = totalAmount = 28 × 2 = 56`。
4. 列表返回的 `receiverPhone` 已脱敏为 `138****8000`。
5. `items[0]` 的 `price = 28`、`quantity = 2`、`subtotal = 56`，`entrustId = null`（自营）。
6. 再查 `GET /api/v1/products/1`，库存比下单前**减少 2**（下单即预占）。

**实测响应（已裁剪）**

```json
{
  "code": "00000",
  "data": [{
    "id": "51",
    "orderNo": "ZS202611263264",
    "buyerName": "小娟",
    "farmerId": "4",
    "farmerName": "吴叔山货铺",
    "shopName": "绿野农场",
    "totalAmount": 56.0,
    "freight": 0,
    "payAmount": 56.0,
    "payStatus": 0,
    "payType": 3,
    "orderStatus": 0,
    "receiverPhone": "138****8000",
    "logisticsCompany": null,
    "logisticsNo": null,
    "shipTime": null,
    "finishTime": null,
    "afterSaleAllowed": false,
    "items": [{
      "productId": "1",
      "productName": "钟山手工兴化米粉（2斤装）",
      "price": 28.0,
      "quantity": 2,
      "subtotal": 56.0,
      "entrustId": null
    }]
  }]
}
```

> 把 `data[0].id` 和 `orderNo` 记下来，后面 053~060 都要用。

**反向用例（逐条都应失败）**

| 输入 | 预期 code / msg |
|---|---|
| `quantity = 0` | `A0400` 购买数量至少为 1 |
| `receiverPhone = 12345` | `A0400` 收货电话格式不正确 |
| `receiverName = 沈` | `A0400` 收货人姓名长度需在 2-20 个字符之间 |
| `source = abc` | `A0400` 下单来源只能是 cart 或 buyNow |
| `source = buyNow` 但不传 `productId` | `A0400` 立即购买必须指定商品 |
| `quantity = 99999` | `A0400` 「…」库存不足，当前库存 N |
| `source = cart`、`cartIds = []` | `A0400` 请选择要结算的购物车商品 |
| 用已下架商品下单 | `A0400` 「…」已下架，无法下单 |
| 不传 Token | HTTP 401 / `A0230` |

---

## 2. API-053 我的订单列表

**请求**

```
GET /api/v1/orders?pageNum=1&pageSize=10&orderStatus=0&keyword={{orderNo}}
Authorization: Bearer {{buyerToken}}
```

| 参数 | 必填 | 说明 |
|---|---|---|
| `pageNum` | 否 | 默认 1 |
| `pageSize` | 否 | 默认 10，最大 100 |
| `orderStatus` | 否 | 0 待付款 / 1 待发货 / 2 待收货 / 3 已完成 / 4 已取消 / 5 售后中 |
| `keyword` | 否 | 匹配订单号**或商品名** |

**必须核对的结果**

1. `data.total ≥ 1`，`data.list[0].orderNo` 等于刚下的单号。
2. 列表中**只能出现 buyer01 自己的订单**（用 buyer02 的 Token 调，结果里不能有这单）。
3. `receiverPhone` 为 `138****8000`（列表脱敏）。
4. 把 `keyword` 换成商品名「兴化米粉」，也能搜到该订单。
5. `pageSize=1000` 时实际返回不超过 100 条。

**实测响应（已裁剪）**

```json
{
  "code": "00000",
  "data": {
    "total": 1,
    "list": [{
      "orderNo": "ZS202611263264",
      "orderStatus": 0,
      "receiverPhone": "138****8000",
      "totalAmount": 56.0
    }]
  }
}
```

---

## 3. API-054 订单详情

**请求**

```
GET /api/v1/orders/{{orderId}}
Authorization: Bearer {{buyerToken}}
```

**必须核对的结果**

1. `code = 00000`，返回单个 `OrderVO`。
2. `receiverPhone` 是**明文** `13800138000`（与列表的脱敏形成对照）。
3. `items` 完整，金额与下单时一致。
4. 买家用**自己的** Token 能看；买家 2 看买家 1 的订单 → `200 + A0301 无权查看该订单`。
5. 商家 farmer01 能看本店订单；farmer02 看该单 → `200 + A0301`。
6. 管理员 admin 能看全部订单。
7. 不存在的 ID（如 `999999`）→ `200 + A0404 订单不存在`。

---

## 4. API-055 支付订单（模拟）

**请求**

```
PUT /api/v1/orders/{{orderId}}/pay
Authorization: Bearer {{buyerToken}}
Content-Type: application/json

{ "payType": 3 }
```

**必须核对的结果**

1. `orderStatus` 由 `0` 变 `1`（待发货）。
2. `payStatus` 由 `0` 变 `1`，`payTime` 有值且为当前时间。
3. `payType = 3`。
4. **再次支付同一单** → `200 + A0400 该订单当前状态不可支付`。
5. 用买家 2 的 Token 支付 → `200 + A0301 无权操作他人的订单`。
6. 用已取消的订单支付 → `A0400 该订单当前状态不可支付`。

> 管理员虽然拥有 `order:pay` 权限点，但数据归属校验会拦下：`A0400 无权操作他人的订单`。

### 4.1 支付宝沙箱支付（可选）

上面测的是模拟支付。要看**真实支付链路**，得先按 `doc/alipay_sandbox.md` 填好沙箱的
`app-id` / `private-key` / `alipay-public-key` 并把 `alipay.enabled` 打开，重启后端。

**自检配置**

```bash
curl http://localhost:8080/api/v1/pay/alipay/config
# 期望 data.ready = true，data.missing = ""
```

**四个接口**

| 方法 | 路径 | 鉴权 | 期望 |
|---|---|---|---|
| GET | `/api/v1/pay/alipay/config` | 否 | `00000`，`ready` 反映配置状态 |
| GET | `/api/v1/pay/alipay/page?orderId={{id}}` | 买家 | `00000`，`payUrl` 指向沙箱网关，含 `sign` |
| GET | `/api/v1/pay/alipay/query?orderNo={{no}}` | 买家 | `00000`，未付 `paid=false`，付完 `paid=true` |
| POST | `/api/v1/pay/alipay/notify` | 否 | 纯文本 `success` / `failure` |

**必须核对的结果**

1. `/page` 返回的 `payUrl` 里 `method=alipay.trade.page.pay`、`sign_type=RSA2`、
   `biz_content` 里的 `out_trade_no` 与订单号一致、`total_amount` 与 `payAmount` 一致。
2. 拿**别人的**订单 id 调 `/page` → `A0400 无权操作他人的订单`；不带 Token → `401`。
3. 未配置密钥时调 `/page` → `A0400 支付宝支付未启用…`（不是 500）。
4. 订单已支付后再调 `/query` → 直接返回 `paid=true` 并带上 `tradeNo`，不再请求支付宝。
5. 已支付订单再发异步通知 → 仍返回 `success`，`pay_time` 不变（幂等）。
6. 通知里的 `total_amount` 与订单不符 → 返回 `failure`，订单状态不变。
7. 用错误的密钥签名通知 → 返回 `failure`，订单状态不变。
8. 支付成功后 `pay_type` 落为 `2`（支付宝），`orders.trade_no` 有值。

**本地无公网怎么测**：`notify_url` 打不进来是正常的，主链路走
「浏览器跳回 return_url → 前端调 `/query` 主动查单」，见 `doc/alipay_sandbox.md` 第 6 节。

---

## 5. API-056 取消订单

**请求**

```
PUT /api/v1/orders/{{cancelOrderId}}/cancel
Authorization: Bearer {{buyerToken}}
Content-Type: application/json

{ "reason": "不想要了" }
```

> 需要一张**待付款**订单。先按 API-052 再下一单，把新 ID 填进 `cancelOrderId`。

**必须核对的结果**

1. `orderStatus` 变为 `4`（已取消），`payStatus` 仍为 `0`。
2. 取消前后对比商品库存：**回到下单前的数量**（库存回滚）。
3. 已支付订单取消 → `200 + A0400 仅待付款订单可取消`。
4. 取消别人的订单 → `200 + A0301 无权操作他人的订单`。
5. `reason` 超过 100 字 → `A0400 取消原因最长 100 个字符`。

实测：绿茶库存 `36 → 33`（下单 3 件），取消后 `33 → 36`。

---

## 6. API-057 确认收货

**请求**

```
PUT /api/v1/orders/{{orderId}}/confirm
Authorization: Bearer {{buyerToken}}
```

**必须核对的结果**

1. 只有 `orderStatus = 2`（待收货）才能确认；成功后变 `3`（已完成）。
2. `finishTime` 写入当前时间。
3. `afterSaleAllowed` 由 `false` 变 `true`（已完成订单可申请售后）。
4. 未发货（状态 0/1）就确认 → `A0400 仅待收货订单可确认收货`。
5. 重复确认 → 同上。

---

## 7. API-058 商家订单列表

**请求**

```
GET /api/v1/farmer/orders?pageNum=1&pageSize=10&orderStatus=1&keyword={{orderNo}}
Authorization: Bearer {{farmerToken}}
```

| 参数 | 说明 |
|---|---|
| `orderStatus` | 同 API-053 |
| `keyword` | 匹配订单号**或买家昵称/用户名** |

**必须核对的结果**

1. farmer01 的结果里包含刚支付、待发货的订单。
2. **只包含本店订单**：用 farmer02 的 Token 查同一单号，`total = 0`。
3. 买家用 Token 调该接口 → **HTTP 403 + A0301 权限不足**（买家没有 `order:farmerList`）。
4. `keyword` 换成买家昵称「小娟」，能命中该订单。

---

## 8. API-059 订单发货

**请求**

```
PUT /api/v1/farmer/orders/{{orderId}}/ship
Authorization: Bearer {{farmerToken}}
Content-Type: application/json

{ "logisticsCompany": "顺丰速运", "logisticsNo": "SF1234567890" }
```

**必须核对的结果**

1. 只有 `orderStatus = 1` 可发货；成功后变 `2`（待收货）。
2. `logisticsCompany`、`logisticsNo` 与请求完全一致，`shipTime` 有值。
3. 商品销量 `saleCount` 增加对应数量（`GET /api/v1/products/1` 对比）。
4. **重复发货** → `A0400 仅待发货订单可发货`。
5. farmer02 给 farmer01 的订单发货 → `200 + A0301 无权操作其他商家的订单`。
6. 未支付订单发货 → `A0400 仅待发货订单可发货`。
7. 缺少物流公司或单号 → `A0400 物流公司不能为空 / 物流单号不能为空`。

---

## 9. API-060 后台订单查询与导出

**请求**

```
GET /api/v1/admin/orders?orderNo={{orderNo}}
Authorization: Bearer {{adminToken}}
```

| 参数 | 说明 |
|---|---|
| `orderNo` | **精确匹配**订单号 |
| `buyerId` | 买家 userId |
| `farmerId` | 商家 userId |
| `orderStatus` | 状态筛选 |
| `startTime` / `endTime` | `yyyy-MM-dd HH:mm:ss`，按下单时间过滤 |
| `keyword` | 订单号模糊匹配（与 `orderNo` 精确匹配区分） |

**必须核对的结果**

1. `orderNo` 精确命中 1 条，字段与其他端一致。
2. `buyerId=1` 只返回 buyer01 的订单；`farmerId=5` 只返回鲜果合作社的订单。
3. `orderStatus=3` 的结果全部是已完成。
4. 时间范围传今天 `00:00:00` ~ `23:59:59`，能查到刚完成的订单。
5. 该接口**只有 GET**，没有改状态、改金额的入口（管理端只读）。
6. 商家或买家调用 → HTTP 403（没有 `order:adminList`）。

### 9.1 导出订单（API-060 的第二个动作）

**请求**

```
GET /api/v1/admin/orders/export?orderStatus=3&startTime=2026-11-26 00:00:00&endTime=2026-11-26 23:59:59
Authorization: Bearer {{adminToken}}
```

| 参数 | 说明 |
|---|---|
| 筛选参数 | 与上面的查询接口**完全一致**（`orderNo` / `buyerId` / `farmerId` / `orderStatus` / `startTime` / `endTime` / `keyword`） |
| `pageNum` / `pageSize` | 忽略，导出不分页 |
| 上限 | 单次最多 10000 条 |

**返回**：`application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`（Excel .xlsx），文件名 `订单列表_yyyyMMdd_HHmmss.xlsx`；响应头 `X-Export-Count` 给出实际导出条数。

**表格版式**（Excel 打开后的前 3 行）

| 行 | 内容 | 样式 |
|---|---|---|
| 1 | 钟山镇扶贫助农系统 · 订单列表 | 合并 A1:U1，16 号加粗、居中 |
| 2 | 导出时间：2026-11-26 21:59:41　　数据条数：57 条　　筛选条件：全部订单 | 合并 A2:U2，灰色小字、居中 |
| 3 | 订单号、下单时间、…、完成时间（21 列） | 深绿底白字加粗、带筛选箭头 |
| 4 起 | 订单数据，一单一行 | 金额右对齐两位小数；商品明细/收货地址/买家备注自动换行 |

> 前三行冻结，向下滚动时表头始终可见；第 2 行的「筛选条件」会写成 `订单状态=已完成`、`下单起=…，下单止=…`、`订单号=…`，无条件时显示「全部订单」。

**必须核对的结果**

1. `X-Export-Count` 与同条件查询返回的 `total` 一致，也等于第 2 行「数据条数」。
2. 第 1 行是标题「钟山镇扶贫助农系统 · 订单列表」，居中加粗，A1:U1 合并。
3. 第 3 行是 21 列字段名：订单号、下单时间、订单状态、支付状态、支付方式、买家、商家店铺、商家账号、商品明细、商品总额、运费、实付金额、收货人、联系电话、收货地址、买家备注、支付时间、发货时间、物流公司、物流单号、完成时间。
4. 一个订单一行，多商品合并成一格（`商品A×2；商品B×1`）。
5. 联系电话为**明文**（与订单详情一致，列表才脱敏）。
6. 用买家 / 商家 Token 调用 → HTTP 403 + `A0301`；不带 Token → HTTP 401 + `A0230`。
7. 空结果也应是合法文件：只有标题、说明、表头 3 行，`X-Export-Count: 0`。

**命令行快速验证**

```bash
curl -s -D - -o orders.xlsx \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  "http://localhost:8081/api/v1/admin/orders/export?orderStatus=3"
file orders.xlsx    # 应为 Microsoft Excel 2007+
python -c "import openpyxl;ws=openpyxl.load_workbook('orders.xlsx').active;print(ws['A1'].value,ws['A3'].value,ws.max_row)"
# 期望：钟山镇扶贫助农系统 · 订单列表 订单号 <数据行数+3>
```

---

## 10. 三条端到端剧本

### 剧本 A：主链路（约 8 分钟）

| 步骤 | 接口 | 关键动作 | 预期 |
|---|---|---|---|
| 1 | 登录 | buyer01 取 Token | `00000` |
| 2 | API-052 | 米粉 ×2 下单 | 1 张订单，状态 0，扣库存 2 |
| 3 | API-053 | 按单号查 | 命中 1 条，手机号脱敏 |
| 4 | API-054 | 查详情 | 手机号明文 |
| 5 | API-055 | 支付 | 状态 1，payTime 有值 |
| 6 | 登录 | farmer01 取 Token | — |
| 7 | API-058 | 查待发货 | 能看到该单 |
| 8 | API-059 | 顺丰 SF1234567890 | 状态 2，物流写入 |
| 9 | API-057 | buyer01 确认收货 | 状态 3，afterSaleAllowed=true |
| 10 | 登录 | admin 取 Token | — |
| 11 | API-060 | 按单号 / 状态 / 时间查 | 均能命中 |

### 剧本 B：取消与库存回滚

1. 记录商品 8 当前库存（如 36）。
2. API-052 下单 3 件 → 库存 33。
3. API-056 取消该单 → 状态 4，库存回到 36。
4. 再用 API-055 支付该单 → `A0400 该订单当前状态不可支付`。

### 剧本 C：购物车跨店拆单

1. API-048 加购：商品 1 ×1、商品 8 ×2（两个接口调用）。
2. `GET /api/v1/cart` 拿到两个 `cartIds`。
3. API-052 传 `source=cart`、`cartIds=[两个ID]`。
4. 预期返回 **2 张订单**：
   - 绿野农场，`28.00`，明细为米粉；
   - 鲜果合作社，`152.00`，明细为绿茶。
5. 再查购物车：已结算的行被清空。

---

## 11. 测试记录表

| 编号 | 用例 | 预期 | 实际 | 结论 |
|---|---|---|---|---|
| 052-1 | 立即购买下单 | 1 单，状态 0，金额=单价×数量 | | |
| 052-2 | 购物车跨店下单 | 2 单，按店铺拆分 | | |
| 052-3 | 库存不足 | A0400 | | |
| 052-4 | 手机号非法 | A0400 | | |
| 053-1 | 按状态 + 单号筛选 | 命中 1 条 | | |
| 053-2 | 列表手机号脱敏 | 138****8000 | | |
| 054-1 | 买家看本人订单 | 手机号明文 | | |
| 054-2 | 买家 2 越权查看 | A0301 | | |
| 055-1 | 支付成功 | 状态 1，payTime 有值 | | |
| 055-2 | 重复支付 | A0400 | | |
| 056-1 | 取消待付款订单 | 状态 4，库存回滚 | | |
| 056-2 | 取消已支付订单 | A0400 | | |
| 057-1 | 确认收货 | 状态 3，finishTime | | |
| 058-1 | 商家看待发货 | 含该单 | | |
| 058-2 | 跨店隔离 | farmer02 查不到 | | |
| 059-1 | 发货 | 状态 2，物流写入 | | |
| 059-2 | 越权发货 | A0301 | | |
| 060-1 | 按单号查询 | 精确 1 条 | | |
| 060-2 | 按时间范围查询 | 命中当天订单 | | |

---

## 12. 常见问题

| 现象 | 原因 | 处理 |
|---|---|---|
| 登录返回 `A0211 请输入验证码` | 连到了 8080 | 改用 8081，或带上 `captchaId/captchaCode` |
| 所有接口 401 | Token 过期（2 小时）或没加 `Bearer ` 前缀 | 重新登录 |
| 下单提示库存不足 | 前面测试没取消订单，库存被占 | 取消待付款单，或换商品 |
| 拆单只得到 1 张 | 其中一件是委托代销商品（如笋干） | 用商品 1 + 8 验证 |
| 后台时间筛选查不到 | 空格没编码或时分秒写成 `00:00` | 用 `2026-11-26 00:00:00` 并 URL 编码 |
| 商家登录后看不到店铺名 | 连的是改动前的旧实例 | 重启后端，或改用 8080 |

> 测试会真实写库：订单会留在管理端列表里，商品库存也会变化。答辩前如需干净数据，重新导入 `doc/zhongshan_farm_init.sql` 即可。
