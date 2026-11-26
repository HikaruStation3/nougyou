# 文件上传模块测试手册（API-113）

> 对应需求：FR-013（入驻申请需上传身份证、贫困证明）；接口文档 sheet「接口清单」第 113 行。
> 适用范围：买家、农户商家、管理员三类角色；身份证、贫困证明、商品图、头像、短视频统一走本接口。

---

## 1. 模块范围

| 项 | 内容 |
| --- | --- |
| 上传接口 | `POST /api/v1/files/upload`（multipart/form-data） |
| 必填参数 | `file` 文件；`bizType` 业务类型 |
| 业务类型 | `idcard` 身份证 / `proof` 贫困证明 / `product` 商品图 / `video` 短视频 / `avatar` 头像 |
| 返回结构 | `{ url, name, size, contentType }`（UploadVO） |
| 存储 | MinIO 优先；MinIO 不可用时落本地 `uploads/`，返回 `/api/v1/files/local/...` |
| 目录规则 | `{bizType}/{yyyy}/{MM}/{uuid}.{ext}`，例如 `proof/2026/11/xxxx.png` |
| 删除接口 | `DELETE /api/v1/files?filePath={url}`，仅 `file:delete`（管理员、商家） |
| 兼容接口 | `POST /api/v1/files` 保留旧调用：mp4 归 `video/`，图片归 `product/` |

### 1.1 校验规则

| bizType | 允许格式 | 单文件上限 | 说明 |
| --- | --- | --- | --- |
| idcard | jpg / jpeg / png | 5MB | 身份证正反面 |
| proof | jpg / jpeg / png | 5MB | 贫困证明，入驻申请必传 |
| product | jpg / jpeg / png | 5MB | 商品图集、农家乐图片、封面 |
| video | mp4 | 100MB | 短视频，容器上限同为 100MB |
| avatar | jpg / jpeg / png | 5MB | 用户头像 |

统一错误口径：业务校验失败返回 HTTP 200 + `code=A0400`；未登录 401 + `A0230`；无权限 403 + `A0301`；超过 multipart 上限返回 HTTP 413 + `A0400`。

---

## 2. 接口用例与实测结果

测试环境：`http://localhost:8081`（`--nougyou.captcha.enabled=false`），MinIO 未启动，全部走本地兜底。
测试脚本：`python scripts/file_upload_smoke_test.py --base http://localhost:8081`
实测结果：**25 / 25 通过**（2026-11-26）。

| # | 用例 | 请求要点 | 预期 | 实测 |
| --- | --- | --- | --- | --- |
| 1 | 买家上传贫困证明 | buyer01，`bizType=proof`，png | 200，`url` 含 `/proof/2026/11/` | ✅ 179B，`image/png` |
| 2 | 买家上传身份证 | buyer01，`bizType=idcard` | 200，`url` 含 `/idcard/` | ✅ |
| 3 | 上传地址匿名访问 | GET 返回的 `url` | 200，字节数一致 | ✅ 200 / 179B |
| 4 | bizType 必填 | 不传 `bizType` | `A0400`「请选择文件业务类型」 | ✅ |
| 5 | bizType 非法 | `bizType=unknown` | `A0400`「不支持的业务类型」 | ✅ |
| 6 | 图片拒绝非图片 | `bizType=product`，txt | `A0400`「仅支持 jpg/jpeg/png」 | ✅ |
| 7 | 图片超 5MB | `bizType=product`，6.44MB png | `A0400`「不能超过 5MB」 | ✅ |
| 8 | mp4 不能当商品图 | `bizType=product`，mp4 | `A0400` 格式错误 | ✅ |
| 9 | 短视频 6MB 可上传 | `bizType=video`，6MB mp4 | 200，归 `video/` | ✅ `video/mp4` |
| 10 | 商家上传头像 | farmer01，`bizType=avatar` | 200，归 `avatar/` | ✅ |
| 11 | 管理员上传商品图 | admin，`bizType=product` | 200 | ✅ |
| 12 | 未登录上传 | 不带令牌 | 401 `A0230` | ✅ |
| 13 | 买家删除文件 | buyer01 调 DELETE | 403 `A0301` | ✅ 买家只有 `file:upload` |
| 14 | 旧接口 mp4 | `POST /api/v1/files` | 200，`objectName` 以 `video/` 开头 | ✅ |
| 15 | 旧接口图片 | `POST /api/v1/files` | 200，`objectName` 以 `product/` 开头 | ✅ |
| 16 | 管理员删除 | DELETE 贫困证明 | 200，随后 GET 返回 404 | ✅ |
| 17 | 超过容器上限 | 101MB mp4 | HTTP 413「文件大小超过 100MB 上限」 | ✅ |

---

## 3. 页面操作路径

### 3.1 管理端「工具箱 → 文件上传」

1. `pnpm dev` 启动管理端（3000），admin 登录。
2. 左侧菜单：**工具箱 → 文件上传**（组件 `views/system/file/index.vue`，菜单权限 `file:upload:menu`）。
3. 页面左侧选择业务类型（身份证 / 贫困证明 / 商品图 / 短视频 / 头像），下方会显示该类型的格式与大小提示。
4. 拖拽或点击上传：
   - 客户端先校验扩展名与大小，不符合直接 toast，不发请求；
   - 上传中显示百分比进度；
   - 成功后右侧展示预览（图片可点击放大，视频可播放）、文件名、大小、MIME、访问地址。
5. 点击「复制链接」/「新窗口打开」验证地址可用；有 `file:delete` 时可删除，删除后地址立即 404。
6. 本次会话上传的文件会出现在「本次上传记录」表中（仅前端内存，刷新即清空）。

> 管理端开发环境图片地址是相对路径，页面会自动补 `VITE_APP_BASE_API`（`/dev-api`）前缀，否则 3000 端口无法直接访问后端静态资源。

### 3.2 门户「加入我们」上传证明材料

1. `npm run dev` 启动门户（3100），使用未入驻的买家账号登录（如 buyer01；已是商家会提示无需重复提交）。
2. 进入 **加入我们**，表单「证件材料」区有三个上传位：身份证正面、身份证反面、贫困证明。
3. 贫困证明为必填：不上传直接提交，表单提示「请上传贫困证明」。
4. 上传成功后提交申请，管理员在 **入驻审核 → 详情 → 材料** 中可看到三张图片。
5. 支持 jpg / jpeg / png，单张 ≤5MB；格式或大小不符会在选择文件时被拦截。

### 3.3 门户其他入口

| 入口 | 页面 | bizType |
| --- | --- | --- |
| 商家 → 我的农产品 → 新增/编辑 → 商品图集 | `MerchantProducts.vue` | product |
| 商家 → 我的农家乐 → 封面/图集 | `MerchantProjects.vue` | product |
| 商家 → 短视频 → 发布（视频 / 封面） | `MerchantVideos.vue` | video / product |
| 管理端 → 个人中心 → 头像 | `views/profile/index.vue` | avatar |

---

## 4. 关键口径与常见坑

1. **买家必须有 `file:upload`**：原 `BUYER_PERMISSION_CODES` 没有该权限，买家上传证明材料会 403。已补入 `DataInitializer`，并在 `zhongshan_farm_init.sql` 的买家角色权限中补 `(2,13)`；旧库启动后端会自动补齐，无需手工刷库。
2. **bizType 不传不会走默认值**：接口文档标注必填，服务端返回 `A0400`，避免材料被错误归档到商品目录。
3. **视频上限 100MB 是两道**：业务层校验 `file.getSize()`，Spring multipart 也会在进 Controller 前拦截，已在全局异常处理里统一成「文件大小超过 100MB 上限」（HTTP 413）。
4. **旧接口仍可用**：门户历史页面与短视频上传仍可能调用 `POST /api/v1/files`，该入口按扩展名自动归类，不要删除。
5. **MIME 不信任客户端**：服务端按扩展名映射 `image/jpeg`、`image/png`、`video/mp4` 写入对象存储，避免把 html 伪装成图片直出。
6. **本地兜底地址**：MinIO 未启动时返回 `/api/v1/files/local/...`，该路径已在 `SecurityConfig` 放行，可直接 `<img>` / `<video>` 访问；启动 MinIO 后返回 `http://localhost:9000/nougyou/{biz}/...`。
7. **删除是物理删除**：对象存储或本地文件立即移除，数据库中的历史 URL 会失效；演示时不要删除种子数据引用的图片。
8. **本模块没有文件表**：接口文档未定义文件列表/下载接口，上传记录只在管理端页面内存中展示；如后续需要「文件库」再补表与列表接口。

---

## 5. 一分钟自检

```bash
# 1. 后端编译
JAVA_HOME="D:/Program Files/Java/jdk-21.0.13.1" \
  "D:/Program Files/Apache/maven/bin/mvn.cmd" -o -f backend/pom.xml compile

# 2. 启动免验证码测试实例
#    mvn -o -f backend/pom.xml spring-boot:run \
#    -Dspring-boot.run.arguments="--server.port=8081 --nougyou.captcha.enabled=false"

# 3. 跑冒烟
python scripts/file_upload_smoke_test.py --base http://localhost:8081

# 4. 前端构建
cd portal && npm run build
cd ../frontend && npm run build-only
```

预期：脚本输出 `结果：25/25 通过`；两个前端构建均成功。
