# 支付宝沙箱支付接入说明

订单模块原来的支付是「模拟支付」——点一下直接改状态，`payType` 只是记录一个数字。
现在补上了**真正的支付宝电脑网站支付（沙箱环境）**：会跳到支付宝收银台，用沙箱虚拟买家账号付款，
回来后向后端主动查单、验签、落库。

> 默认是关闭的。不填密钥时订单页只走模拟支付，其它功能不受影响；
> 填好三个参数并打开开关，订单页的「去支付」就会多出「支付宝沙箱」选项。

---

## 1. 先拿到 3 个参数

1. 打开 <https://open.alipay.com>，用支付宝账号登录。
2. 右上角进 **控制台** → 左侧 **沙箱**（有的账号在「开发工具 → 沙箱」）。
3. 页面上能直接看到 **APPID**，形如 `9021000xxxxxxxxx`——这就是第 1 个参数。
4. 同一页往下找 **开发信息 → 接口加签方式**，选 **自定义密钥**（没有就点设置），
   用「支付宝开放平台密钥工具」生成一对 **RSA2（2048 位）** 密钥：
   - 工具生成后，复制 **应用私钥** → 第 2 个参数（只在你本地，不上传）
   - 把 **应用公钥** 粘贴到沙箱页面保存
   - 保存后页面会显示 **支付宝公钥** → 第 3 个参数
5. 再往下有 **沙箱账号**：一个买家账号、登录密码、支付密码。
   演示付款时就用这个买家账号登录收银台。

关键区别（最容易搞错）：

| 名称 | 谁生成 | 用在哪 |
|---|---|---|
| 应用私钥 | 你在本地生成，**不告诉任何人** | 后端签名 |
| 应用公钥 | 由应用私钥导出，粘到沙箱页面 | 支付宝用来验你的签名 |
| **支付宝公钥** | 沙箱页面生成，你复制下来 | 后端用来验支付宝的响应/通知 |

后端配置要填的是 **支付宝公钥**，不是应用公钥。填错了查单会报「验签失败」。

---

## 2. 填配置

改 `backend/src/main/resources/application.yml` 的 `alipay` 段：

```yaml
alipay:
  enabled: true                    # ← 打开开关
  sandbox: true
  gateway: https://openapi-sandbox.dl.alipaydev.com/gateway.do
  app-id: 9021000xxxxxxxxx
  private-key: 你的应用私钥（单行 Base64，可以带或不带 -----BEGIN 头尾）
  alipay-public-key: 支付宝页面上的支付宝公钥
  notify-url: http://localhost:8080/api/v1/pay/alipay/notify
  return-url: http://localhost:3100/#/pay/result
  subject-prefix: 钟山镇扶贫助农-
```

不想把密钥写进文件，也可以用环境变量（优先级更高，适合答辩前临时注入）：

```bash
ALIPAY_ENABLED=true
ALIPAY_APP_ID=9021000xxxxxxxxx
ALIPAY_PRIVATE_KEY=<应用私钥>
ALIPAY_PUBLIC_KEY=<支付宝公钥>
```

密钥里有 `+ / =` 这些字符，YAML 里建议整行加引号，或直接用环境变量避免转义问题。

改完重启后端。

---

## 3. 自检：确认配对了

```bash
curl http://localhost:8080/api/v1/pay/alipay/config
```

- 配好：`"enabled":true, "ready":true, "missing":""`
- 没配好：`"ready":false`，`missing` 会直接告诉你缺哪几项，比如 `app-id alipay-public-key`

这个接口不需要登录，前端也用它决定要不要显示「支付宝沙箱」入口。

---

## 4. 完整演示流程

1. 买家登录门户（`http://localhost:3100`），加购物车 → 结算 → 支付方式选「支付宝（沙箱）」→ 提交订单。
2. 订单列表点「去支付」，弹窗里选 **「支付宝沙箱」**。
3. 浏览器跳到支付宝沙箱收银台，用**沙箱买家账号**登录 → 输入**沙箱支付密码** → 确认付款。
4. 付款成功后支付宝把浏览器跳回 `http://localhost:3100/#/pay/result?out_trade_no=...`。
5. 结果页自动调 `/pay/alipay/query` 查单：
   - 查到 `TRADE_SUCCESS` → 显示「支付成功」，订单置为**待发货**，`trade_no` 落库
   - 还没付 → 显示「尚未完成支付」，可以点「重新查询」
6. 回订单详情：支付方式显示 **支付宝（沙箱）**，多出一行 **支付宝交易号**。

在沙箱收银台付款不会产生真实扣款。

---

## 5. 接口清单

| 方法 | 路径 | 鉴权 | 说明 |
|---|---|---|---|
| GET | `/api/v1/pay/alipay/config` | 否 | 探测是否已配置 |
| GET | `/api/v1/pay/alipay/page?orderId=` | 买家 | 返回收银台跳转 URL（后端已签名） |
| GET | `/api/v1/pay/alipay/query?orderNo=` | 买家/管理员 | 主动查单，成功则落库为已支付 |
| POST | `/api/v1/pay/alipay/notify` | 否（验签） | 支付宝异步通知，返回 `success` / `failure` |

对应的订单字段：`orders.trade_no`（支付宝交易号，模拟支付为空）。

---

## 6. 为什么本机没有公网也能跑

支付宝的 `notify_url` 是**服务器之间**的调用，必须是公网可达地址，本机 `localhost` 收不到。
所以主链路设计成：

```
浏览器跳转收银台 → 付款 → 支付宝 302 把浏览器带回 return_url
                                    ↓
                          前端读 out_trade_no
                                    ↓
                     调 /pay/alipay/query 主动查单（后端 → 支付宝网关）
                                    ↓
                          验签通过 → 落库为已支付
```

`return_url` 由浏览器发起，`localhost` 完全没问题。
部署到公网后，把 `notify-url` 改成真实域名，异步通知会自动接管，`/query` 仍可作为兜底。

---

## 7. 常见问题

| 现象 | 原因 | 处理 |
|---|---|---|
| `ready:false` | `enabled` 没开，或三个参数有空值 | 看 `/config` 返回的 `missing` |
| 收银台报「无效的AppID参数」 | APPID 填错，或用了正式环境 APPID | 从沙箱页面重新复制 |
| 查单报「响应验签失败」 | 把**应用公钥**填成了支付宝公钥 | 换成沙箱页面的「支付宝公钥」 |
| 收银台报「验签失败 / 签名不正确」 | 应用公钥没粘到沙箱页面，或私钥不匹配 | 重新导出应用公钥并保存 |
| 付完回来还是「尚未完成支付」 | 支付宝侧有 1~3 秒延迟 | 点「重新查询」，或稍等再看 |
| 通知一直返回 `failure` | `total_amount` 与订单应付金额不一致 | 查订单金额是否被改动 |
| 收银台提示「交易已关闭」 | 超过 30 分钟未付款（`timeout_express=30m`） | 回订单列表取消重下 |
| 私钥解析失败 | 用了 PKCS#1 格式 | 用支付宝密钥工具重新导出（PKCS#8） |

日志里搜 `支付宝` 能看到下单、查单、通知的完整记录，`logging.level.com.nougyou: debug` 还能看响应原文。

---

## 8. 技术实现说明（答辩可直接讲）

不引入官方 `alipay-sdk-java`（40MB 且带一堆传递依赖，本项目是离线 m2repo），
改用 JDK 自带的 `SHA256withRSA` 实现，代码在 `com.nougyou.payment`：

- `AlipaySigner`：待签名串 = 除 `sign`/`sign_type` 外的非空参数，按参数名 ASCII 升序拼成 `k=v&k=v`；
  签名 `SHA256withRSA` + Base64。
- `AlipayService`：拼公共参数 + `biz_content` → 签名 → 拼收银台 URL；
  查单走 `alipay.trade.query`，对响应节点**原文**验签（不能解析后重新序列化，字段顺序会变）。
- `AlipayController`：`notify` 验签 + 校验金额后调 `OrderService.markPaidByAlipay`。
- 幂等：`markPaidByAlipay` 用带 `pay_status=0` 条件的 UPDATE，重复通知/重复查单只生效一次。

安全点：金额比对、归属校验（只能给自己的订单付款）、验签、CSV/Excel 导出不涉及金额篡改。
