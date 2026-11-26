#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""文件上传模块（API-113）冒烟测试。

覆盖：
  1. 买家上传贫困证明 / 身份证（FR-013 需要的 bizType）
  2. 业务类型必填、取值非法、格式非法、图片超过 5MB
  3. 短视频 6MB 可通过（证明按类型分别限额），mp4 不能当商品图
  4. 商家上传头像、管理员上传商品图
  5. 未登录 401；买家无 file:delete 权限删除 403
  6. 旧版 POST /api/v1/files 兼容，并按扩展名归档到 video / product
  7. 上传后地址可直接访问，管理员删除后地址失效

用法：
  python scripts/file_upload_smoke_test.py --base http://localhost:8081
默认账号：buyer01 / farmer01 / admin，密码可用 --password 指定。
"""

import argparse
import json
import os
import random
import re
import struct
import sys
import urllib.error
import urllib.request
import uuid
import zlib

BASE = "http://localhost:8081"
PASSWORD = "123456"
RESULTS = []


def request(method, path, token=None, body=None, content_type=None):
    req = urllib.request.Request(BASE + path, data=body, method=method)
    if token:
        req.add_header("Authorization", "Bearer " + token)
    if content_type:
        req.add_header("Content-Type", content_type)
    try:
        with urllib.request.urlopen(req, timeout=120) as resp:
            return resp.status, resp.read(), dict(resp.headers)
    except urllib.error.HTTPError as exc:
        return exc.code, exc.read(), dict(exc.headers)


def json_request(method, path, token=None, payload=None):
    body = json.dumps(payload).encode("utf-8") if payload is not None else None
    status, raw, _ = request(method, path, token, body, "application/json")
    try:
        data = json.loads(raw.decode("utf-8"))
    except Exception:
        data = {"raw": raw[:200].decode("utf-8", "replace")}
    return status, data


def login(username):
    status, data = json_request("POST", "/api/v1/auth/login",
                                payload={"username": username, "password": PASSWORD})
    assert status == 200 and data.get("code") == "00000", f"{username} 登录失败：{status} {data}"
    return data["data"]["accessToken"]


def multipart(fields, filename, content, content_type):
    boundary = "----nougyou" + uuid.uuid4().hex
    lines = []
    for key, value in fields.items():
        lines.append(f"--{boundary}\r\nContent-Disposition: form-data; name=\"{key}\"\r\n\r\n{value}\r\n")
    head = "".join(lines)
    head += (f"--{boundary}\r\nContent-Disposition: form-data; name=\"file\"; "
             f"filename=\"{filename}\"\r\nContent-Type: {content_type}\r\n\r\n")
    body = head.encode("utf-8") + content + f"\r\n--{boundary}--\r\n".encode("utf-8")
    return body, f"multipart/form-data; boundary={boundary}"


def upload(token, filename, content, content_type, biz_type=None, path="/api/v1/files/upload"):
    fields = {"bizType": biz_type} if biz_type is not None else {}
    body, ctype = multipart(fields, filename, content, content_type)
    status, raw, _ = request("POST", path, token, body, ctype)
    try:
        data = json.loads(raw.decode("utf-8"))
    except Exception:
        data = {"raw": raw[:200].decode("utf-8", "replace")}
    return status, data


def make_png(width, height, seed=None):
    """生成合法 PNG；seed 有值时用不可压缩像素把体积做大。"""
    rnd = random.Random(seed)
    rows = []
    for _ in range(height):
        if seed is None:
            rows.append(b"\x00" + bytes([120, 160, 90]) * width)
        else:
            rows.append(b"\x00" + rnd.randbytes(width * 3))
    raw = b"".join(rows)
    compressed = zlib.compress(raw, 6)

    def chunk(tag, payload):
        return struct.pack(">I", len(payload)) + tag + payload + struct.pack(
            ">I", zlib.crc32(tag + payload) & 0xFFFFFFFF)

    return (b"\x89PNG\r\n\x1a\n"
            + chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, 2, 0, 0, 0))
            + chunk(b"IDAT", compressed)
            + chunk(b"IEND", b""))


def make_mp4(size):
    return b"\x00\x00\x00\x18ftypmp42\x00\x00\x00\x00mp42isom" + os.urandom(size)


def check(name, condition, detail=""):
    RESULTS.append((name, bool(condition), detail))
    flag = "PASS" if condition else "FAIL"
    print(f"[{flag}] {name}" + (f" · {detail}" if detail else ""))
    return condition


def main():
    global BASE, PASSWORD
    parser = argparse.ArgumentParser()
    parser.add_argument("--base", default=BASE)
    parser.add_argument("--password", default=PASSWORD)
    args = parser.parse_args()
    BASE = args.base.rstrip("/")
    PASSWORD = args.password

    print(f"目标环境：{BASE}")
    buyer = login("buyer01")
    farmer = login("farmer01")
    admin = login("admin")

    small_png = make_png(64, 64)
    big_png = make_png(1500, 1500, seed=7)
    mp4_6m = make_mp4(6 * 1024 * 1024)
    print(f"测试素材：png {len(small_png) / 1024:.1f}KB / 大图 {len(big_png) / 1024 / 1024:.2f}MB "
          f"/ mp4 {len(mp4_6m) / 1024 / 1024:.1f}MB")

    created = []

    # 1. 买家上传贫困证明与身份证
    status, data = upload(buyer, "poverty.png", small_png, "image/png", "proof")
    proof_url = (data.get("data") or {}).get("url")
    if check("买家上传贫困证明", status == 200 and data.get("code") == "00000",
             f"{status} {data.get('msg')}"):
        created.append(proof_url)
        info = data["data"]
        check("贫困证明归档到 proof 目录",
              re.match(r"^/api/v1/files/local/proof/\d{4}/\d{2}/[0-9a-f-]+\.png$",
                       proof_url or "") is not None, proof_url or "")
        check("UploadVO 字段完整",
              info.get("name") == "poverty.png" and info.get("size") == len(small_png)
              and info.get("contentType") == "image/png",
              f"name={info.get('name')} size={info.get('size')} type={info.get('contentType')}")

    status, data = upload(buyer, "idcard.png", small_png, "image/png", "idcard")
    idcard_url = (data.get("data") or {}).get("url")
    if check("买家上传身份证", status == 200 and data.get("code") == "00000",
             f"{status} {data.get('msg')}"):
        created.append(idcard_url)
        check("身份证归档到 idcard 目录", "/files/local/idcard/" in (idcard_url or ""), idcard_url or "")

    # 2. 上传后的地址可直接访问
    if proof_url:
        status, raw, headers = request("GET", proof_url)
        check("上传地址匿名可访问", status == 200 and len(raw) == len(small_png),
              f"{status} {len(raw)}B {headers.get('Content-Type', '')}")

    # 3. 参数与类型校验
    status, data = upload(buyer, "poverty.png", small_png, "image/png", None)
    check("bizType 必填", status == 200 and data.get("code") == "A0400", data.get("msg", ""))

    status, data = upload(buyer, "poverty.png", small_png, "image/png", "unknown")
    check("bizType 取值非法", status == 200 and data.get("code") == "A0400", data.get("msg", ""))

    status, data = upload(buyer, "note.txt", b"hello", "text/plain", "product")
    check("商品图拒绝 txt", status == 200 and data.get("code") == "A0400", data.get("msg", ""))

    status, data = upload(buyer, "big.png", big_png, "image/png", "product")
    check("图片超过 5MB 被拒", status == 200 and data.get("code") == "A0400", data.get("msg", ""))

    status, data = upload(buyer, "clip.mp4", mp4_6m, "video/mp4", "product")
    check("mp4 不能当商品图", status == 200 and data.get("code") == "A0400", data.get("msg", ""))

    # 4. 短视频 6MB 可以通过（证明按业务类型区分限额）
    status, data = upload(buyer, "clip.mp4", mp4_6m, "video/mp4", "video")
    video_url = (data.get("data") or {}).get("url")
    if check("买家上传 6MB 短视频", status == 200 and data.get("code") == "00000",
             f"{status} {data.get('msg')}"):
        created.append(video_url)
        check("短视频归档到 video 目录", "/files/local/video/" in (video_url or ""), video_url or "")
        check("短视频 MIME 为 video/mp4",
              (data.get("data") or {}).get("contentType") == "video/mp4",
              str((data.get("data") or {}).get("contentType")))

    # 5. 商家 / 管理员
    status, data = upload(farmer, "avatar.png", small_png, "image/png", "avatar")
    if check("商家上传头像", status == 200 and data.get("code") == "00000",
             f"{status} {data.get('msg')}"):
        created.append((data.get("data") or {}).get("url"))
        check("头像归档到 avatar 目录", "/files/local/avatar/" in (data["data"]["url"] or ""),
              data["data"]["url"])

    status, data = upload(admin, "product.png", small_png, "image/png", "product")
    if check("管理员上传商品图", status == 200 and data.get("code") == "00000",
             f"{status} {data.get('msg')}"):
        created.append((data.get("data") or {}).get("url"))

    # 6. 鉴权
    status, data = upload(None, "poverty.png", small_png, "image/png", "proof")
    check("未登录上传返回 401", status == 401, f"{status} {data.get('code')}")

    if proof_url:
        status, data = json_request("DELETE", f"/api/v1/files?filePath={proof_url}", buyer)
        check("买家无删除权限返回 403", status == 403, f"{status} {data.get('code')}")

    # 7. 旧版接口兼容
    status, data = upload(admin, "legacy-video.mp4", mp4_6m, "video/mp4", path="/api/v1/files")
    legacy_video = data.get("data") or {}
    if check("旧版接口可传 mp4", status == 200 and data.get("code") == "00000",
             f"{status} {data.get('msg')}"):
        created.append(legacy_video.get("url"))
        check("旧版 mp4 归入 video 目录",
              str(legacy_video.get("objectName", "")).startswith("video/"),
              str(legacy_video.get("objectName")))

    status, data = upload(admin, "legacy.png", small_png, "image/png", path="/api/v1/files")
    legacy_png = data.get("data") or {}
    if check("旧版接口可传图片", status == 200 and data.get("code") == "00000",
             f"{status} {data.get('msg')}"):
        created.append(legacy_png.get("url"))
        check("旧版图片归入 product 目录",
              str(legacy_png.get("objectName", "")).startswith("product/"),
              str(legacy_png.get("objectName")))

    # 8. 管理员删除后地址失效
    if proof_url:
        status, data = json_request("DELETE", f"/api/v1/files?filePath={proof_url}", admin)
        check("管理员删除文件", status == 200 and data.get("code") == "00000",
              f"{status} {data.get('msg')}")
        status, _, _ = request("GET", proof_url)
        check("删除后地址返回 404", status == 404, str(status))
        created.remove(proof_url)

    # 收尾：清理本次上传的其余文件
    for url in [item for item in created if item]:
        json_request("DELETE", f"/api/v1/files?filePath={url}", admin)

    passed = sum(1 for _, ok, _ in RESULTS if ok)
    total = len(RESULTS)
    print(f"\n结果：{passed}/{total} 通过")
    if passed != total:
        print("失败项：")
        for name, ok, detail in RESULTS:
            if not ok:
                print(f"  - {name} · {detail}")
    return 0 if passed == total else 1


if __name__ == "__main__":
    sys.exit(main())
